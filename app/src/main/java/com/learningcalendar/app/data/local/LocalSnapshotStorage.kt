package com.learningcalendar.app.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StoredSnapshotFile
import java.io.File
import java.io.IOException
import java.util.UUID

class LocalSnapshotStorage(
    private val context: Context,
) {
    private val resolver = context.contentResolver
    private val rootDirectory: File
        get() = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            SNAPSHOT_FOLDER_NAME,
        )

    fun save(
        cardId: String,
        sourceUri: Uri,
        kind: SnapshotKind,
    ): StoredSnapshotFile = saveInFolder(cardId, sourceUri, kind)

    fun saveErrorScreenshot(
        errorRecordId: String,
        sourceUri: Uri,
    ): StoredSnapshotFile = saveInFolder(
        relativeFolder = "错误记录/$errorRecordId",
        sourceUri = sourceUri,
        kind = SnapshotKind.PHOTO,
    )

    fun saveErrorFile(
        errorRecordId: String,
        sourceUri: Uri,
    ): StoredSnapshotFile = saveInFolder(
        relativeFolder = "错误记录/$errorRecordId",
        sourceUri = sourceUri,
        kind = SnapshotKind.FILE,
    )

    fun saveWeekSummary(
        weekStartDate: String,
        sourceUri: Uri,
    ): StoredSnapshotFile = saveInFolder(
        relativeFolder = "周总结/$weekStartDate",
        sourceUri = sourceUri,
        kind = SnapshotKind.FILE,
        allowedExtensions = setOf("docx", "pdf"),
    )

    private fun saveInFolder(
        relativeFolder: String,
        sourceUri: Uri,
        kind: SnapshotKind,
        allowedExtensions: Set<String>? = null,
    ): StoredSnapshotFile {
        val root = rootDirectory
        if (!root.exists() && !root.mkdirs()) {
            throw IOException("无法创建学习快照文件夹")
        }
        if (!root.isDirectory) {
            throw IOException("学习快照路径不是文件夹")
        }
        val destinationDirectory = File(root, relativeFolder)
        if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
            throw IOException("无法创建快照文件夹")
        }

        val sourceName = queryDisplayName(sourceUri)
        val mimeType = resolver.getType(sourceUri)
        val fallbackName = when (kind) {
            SnapshotKind.PHOTO -> "学习照片"
            SnapshotKind.AUDIO -> "学习录音"
            SnapshotKind.FILE -> "学习文件"
        }
        val extension = resolveExtension(sourceName, mimeType, kind)
        if (allowedExtensions != null && extension !in allowedExtensions) {
            throw IOException("只支持上传 docx 或 pdf 文档")
        }
        val targetFile = File(
            destinationDirectory,
            "snapshot_${UUID.randomUUID()}.$extension",
        )

        try {
            val input = resolver.openInputStream(sourceUri)
                ?: throw IOException("无法读取所选文件")
            input.use { source ->
                targetFile.outputStream().buffered().use { target ->
                    source.copyTo(target)
                }
            }
        } catch (error: Exception) {
            targetFile.delete()
            throw error
        }

        return StoredSnapshotFile(
            relativePath = targetFile.relativeTo(root).invariantSeparatorsPath,
            displayName = sourceName?.takeIf(String::isNotBlank) ?: fallbackName,
            mimeType = mimeType,
        )
    }

    fun resolvePath(relativePath: String): String =
        File(rootDirectory, relativePath).absolutePath

    fun delete(relativePath: String) {
        File(rootDirectory, relativePath).delete()
    }

    fun deleteAll(relativePaths: Collection<String>) {
        relativePaths.forEach(::delete)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val queriedName = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
        return queriedName ?: uri.path
            ?.takeIf { uri.scheme == "file" }
            ?.let { path -> File(path).name }
    }

    private fun resolveExtension(
        displayName: String?,
        mimeType: String?,
        kind: SnapshotKind,
    ): String {
        val fromName = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.length in 2..8 && it.all(Char::isLetterOrDigit) }
        if (fromName != null) return fromName

        return when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "application/pdf" -> "pdf"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            else -> when (kind) {
                SnapshotKind.PHOTO -> "jpg"
                SnapshotKind.AUDIO -> "audio"
                SnapshotKind.FILE -> "bin"
            }
        }
    }

    private companion object {
        const val SNAPSHOT_FOLDER_NAME = "学习快照"
    }
}
