package com.learningcalendar.desktop.sync

import com.learningcalendar.desktop.model.SyncSnapshot
import com.learningcalendar.desktop.model.SyncSnapshotParser
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipFile
import org.json.JSONObject

data class LoadedSync(
    val snapshot: SyncSnapshot,
    val directory: File,
)

class DesktopSyncStore(
    val applicationRoot: File = defaultApplicationRoot(),
) {
    private val snapshotsDirectory = File(applicationRoot, "snapshots")
    private val currentPointer = File(applicationRoot, "current-snapshot.txt")

    @Volatile
    var current: LoadedSync? = null
        private set

    init {
        applicationRoot.mkdirs()
        snapshotsDirectory.mkdirs()
        current = runCatching(::loadCurrent).getOrNull()
    }

    @Synchronized
    fun install(zipFile: File): LoadedSync {
        val temporary = File(
            applicationRoot,
            "incoming-${UUID.randomUUID()}",
        )
        require(temporary.mkdirs()) { "无法创建临时同步目录" }

        try {
            extractSafely(zipFile, temporary)
            val manifestFile = File(temporary, "manifest.json")
            require(manifestFile.isFile) { "同步包缺少 manifest.json" }
            val manifest = mergeDesktopWeeklyDocuments(
                previous = current,
                destination = temporary,
                manifestFile = manifestFile,
            )
            val snapshot = SyncSnapshotParser.parse(manifest.toString())

            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .format(Instant.now().atZone(ZoneId.systemDefault()))
            val finalDirectory = File(
                snapshotsDirectory,
                "$timestamp-${UUID.randomUUID().toString().take(8)}",
            )
            if (!temporary.renameTo(finalDirectory)) {
                throw IOException("无法保存同步数据")
            }
            writeCurrentPointer(finalDirectory)
            val loaded = LoadedSync(snapshot, finalDirectory)
            current = loaded
            return loaded
        } catch (error: Exception) {
            deleteOwnedDirectory(temporary)
            throw error
        }
    }

    private fun mergeDesktopWeeklyDocuments(
        previous: LoadedSync?,
        destination: File,
        manifestFile: File,
    ): JSONObject {
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val previousLoaded = previous ?: return manifest
        val desktopDocuments = previousLoaded.snapshot.weekDocuments
            .filter { it.relativePath.replace('\\', '/').startsWith(DESKTOP_WEEKLY_PREFIX) }
        if (desktopDocuments.isEmpty()) return manifest

        val tables = manifest.getJSONObject("tables")
        val documents = tables.optJSONArray("week_summary_documents")
            ?: org.json.JSONArray().also {
                tables.put("week_summary_documents", it)
            }
        val existingIds = buildSet {
            for (index in 0 until documents.length()) {
                add(documents.getJSONObject(index).optString("id"))
            }
        }
        var addedCount = 0
        desktopDocuments.forEach { document ->
            if (document.id in existingIds) return@forEach
            val source = File(
                previousLoaded.directory,
                "files/${document.relativePath}",
            ).canonicalFile
            if (!source.isFile) return@forEach
            val target = File(
                destination,
                "files/${document.relativePath}",
            ).canonicalFile
            target.parentFile.mkdirs()
            source.copyTo(target, overwrite = false)
            documents.put(
                JSONObject().apply {
                    put("id", document.id)
                    put("weekStartDate", document.weekStartDate.toString())
                    put("displayName", document.displayName)
                    put("relativePath", document.relativePath.replace('\\', '/'))
                    put("mimeType", document.mimeType ?: JSONObject.NULL)
                    put("uploadedAtEpochMillis", document.uploadedAtEpochMillis)
                },
            )
            addedCount += 1
        }
        if (addedCount > 0) {
            adjustAttachmentCount(manifest, addedCount)
            manifestFile.writeText(manifest.toString(2), Charsets.UTF_8)
        }
        return manifest
    }

    fun resolveAttachment(relativePath: String): File? {
        val snapshotDirectory = current?.directory ?: return null
        val filesRoot = File(snapshotDirectory, "files").canonicalFile
        val candidate = File(filesRoot, relativePath).canonicalFile
        val isInside = candidate.path.startsWith(filesRoot.path + File.separator)
        return candidate.takeIf { isInside && it.isFile }
    }

    @Synchronized
    fun importWeekSummaryDocument(
        weekStartDate: LocalDate,
        sourceFile: File,
    ): LoadedSync {
        val loaded = current ?: throw IOException("还没有可编辑的同步数据")
        require(sourceFile.isFile) { "所选文件不存在" }
        val extension = sourceFile.extension.lowercase()
        require(extension == "docx" || extension == "pdf") {
            "周总结只支持 docx 或 pdf 文档"
        }
        val snapshotDirectory = loaded.directory
        val filesRoot = File(snapshotDirectory, "files")
        val relativePath = "电脑周总结/$weekStartDate/" +
            "${UUID.randomUUID()}.$extension"
        val targetFile = File(filesRoot, relativePath)
        targetFile.parentFile.mkdirs()
        sourceFile.copyTo(targetFile, overwrite = false)
        try {
            val manifestFile = File(snapshotDirectory, "manifest.json")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val tables = manifest.getJSONObject("tables")
            val documents = tables.optJSONArray("week_summary_documents")
                ?: org.json.JSONArray().also {
                    tables.put("week_summary_documents", it)
                }
            documents.put(
                JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("weekStartDate", weekStartDate.toString())
                    put("displayName", sourceFile.name)
                    put("relativePath", relativePath.replace('\\', '/'))
                    put("mimeType", mimeTypeFor(extension))
                    put("uploadedAtEpochMillis", System.currentTimeMillis())
                },
            )
            adjustAttachmentCount(manifest, 1)
            writeManifest(snapshotDirectory, manifest)
            val updated = LoadedSync(
                snapshot = SyncSnapshotParser.parse(manifest.toString()),
                directory = snapshotDirectory,
            )
            current = updated
            return updated
        } catch (error: Exception) {
            targetFile.delete()
            throw error
        }
    }

    @Synchronized
    fun deleteWeekSummaryDocument(documentId: String): LoadedSync {
        val loaded = current ?: throw IOException("还没有可编辑的同步数据")
        val manifestFile = File(loaded.directory, "manifest.json")
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val documents = manifest.getJSONObject("tables")
            .getJSONArray("week_summary_documents")
        var removedPath: String? = null
        var index = 0
        while (index < documents.length()) {
            val item = documents.getJSONObject(index)
            if (item.optString("id") == documentId) {
                removedPath = item.optString("relativePath")
                documents.remove(index)
                break
            }
            index += 1
        }
        require(removedPath != null) { "周总结报告不存在" }
        val relativePathToDelete = requireNotNull(removedPath)
        adjustAttachmentCount(manifest, -1)
        writeManifest(loaded.directory, manifest)
        resolveAttachment(relativePathToDelete)?.delete()
        val updated = LoadedSync(
            snapshot = SyncSnapshotParser.parse(manifest.toString()),
            directory = loaded.directory,
        )
        current = updated
        return updated
    }

    private fun loadCurrent(): LoadedSync? {
        if (!currentPointer.isFile) return null
        val directory = File(currentPointer.readText().trim())
        val manifest = File(directory, "manifest.json")
        if (!manifest.isFile) return null
        val snapshot = SyncSnapshotParser.parse(manifest.readText(Charsets.UTF_8))
        return LoadedSync(snapshot, directory)
    }

    private fun writeCurrentPointer(directory: File) {
        val pointerTemp = File(applicationRoot, "current-snapshot.tmp")
        pointerTemp.writeText(directory.absolutePath, Charsets.UTF_8)
        Files.move(
            pointerTemp.toPath(),
            currentPointer.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun writeManifest(
        directory: File,
        manifest: JSONObject,
    ) {
        val target = File(directory, "manifest.json")
        val temporary = File(directory, "manifest.${UUID.randomUUID()}.tmp")
        temporary.writeText(manifest.toString(2), Charsets.UTF_8)
        Files.move(
            temporary.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun adjustAttachmentCount(
        manifest: JSONObject,
        delta: Int,
    ) {
        val counts = manifest.optJSONObject("counts") ?: JSONObject().also {
            manifest.put("counts", it)
        }
        counts.put("attachments", (counts.optInt("attachments") + delta).coerceAtLeast(0))
    }

    private fun mimeTypeFor(extension: String): String = when (extension) {
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }

    private fun extractSafely(zipFile: File, destination: File) {
        val destinationRoot = destination.canonicalFile
        var totalBytes = 0L
        var entryCount = 0
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                entryCount += 1
                require(entryCount <= MAX_ENTRY_COUNT) { "同步包文件数量过多" }
                val entry = entries.nextElement()
                val normalizedName = entry.name.replace('\\', '/')
                require(
                    normalizedName.isNotBlank() &&
                        !normalizedName.startsWith("/") &&
                        normalizedName.split('/').none { it == ".." },
                ) { "同步包含有无效路径" }
                val target = File(destinationRoot, normalizedName).canonicalFile
                require(target.path.startsWith(destinationRoot.path + File.separator)) {
                    "同步包含有无效路径"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                    continue
                }
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            totalBytes += read
                            require(totalBytes <= MAX_UNCOMPRESSED_BYTES) {
                                "同步数据超过允许大小"
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
        }
    }

    private fun deleteOwnedDirectory(directory: File) {
        val canonicalRoot = applicationRoot.canonicalFile
        val canonicalTarget = directory.canonicalFile
        if (canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)) {
            canonicalTarget.deleteRecursively()
        }
    }

    companion object {
        private const val MAX_ENTRY_COUNT = 100_000
        private const val MAX_UNCOMPRESSED_BYTES = 1_500L * 1024 * 1024
        private const val DESKTOP_WEEKLY_PREFIX = "电脑周总结/"

        fun defaultApplicationRoot(): File = DesktopDataLocation.resolve()
    }
}
