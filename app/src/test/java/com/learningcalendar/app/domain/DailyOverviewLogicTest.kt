package com.learningcalendar.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyOverviewLogicTest {
    private val date = LocalDate.of(2026, 10, 27)

    @Test
    fun calculatesNewAndReviewCompletionSeparately() {
        val tasks = listOf(
            task("new-done", TaskSection.NEW, completed = true),
            task("new-open", TaskSection.NEW, completed = false),
            task("review-done", TaskSection.REVIEW, completed = true),
            task("review-done-2", TaskSection.REVIEW, completed = true),
        )

        assertEquals(
            CompletionSummary(completed = 1, total = 2),
            DailyOverviewLogic.summary(DailyOverviewLogic.newTasks(tasks)),
        )
        assertEquals(
            CompletionSummary(completed = 2, total = 2),
            DailyOverviewLogic.summary(DailyOverviewLogic.reviewTasks(tasks)),
        )
    }

    @Test
    fun emptyTaskGroupUsesZeroRate() {
        assertEquals(
            CompletionSummary(completed = 0, total = 0).copy(completed = 0),
            DailyOverviewLogic.summary(emptyList()),
        )
        assertEquals(0, DailyOverviewLogic.summary(emptyList()).rate)
    }

    private fun task(
        id: String,
        section: TaskSection,
        completed: Boolean,
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
        sourceType = if (section == TaskSection.NEW) {
            TaskSourceType.ORIGINAL
        } else {
            TaskSourceType.MANUAL_REVIEW
        },
        sourceTaskId = null,
        reviewSequence = null,
    )
}
