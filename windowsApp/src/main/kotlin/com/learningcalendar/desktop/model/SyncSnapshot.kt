package com.learningcalendar.desktop.model

import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

data class SyncTask(
    val id: String,
    val title: String,
    val scheduledDate: LocalDate,
    val startTime: String?,
    val section: String,
    val isCompleted: Boolean,
    val spacedRepetitionEnabled: Boolean,
    val sourceType: String,
)

data class SyncContentCard(
    val id: String,
    val taskId: String,
    val objective: String,
    val initialAction: String,
    val acceptanceCriteria: String,
    val sortOrder: Long,
    val acceptanceCompleted: Boolean,
    val masteryLevel: Int?,
)

data class SyncLearningSnapshot(
    val id: String,
    val cardId: String,
    val kind: String,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
)

data class SyncErrorRecord(
    val id: String,
    val cardId: String,
    val errorPoint: String,
    val description: String,
    val createdAtEpochMillis: Long,
)

data class SyncErrorAttachment(
    val id: String,
    val errorRecordId: String,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
)

data class SyncWeekDocument(
    val id: String,
    val weekStartDate: LocalDate,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val uploadedAtEpochMillis: Long,
)

data class SyncSnapshot(
    val formatVersion: Int,
    val exportedAtEpochMillis: Long,
    val appVersion: String,
    val taskCount: Int,
    val contentCardCount: Int,
    val attachmentCount: Int,
    val tasks: List<SyncTask>,
    val contentCards: List<SyncContentCard>,
    val learningSnapshots: List<SyncLearningSnapshot>,
    val errorRecords: List<SyncErrorRecord>,
    val errorAttachments: List<SyncErrorAttachment>,
    val weekDocuments: List<SyncWeekDocument>,
) {
    val exportedAt: Instant
        get() = Instant.ofEpochMilli(exportedAtEpochMillis)
}

object SyncSnapshotParser {
    const val SUPPORTED_FORMAT_VERSION = 1

    fun parse(manifestText: String): SyncSnapshot {
        val root = JSONObject(manifestText)
        val formatVersion = root.optInt("formatVersion", -1)
        require(formatVersion == SUPPORTED_FORMAT_VERSION) {
            "不支持的数据版本：$formatVersion"
        }
        val tables = root.optJSONObject("tables")
            ?: throw IllegalArgumentException("同步数据缺少 tables")
        val counts = root.optJSONObject("counts") ?: JSONObject()

        return SyncSnapshot(
            formatVersion = formatVersion,
            exportedAtEpochMillis = root.optLong("exportedAtEpochMillis"),
            appVersion = root.optString("appVersion", "未知"),
            taskCount = counts.optInt("studyTasks"),
            contentCardCount = counts.optInt("contentCards"),
            attachmentCount = counts.optInt("attachments"),
            tasks = tables.array("study_tasks").mapObjects { item ->
                SyncTask(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    scheduledDate = LocalDate.parse(item.getString("scheduledDate")),
                    startTime = item.nullableString("startTime"),
                    section = item.optString("section", "NEW"),
                    isCompleted = item.optBoolean("isCompleted"),
                    spacedRepetitionEnabled = item.optBoolean("spacedRepetitionEnabled"),
                    sourceType = item.optString("sourceType", "ORIGINAL"),
                )
            },
            contentCards = tables.array("task_content_cards").mapObjects { item ->
                SyncContentCard(
                    id = item.getString("id"),
                    taskId = item.getString("taskId"),
                    objective = item.optString("objective"),
                    initialAction = item.optString("initialAction"),
                    acceptanceCriteria = item.optString("acceptanceCriteria"),
                    sortOrder = item.optLong("sortOrder"),
                    acceptanceCompleted = item.optBoolean("acceptanceCompleted"),
                    masteryLevel = item.nullableInt("masteryLevel"),
                )
            },
            learningSnapshots = tables.array("learning_snapshots").mapObjects { item ->
                SyncLearningSnapshot(
                    id = item.getString("id"),
                    cardId = item.getString("cardId"),
                    kind = item.optString("kind"),
                    displayName = item.optString("displayName"),
                    relativePath = item.getString("relativePath"),
                    mimeType = item.nullableString("mimeType"),
                    createdAtEpochMillis = item.optLong("createdAtEpochMillis"),
                )
            },
            errorRecords = tables.array("error_records").mapObjects { item ->
                SyncErrorRecord(
                    id = item.getString("id"),
                    cardId = item.getString("cardId"),
                    errorPoint = item.optString("errorPoint"),
                    description = item.optString("description"),
                    createdAtEpochMillis = item.optLong("createdAtEpochMillis"),
                )
            },
            errorAttachments = tables.array("error_screenshots").mapObjects { item ->
                SyncErrorAttachment(
                    id = item.getString("id"),
                    errorRecordId = item.getString("errorRecordId"),
                    displayName = item.optString("displayName"),
                    relativePath = item.getString("relativePath"),
                    mimeType = item.nullableString("mimeType"),
                    createdAtEpochMillis = item.optLong("createdAtEpochMillis"),
                )
            },
            weekDocuments = tables.array("week_summary_documents").mapObjects { item ->
                SyncWeekDocument(
                    id = item.getString("id"),
                    weekStartDate = LocalDate.parse(item.getString("weekStartDate")),
                    displayName = item.optString("displayName"),
                    relativePath = item.getString("relativePath"),
                    mimeType = item.nullableString("mimeType"),
                    uploadedAtEpochMillis = item.optLong("uploadedAtEpochMillis"),
                )
            },
        )
    }
}

private fun JSONObject.array(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { index -> transform(getJSONObject(index)) }

private fun JSONObject.nullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key)

private fun JSONObject.nullableInt(key: String): Int? =
    if (!has(key) || isNull(key)) null else optInt(key)
