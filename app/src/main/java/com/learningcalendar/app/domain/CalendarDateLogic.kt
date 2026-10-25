package com.learningcalendar.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

data class CalendarDay(
    val date: LocalDate,
    val isInDisplayedMonth: Boolean,
    val isToday: Boolean,
)

object CalendarDateLogic {
    const val DAYS_PER_WEEK = 7
    const val GRID_WEEKS = 6

    fun monthGrid(month: YearMonth, today: LocalDate): List<CalendarDay> {
        val gridStart = month
            .atDay(1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        return List(DAYS_PER_WEEK * GRID_WEEKS) { index ->
            val date = gridStart.plusDays(index.toLong())
            CalendarDay(
                date = date,
                isInDisplayedMonth = YearMonth.from(date) == month,
                isToday = date == today,
            )
        }
    }

    fun shiftMonth(month: YearMonth, monthOffset: Long): YearMonth =
        month.plusMonths(monthOffset)

    fun selectionForMonth(targetMonth: YearMonth, currentSelection: LocalDate): LocalDate =
        targetMonth.atDay(min(currentSelection.dayOfMonth, targetMonth.lengthOfMonth()))
}
