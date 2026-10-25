package com.learningcalendar.app.ui

import com.learningcalendar.app.domain.CalendarDateLogic
import com.learningcalendar.app.domain.StudyTask
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDayUi(
    val date: LocalDate,
    val isInDisplayedMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val taskCount: Int,
)

data class CalendarUiState(
    val displayedMonth: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val weeks: List<List<CalendarDayUi>>,
    val selectedTasks: List<StudyTask>,
) {
    companion object {
        fun create(
            displayedMonth: YearMonth,
            selectedDate: LocalDate,
            today: LocalDate,
            taskCounts: Map<LocalDate, Int>,
            selectedTasks: List<StudyTask> = emptyList(),
        ): CalendarUiState {
            val days = CalendarDateLogic.monthGrid(displayedMonth, today).map { day ->
                CalendarDayUi(
                    date = day.date,
                    isInDisplayedMonth = day.isInDisplayedMonth,
                    isToday = day.isToday,
                    isSelected = day.date == selectedDate,
                    taskCount = taskCounts[day.date] ?: 0,
                )
            }

            return CalendarUiState(
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                today = today,
                weeks = days.chunked(CalendarDateLogic.DAYS_PER_WEEK),
                selectedTasks = selectedTasks,
            )
        }
    }
}
