package com.learningcalendar.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDateLogicTest {
    @Test
    fun leapYearFebruaryContainsTwentyNineDays() {
        val month = YearMonth.of(2028, 2)
        val grid = CalendarDateLogic.monthGrid(
            month = month,
            today = LocalDate.of(2028, 2, 29),
        )

        val daysInMonth = grid.filter(CalendarDay::isInDisplayedMonth)

        assertEquals(29, daysInMonth.size)
        assertEquals(LocalDate.of(2028, 2, 1), daysInMonth.first().date)
        assertEquals(LocalDate.of(2028, 2, 29), daysInMonth.last().date)
    }

    @Test
    fun gridStartsOnMondayAndShiftsAcrossYearBoundary() {
        val grid = CalendarDateLogic.monthGrid(
            month = YearMonth.of(2026, 12),
            today = LocalDate.of(2026, 10, 25),
        )

        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().date.dayOfWeek)
        assertEquals(YearMonth.of(2027, 1), CalendarDateLogic.shiftMonth(YearMonth.of(2026, 12), 1))
    }

    @Test
    fun todayIsHighlightedOnceAndOtherDaysAreNot() {
        val today = LocalDate.of(2026, 10, 25)
        val grid = CalendarDateLogic.monthGrid(
            month = YearMonth.of(2026, 10),
            today = today,
        )

        val highlightedDays = grid.filter(CalendarDay::isToday)

        assertEquals(1, highlightedDays.size)
        assertEquals(today, highlightedDays.single().date)
        assertTrue(grid.filterNot(CalendarDay::isToday).none(CalendarDay::isToday))
    }

    @Test
    fun dateSelectionClampsToTargetMonthLength() {
        val selectedLeapDay = CalendarDateLogic.selectionForMonth(
            targetMonth = YearMonth.of(2028, 2),
            currentSelection = LocalDate.of(2028, 1, 31),
        )
        val selectedInFebruary = CalendarDateLogic.selectionForMonth(
            targetMonth = YearMonth.of(2026, 2),
            currentSelection = LocalDate.of(2026, 1, 30),
        )
        val selectedMarchThirtyFirst = CalendarDateLogic.selectionForMonth(
            targetMonth = YearMonth.of(2026, 3),
            currentSelection = LocalDate.of(2026, 2, 28),
        )

        assertEquals(LocalDate.of(2028, 2, 29), selectedLeapDay)
        assertEquals(LocalDate.of(2026, 2, 28), selectedInFebruary)
        assertEquals(LocalDate.of(2026, 3, 28), selectedMarchThirtyFirst)
        assertFalse(YearMonth.of(2026, 2).isLeapYear)
    }
}
