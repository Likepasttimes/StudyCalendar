package com.learningcalendar.app.data.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.core.content.edit
import com.learningcalendar.app.data.local.ErrorRecordEntity
import com.learningcalendar.app.data.local.ErrorScreenshotEntity
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import com.learningcalendar.app.data.local.LearningSnapshotEntity
import com.learningcalendar.app.data.local.LocalSnapshotStorage
import com.learningcalendar.app.data.local.StudyTaskEntity
import com.learningcalendar.app.data.local.TaskContentCardEntity
import com.learningcalendar.app.data.local.WeekSummaryDocumentEntity
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SavedComputerConnection(
    val host: String,
    val port: String,
    val syncToken: String?,
)

data class ComputerSyncResult(
    val taskCount: Int,
    val contentCardCount: Int,
    val attachmentCount: Int,
    val uploadedBytes: Long,
)

class ComputerSyncRepository(
    context: Context,
    private val database: LearningCalendarDatabase,
    private val snapshotStorage: LocalSnapshotStorage,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "computer_sync",
        Context.MODE_PRIVATE,
    )
    private val syncMutex = Mutex()

    fun savedConnection(): SavedComputerConnection = SavedComputerConnection(
        host = preferences.getString(KEY_HOST, "").orEmpty(),
        port = preferences.getString(KEY_PORT, DEFAULT_PORT).orEmpty(),
        syncToken = preferences.getString(KEY_SYNC_TOKEN, null),
    )

    suspend fun syncToComputer(
        host: String,
        portText: String,
        pairingCode: String,
    ): ComputerSyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val normalizedHost = normalizeHost(host)
            val port = parsePort(portText)
            require(pairingCode.trim().matches(Regex("\\d{6}"))) {
                "配对码应为电脑界面显示的 6 位数字"
            }
            performSync(
                host = normalizedHost,
                port = port,
                pairingCode = pairingCode.trim(),
                syncToken = null,
            )
        }
    }

    suspend fun autoSyncIfConfigured(): Boolean = withContext(Dispatchers.IO) {
        val saved = savedConnection()
        val syncToken = saved.syncToken?.takeIf(String::isNotBlank) ?: return@withContext false
        if (saved.host.isBlank()) return@withContext false
        syncMutex.withLock {
            runCatching {
                performSync(
                    host = normalizeHost(saved.host),
                    port = parsePort(saved.port),
                    pairingCode = null,
                    syncToken = syncToken,
                    connectTimeoutMillis = 4_000,
                )
            }.isSuccess
        }
    }

    private suspend fun performSync(
        host: String,
        port: Int,
        pairingCode: String?,
        syncToken: String?,
        connectTimeoutMillis: Int = 10_000,
    ): ComputerSyncResult {
        val bundle = createBundle()
        try {
            val endpoint = URL("http://$host:$port$SYNC_ENDPOINT")
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = connectTimeoutMillis
                readTimeout = 180_000
                setRequestProperty("Content-Type", "application/zip")
                pairingCode?.let { setRequestProperty("X-Pairing-Code", it) }
                syncToken?.let { setRequestProperty("X-Sync-Token", it) }
                setRequestProperty("X-Format-Version", SYNC_FORMAT_VERSION.toString())
                setFixedLengthStreamingMode(bundle.file.length())
            }
            try {
                connection.outputStream.buffered().use { output ->
                    bundle.file.inputStream().buffered().use { input ->
                        input.copyTo(output)
                    }
                }
                val status = connection.responseCode
                val responseText = (
                    if (status in 200..299) connection.inputStream else connection.errorStream
                    )?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching {
                        JSONObject(responseText).optString("message")
                    }.getOrNull()?.takeIf(String::isNotBlank)
                    throw IOException(message ?: "电脑拒绝了同步请求（$status）")
                }
                runCatching {
                    JSONObject(responseText).optString(KEY_SYNC_TOKEN)
                }.getOrNull()
                    ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
                    ?.let { issuedToken ->
                        preferences.edit {
                            putString(KEY_HOST, host)
                            putString(KEY_PORT, port.toString())
                            putString(KEY_SYNC_TOKEN, issuedToken)
                        }
                    }
            } finally {
                connection.disconnect()
            }

            preferences.edit {
                putString(KEY_HOST, host)
                putString(KEY_PORT, port.toString())
            }

            return ComputerSyncResult(
                taskCount = bundle.counts.taskCount,
                contentCardCount = bundle.counts.contentCardCount,
                attachmentCount = bundle.counts.attachmentCount,
                uploadedBytes = bundle.file.length(),
            )
        } finally {
            bundle.file.delete()
        }
    }

    private fun parsePort(portText: String): Int {
        val port = portText.trim().toIntOrNull()
            ?: throw IllegalArgumentException("电脑端口格式不正确")
        require(port in 1..65535) { "电脑端口应在 1 到 65535 之间" }
        return port
    }

    internal suspend fun createBackupForTest(): File = createBundle().file

    private suspend fun createBundle(): SyncBundle = database.withTransaction {
        val tasks = database.studyTaskDao().getAllTasksForSync()
        val cards = database.studyTaskDao().getAllCardsForSync()
        val snapshots = database.studyTaskDao().getAllSnapshotsForSync()
        val errorRecords = database.studyTaskDao().getAllErrorRecordsForSync()
        val errorScreenshots = database.studyTaskDao().getAllErrorScreenshotsForSync()
        val weekDocuments = database.studyTaskDao().getAllWeekSummaryDocumentsForSync()

        val manifest = JSONObject().apply {
            put("formatVersion", SYNC_FORMAT_VERSION)
            put("exportedAtEpochMillis", System.currentTimeMillis())
            put("appVersion", "2.7.0")
            put(
                "tables",
                JSONObject().apply {
                    put("study_tasks", tasks.toJsonArray(StudyTaskEntity::toSyncJson))
                    put("task_content_cards", cards.toJsonArray(TaskContentCardEntity::toSyncJson))
                    put("learning_snapshots", snapshots.toJsonArray(LearningSnapshotEntity::toSyncJson))
                    put("error_records", errorRecords.toJsonArray(ErrorRecordEntity::toSyncJson))
                    put("error_screenshots", errorScreenshots.toJsonArray(ErrorScreenshotEntity::toSyncJson))
                    put(
                        "week_summary_documents",
                        weekDocuments.toJsonArray(WeekSummaryDocumentEntity::toSyncJson),
                    )
                },
            )
        }

        val attachmentPaths = buildList {
            addAll(snapshots.map { it.relativePath })
            addAll(errorScreenshots.map { it.relativePath })
            addAll(weekDocuments.map { it.relativePath })
        }.toSet()
        val counts = SyncCounts(
            taskCount = tasks.size,
            contentCardCount = cards.size,
            attachmentCount = attachmentPaths.size,
        )
        manifest.put(
            "counts",
            JSONObject().apply {
                put("studyTasks", counts.taskCount)
                put("contentCards", counts.contentCardCount)
                put("attachments", counts.attachmentCount)
            },
        )

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(Instant.now().atZone(ZoneId.systemDefault()))
        val bundleFile = File(appContext.cacheDir, "学习日历-电脑同步-$timestamp.zip")
        ZipOutputStream(BufferedOutputStream(bundleFile.outputStream())).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            attachmentPaths.sorted().forEach { relativePath ->
                val source = snapshotStorage.resolveExistingFile(relativePath) ?: return@forEach
                val entryName = "files/" + relativePath.replace('\\', '/')
                zip.putNextEntry(ZipEntry(entryName))
                source.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        SyncBundle(bundleFile, counts)
    }

    private fun normalizeHost(rawHost: String): String {
        val host = rawHost.trim()
            .removePrefix("http://")
            .trimEnd('/')
        require(host.isNotBlank()) { "请填写电脑地址" }
        require(!host.contains('/') && !host.contains(' ')) { "电脑地址格式不正确" }
        return host
    }

    private data class SyncCounts(
        val taskCount: Int,
        val contentCardCount: Int,
        val attachmentCount: Int,
    )

    private data class SyncBundle(
        val file: File,
        val counts: SyncCounts,
    )

    private companion object {
        const val SYNC_FORMAT_VERSION = 1
        const val SYNC_ENDPOINT = "/api/v1/sync"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_SYNC_TOKEN = "syncToken"
        const val DEFAULT_PORT = "8765"
    }
}

