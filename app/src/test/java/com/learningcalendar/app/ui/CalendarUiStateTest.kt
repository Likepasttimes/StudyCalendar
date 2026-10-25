package com.learningcalendar.app.ui

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CalendarUiStateTest {
    @Test
    fun thirtyOneDayMonthKeepsEveryDayInOneSixWeekGrid() {
        val state = CalendarUiState.create(
            displayedMonth = YearMonth.of(2026, 10),
            selectedDate = LocalDate.of(2026, 10, 25),
            today = LocalDate.of(2026, 10, 25),
            taskCounts = emptyMap(),
        )

        val daysInOctober = state.weeks.flatten().filter(CalendarDayUi::isInDisplayedMonth)

        assertEquals(6, state.weeks.size)
        assertEquals(31, daysInOctober.size)
    }

    @Test
    fun selectedDateWithoutTasksProducesEmptyDayCount() {
        val selectedDate = LocalDate.of(2026, 11, 3)
        val state = CalendarUiState.create(
            displayedMonth = YearMonth.from(selectedDate),
            selectedDate = selectedDate,
            today = LocalDate.of(2026, 10, 25),
            taskCounts = emptyMap(),
        )
        val selectedDay = state.weeks
            .flatten()
            .single { it.date == selectedDate }

        assertEquals(0, selectedDay.taskCount)
        assertEquals(selectedDate, selectedDay.date)
        assertFalse(selectedDay.isToday)
    }

    @Test
    fun calendarCountsIncludeBothNewAndReviewTasksFromRepositoryMap() {
        val date = LocalDate.of(2026, 10, 25)
        val state = CalendarUiState.create(
            displayedMonth = YearMonth.from(date),
            selectedDate = date,
            today = date,
            taskCounts = mapOf(date to 4),
        )

        val day = state.weeks.flatten().single { it.date == date }

        assertEquals(4, day.taskCount)
    }
}
