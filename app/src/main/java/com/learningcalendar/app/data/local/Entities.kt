package com.learningcalendar.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StudyTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["scheduledDate"]),
        Index(value = ["sourceTaskId"]),
        Index(value = ["sourceTaskId", "reviewSequence"], unique = true),
    ],
)
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val scheduledDate: String,
    val startTime: String?,
    val section: String,
    val isCompleted: Boolean,
    val spacedRepetitionEnabled: Boolean,
    val createdDate: String,
    val createdAtEpochMillis: Long,
    val sourceType: String,
    val sourceTaskId: String?,
    val reviewSequence: Int?,
)

@Entity(
    tableName = "task_content_cards",
    foreignKeys = [
        ForeignKey(
            entity = StudyTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["taskId", "sortOrder"], unique = true),
    ],
)
data class TaskContentCardEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val objective: String,
    val initialAction: String,
    val acceptanceCriteria: String,
    val createdAtEpochMillis: Long,
    val sortOrder: Long,
    val acceptanceCompleted: Boolean,
    val masteryLevel: Int?,
)

@Entity(
    tableName = "learning_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = TaskContentCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["cardId"])],
)
data class LearningSnapshotEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val kind: String,
    val displayName: String,
    val relativePath: String,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class TaskCountRow(
    val date: String,
    val taskCount: Int,
)