private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().apply {
        forEach { value -> put(transform(value)) }
    }

private fun StudyTaskEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("scheduledDate", scheduledDate)
    put("startTime", startTime ?: JSONObject.NULL)
    put("section", section)
    put("isCompleted", isCompleted)
    put("spacedRepetitionEnabled", spacedRepetitionEnabled)
    put("createdDate", createdDate)
    put("createdAtEpochMillis", createdAtEpochMillis)
    put("sourceType", sourceType)
    put("sourceTaskId", sourceTaskId ?: JSONObject.NULL)
    put("reviewSequence", reviewSequence ?: JSONObject.NULL)
    put("repetitionStartDate", repetitionStartDate ?: JSONObject.NULL)
}

private fun TaskContentCardEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("taskId", taskId)
    put("objective", objective)
    put("initialAction", initialAction)
    put("acceptanceCriteria", acceptanceCriteria)
    put("createdAtEpochMillis", createdAtEpochMillis)
    put("sortOrder", sortOrder)
    put("acceptanceCompleted", acceptanceCompleted)
    put("masteryLevel", masteryLevel ?: JSONObject.NULL)
}

private fun LearningSnapshotEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("cardId", cardId)
    put("kind", kind)
    put("displayName", displayName)
    put("relativePath", relativePath)
    put("mimeType", mimeType ?: JSONObject.NULL)
    put("createdAtEpochMillis", createdAtEpochMillis)
}

private fun ErrorRecordEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("cardId", cardId)
    put("errorPoint", errorPoint)
    put("description", description)
    put("createdAtEpochMillis", createdAtEpochMillis)
    put("updatedAtEpochMillis", updatedAtEpochMillis)
}

private fun ErrorScreenshotEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("errorRecordId", errorRecordId)
    put("displayName", displayName)
    put("relativePath", relativePath)
    put("mimeType", mimeType ?: JSONObject.NULL)
    put("createdAtEpochMillis", createdAtEpochMillis)
}

private fun WeekSummaryDocumentEntity.toSyncJson() = JSONObject().apply {
    put("id", id)
    put("weekStartDate", weekStartDate)
    put("displayName", displayName)
    put("relativePath", relativePath)
    put("mimeType", mimeType ?: JSONObject.NULL)
    put("uploadedAtEpochMillis", uploadedAtEpochMillis)
}
