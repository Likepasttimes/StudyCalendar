package com.learningcalendar.app.ui.day

import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayTasksUiStateTest {
    private val date = LocalDate.of(2026, 10, 25)

    @Test
    fun completedTasksAlwaysMoveToCompletedSectionAndKeepOriginalSection() {
        val completedNew = task(
            id = "completed-new",
            section = TaskSection.NEW,
            completed = true,
        )
        val completedReview = task(
            id = "completed-review",
            section = TaskSection.REVIEW,
            completed = true,
            sourceType = TaskSourceType.AUTO_REVIEW,
        )
        val activeNew = task(
            id = "active-new",
            section = TaskSection.NEW,
            completed = false,
        )
        val activeReview = task(
            id = "active-review",
            section = TaskSection.REVIEW,
            completed = false,
            sourceType = TaskSourceType.MANUAL_REVIEW,
        )

        val state = DayTasksUiState.create(
            date = date,
            today = date,
            tasks = listOf(completedNew, completedReview, activeNew, activeReview),
        )

        assertEquals(listOf(activeNew), state.newTasks)
        assertEquals(listOf(activeReview), state.reviewTasks)
        assertEquals(listOf(completedNew, completedReview), state.completedTasks)
        assertEquals(TaskSection.NEW, completedNew.section)
        assertEquals(TaskSection.REVIEW, completedReview.section)
        assertTrue(state.completedTasks.all { it.isCompleted })
    }

    private fun task(
        id: String,
        section: TaskSection,
        completed: Boolean,
        sourceType: TaskSourceType = TaskSourceType.ORIGINAL,
    ) = StudyTask(
        id = id,
        title = id,
        scheduledDate = date,
        startTime = null,
        section = section,
        isCompleted = completed,
        spacedRepetitionEnabled = false,
        createdDate = date,
        createdAtEpochMillis = 1,
        sourceType = sourceType,
        sourceTaskId = if (sourceType == TaskSourceType.AUTO_REVIEW) "source" else null,
        reviewSequence = if (sourceType == TaskSourceType.AUTO_REVIEW) 2 else null,
    )
}
