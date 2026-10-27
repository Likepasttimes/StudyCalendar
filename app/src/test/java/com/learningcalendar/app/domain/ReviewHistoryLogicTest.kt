package com.learningcalendar.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewHistoryLogicTest {
    private val start = LocalDate.of(2026, 10, 25)
    private val source = task(
        id = "source",
        date = start,
        sourceType = TaskSourceType.ORIGINAL,
    )
    private val reviews = listOf(2, 3, 7).map { sequence ->
        task(
            id = "review-$sequence",
            date = start.plusDays((sequence - 1).toLong()),
            sourceType = TaskSourceType.AUTO_REVIEW,
            reviewSequence = sequence,
        )
    }
    private val history = listOf(source) + reviews

    @Test
    fun firstDayOnlyShowsOriginalTask() {
        val groups = ReviewHistoryLogic.availableGroups(history, start)

        assertEquals(listOf(1), groups.map { it.day })
        assertEquals(listOf("source"), groups.single().tasks.map { it.id })
    }

    @Test
    fun secondReviewAppearsOnlyWhenItsDateArrives() {
        val groups = ReviewHistoryLogic.availableGroups(history, start.plusDays(1))

        assertEquals(listOf(1, 2), groups.map { it.day })
        assertEquals(
            listOf("source", "review-2"),
            groups.single { it.day == 2 }.tasks.map { it.id },
        )
    }

    @Test
    fun laterReviewGroupsStayHiddenUntilReached() {
        val dayThree = ReviewHistoryLogic.availableGroups(history, start.plusDays(2))
        val daySeven = ReviewHistoryLogic.availableGroups(history, start.plusDays(6))

        assertEquals(listOf(1, 2, 3), dayThree.map { it.day })
        assertEquals(listOf(1, 2, 3, 7), daySeven.map { it.day })
        assertEquals(
            listOf("source", "review-2", "review-3", "review-7"),
            daySeven.last().tasks.map { it.id },
        )
    }

    private fun task(
        id: String,
        date: LocalDate,
        sourceType: TaskSourceType,
        reviewSequence: Int? = null,
    ) = StudyTask(
        id = id,
        title = id,
        scheduledDate = date,
        startTime = null,
        section = if (sourceType == TaskSourceType.ORIGINAL) {
            TaskSection.NEW
        } else {
            TaskSection.REVIEW
        },
        isCompleted = sourceType == TaskSourceType.ORIGINAL,
        spacedRepetitionEnabled = sourceType == TaskSourceType.ORIGINAL,
        createdDate = start,
        createdAtEpochMillis = reviewSequence?.toLong() ?: 0,
        sourceType = sourceType,
        sourceTaskId = if (sourceType == TaskSourceType.AUTO_REVIEW) "source" else null,
        reviewSequence = reviewSequence,
    )
}
