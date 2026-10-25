package com.learningcalendar.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewScheduleTest {
    @Test
    fun createdDayIsDayOneAndMilestonesArePlusOnePlusTwoPlusSix() {
        val milestones = ReviewSchedule.milestones(LocalDate.of(2026, 10, 25))

        assertEquals(listOf(2, 3, 7), milestones.map(ReviewMilestone::sequence))
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 27),
                LocalDate.of(2026, 10, 31),
            ),
            milestones.map(ReviewMilestone::date),
        )
    }

    @Test
    fun reviewDatesCrossMonthAndYearUsingNaturalDays() {
        val newYear = ReviewSchedule.milestones(LocalDate.of(2026, 12, 30))
        val monthEnd = ReviewSchedule.milestones(LocalDate.of(2026, 1, 30))

        assertEquals(
            listOf(
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 5),
            ),
            newYear.map(ReviewMilestone::date),
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 5),
            ),
            monthEnd.map(ReviewMilestone::date),
        )
    }

    @Test
    fun leapYearFebruaryUsesTwentyNineDays() {
        val milestones = ReviewSchedule.milestones(LocalDate.of(2028, 2, 28))

        assertEquals(
            listOf(
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 3, 1),
                LocalDate.of(2028, 3, 5),
            ),
            milestones.map(ReviewMilestone::date),
        )
    }

    @Test
    fun masteryLevelsUseRequiredZeroToFourLabels() {
        assertEquals(
            listOf("看过", "能理解", "不看书能复现", "可做变式习题", "可讲解"),
            MasteryLevel.entries.map(MasteryLevel::label),
        )
        assertEquals(MasteryLevel.VIEWED, MasteryLevel.fromValue(0))
        assertEquals(MasteryLevel.EXPLAINED, MasteryLevel.fromValue(4))
        assertEquals(null, MasteryLevel.fromValue(null))
    }
}
