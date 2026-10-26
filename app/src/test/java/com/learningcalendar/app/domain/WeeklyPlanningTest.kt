package com.learningcalendar.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyPlanningTest {
    @Test
    fun weekRangeAlwaysStartsOnMondayAndEndsOnSunday() {
        val range = WeekRange.containing(LocalDate.of(2026, 10, 28))

        assertEquals(LocalDate.of(2026, 10, 26), range.startDate)
        assertEquals(LocalDate.of(2026, 11, 1), range.endDate)
    }
}
