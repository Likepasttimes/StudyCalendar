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
    ): StoredSnapshotFile {
        val root = rootDirectory
        if (!root.exists() && !root.mkdirs()) {
            throw IOException("无法创建学习快照文件夹")
        }
        if (!root.isDirectory) {
            throw IOException("学习快照路径不是文件夹")
        }
        val cardDirectory = File(root, cardId)
        if (!cardDirectory.exists() && !cardDirectory.mkdirs()) {
            throw IOException("无法创建卡片快照文件夹")
        }

        val sourceName = queryDisplayName(sourceUri)
        val mimeType = resolver.getType(sourceUri)
        val fallbackName = if (kind == SnapshotKind.PHOTO) "学习照片" else "学习录音"
        val extension = resolveExtension(sourceName, mimeType, kind)
        val targetFile = File(
            cardDirectory,
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
        return resolver.query(
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
            else -> if (kind == SnapshotKind.PHOTO) "jpg" else "audio"
        }
    }

    private companion object {
        const val SNAPSHOT_FOLDER_NAME = "学习快照"
    }
}
