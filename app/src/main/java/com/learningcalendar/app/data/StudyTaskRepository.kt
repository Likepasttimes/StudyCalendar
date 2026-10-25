package com.learningcalendar.app.data

import android.net.Uri
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.TaskSection
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface StudyTaskRepository {
    fun observeTaskCounts(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, Int>>

    fun observeTasksForDate(date: LocalDate): Flow<List<StudyTask>>

    fun observeTask(taskId: String): Flow<StudyTask?>

    fun observeCards(taskId: String): Flow<List<TaskContentCard>>

    fun observeCard(cardId: String): Flow<TaskContentCard?>

    fun observeSnapshots(cardId: String): Flow<List<LearningSnapshot>>

    suspend fun seedIfNeeded()

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
}
