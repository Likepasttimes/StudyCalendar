package com.learningcalendar.app.domain

import java.time.LocalDate
import java.time.LocalTime

enum class TaskSection {
    NEW,
    REVIEW,
}

enum class TaskSourceType {
    ORIGINAL,
    AUTO_REVIEW,
    MANUAL_REVIEW,
}

enum class MasteryLevel(
    val value: Int,
    val label: String,
) {
    VIEWED(0, "看过"),
    UNDERSTOOD(1, "能理解"),
    REPRODUCED(2, "不看书能复现"),
    VARIANT(3, "可做变式习题"),
    EXPLAINED(4, "可讲解"),
    ;

    companion object {
        fun fromValue(value: Int?): MasteryLevel? =
            entries.firstOrNull { it.value == value }
    }
}

data class StudyTask(
    val id: String,
    val title: String,
    val scheduledDate: LocalDate,
    val startTime: LocalTime?,
    val section: TaskSection,
    val isCompleted: Boolean,
    val spacedRepetitionEnabled: Boolean,
    val createdDate: LocalDate,
    val createdAtEpochMillis: Long,
    val sourceType: TaskSourceType,
    val sourceTaskId: String?,
    val reviewSequence: Int?,
) {
    val canOfferSpacedRepetition: Boolean
        get() = sourceType == TaskSourceType.ORIGINAL &&
            isCompleted &&
            (!spacedRepetitionEnabled || section == TaskSection.REVIEW)
}

data class TaskContentCard(
    val id: String,
    val taskId: String,
    val objective: String,
    val initialAction: String,
    val acceptanceCriteria: String,
    val createdAtEpochMillis: Long,
    val sortOrder: Long,
    val acceptanceCompleted: Boolean,
    val masteryLevel: Int?,
)
