package com.learningcalendar.app.domain

enum class SnapshotKind {
    PHOTO,
    AUDIO,
    FILE,
}

data class LearningSnapshot(
    val id: String,
    val cardId: String,
    val kind: SnapshotKind,
    val displayName: String,
    val localPath: String,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
)

data class StoredSnapshotFile(
    val relativePath: String,
    val displayName: String,
    val mimeType: String?,
)
