package com.learningcalendar.app.data

import android.net.Uri
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.ErrorRecord
import com.learningcalendar.app.domain.ErrorScreenshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.WeekSummaryDocument
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface StudyTaskRepository {
    fun observeTaskCounts(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, Int>>

    fun observeTasksForDate(date: LocalDate): Flow<List<StudyTask>>

    fun observeCardCountsForDate(date: LocalDate): Flow<Map<String, Int>>

    fun observeTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<StudyTask>>

    fun observeTask(taskId: String): Flow<StudyTask?>

    fun observeReviewChain(taskId: String): Flow<List<StudyTask>>

    fun observeCards(taskId: String): Flow<List<TaskContentCard>>

    fun observeCard(cardId: String): Flow<TaskContentCard?>

    fun observeSnapshots(cardId: String): Flow<List<LearningSnapshot>>

    suspend fun seedIfNeeded()

    suspend fun repairSpacedRepetitionSchedules()

    suspend fun createTask(
        title: String,
        scheduledDate: LocalDate,
        startTime: LocalTime?,
        section: TaskSection,
    ): String

    suspend fun updateTask(
        taskId: String,
        title: String,
        startTime: LocalTime?,
    )

    suspend fun deleteTask(taskId: String)

    suspend fun setTaskCompleted(taskId: String, completed: Boolean)

    suspend fun enableSpacedRepetition(sourceTaskId: String)

    suspend fun cancelSpacedRepetition(sourceTaskId: String)

    suspend fun addCard(
        taskId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    ): String

    suspend fun updateCard(
        cardId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    )

    suspend fun deleteCard(cardId: String)

    suspend fun setCardAcceptance(
        cardId: String,
        completed: Boolean,
        masteryLevel: Int?,
    )

    suspend fun importSnapshot(
        cardId: String,
        sourceUri: Uri,
        kind: SnapshotKind,
    ): String

    suspend fun deleteLearningSnapshot(snapshotId: String)

    fun observeErrorRecords(cardId: String): Flow<List<ErrorRecord>>

    fun observeErrorScreenshotsForCard(cardId: String): Flow<List<ErrorScreenshot>>

    suspend fun createErrorRecord(
        cardId: String,
        errorPoint: String,
        description: String,
    ): String

    suspend fun createErrorRecordWithAttachments(
        cardId: String,
        errorPoint: String,
        description: String,
        photoUris: List<Uri>,
        fileUris: List<Uri>,
    ): String

    suspend fun updateErrorRecord(
        recordId: String,
        errorPoint: String,
        description: String,
    )

    suspend fun deleteErrorRecord(recordId: String)

    suspend fun importErrorScreenshot(
        recordId: String,
        sourceUri: Uri,
    ): String

    suspend fun importErrorAttachment(
        recordId: String,
        sourceUri: Uri,
        kind: SnapshotKind,
    ): String

    suspend fun deleteErrorScreenshot(screenshotId: String)

    fun observeWeekSummaryDocuments(weekStartDate: LocalDate): Flow<List<WeekSummaryDocument>>

    fun observeWeekSummaryDocumentsUploadedBetween(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WeekSummaryDocument>>

    fun observeWeekSummaryDocumentsForMonth(
        month: java.time.YearMonth,
    ): Flow<List<WeekSummaryDocument>>

    suspend fun importWeekSummaryDocument(
        weekStartDate: LocalDate,
        sourceUri: Uri,
    ): String

    suspend fun deleteWeekSummaryDocument(documentId: String)
}
