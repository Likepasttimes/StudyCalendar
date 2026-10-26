package com.learningcalendar.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    init {
        require(endDate == startDate.plusDays(6))
    }

    companion object {
        fun containing(date: LocalDate): WeekRange {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return WeekRange(monday, monday.plusDays(6))
        }
    }
}

data class ErrorRecord(
    val id: String,
    val cardId: String,
    val errorPoint: String,
    val description: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class ErrorScreenshot(
    val id: String,
    val errorRecordId: String,
    val displayName: String,
    val localPath: String,
    val mimeType: String?,
    val createdAtEpochMillis: Long,
)

data class WeekSummaryDocument(
    val id: String,
    val weekStartDate: LocalDate,
    val displayName: String,
    val localPath: String,
    val mimeType: String?,
    val uploadedAtEpochMillis: Long,
)
