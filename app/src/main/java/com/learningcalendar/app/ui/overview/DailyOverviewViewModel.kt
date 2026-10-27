package com.learningcalendar.app.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class OverviewTaskItem(
    val task: StudyTask,
    val contentCardCount: Int,
)

data class DailyOverviewUiState(
    val selectedDate: LocalDate,
    val newTasks: List<OverviewTaskItem> = emptyList(),
    val reviewTasks: List<OverviewTaskItem> = emptyList(),
    val isLoading: Boolean = true,
) {
    val allTasks: List<OverviewTaskItem>
        get() = newTasks + reviewTasks
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyOverviewViewModel(
    repository: StudyTaskRepository,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val selectedDate = MutableStateFlow(today)

    val uiState: StateFlow<DailyOverviewUiState> = selectedDate
        .flatMapLatest { date ->
            combine(
                repository.observeTasksForDate(date),
                repository.observeCardCountsForDate(date),
            ) { tasks, cardCounts ->
                val items = tasks
                    .sortedWith(
                        compareBy<StudyTask> { it.startTime == null }
                            .thenBy { it.startTime?.toString().orEmpty() }
                            .thenBy { it.createdAtEpochMillis },
                    )
                    .map { task ->
                        OverviewTaskItem(
                            task = task,
                            contentCardCount = cardCounts[task.id] ?: 0,
                        )
                    }
                DailyOverviewUiState(
                    selectedDate = date,
                    newTasks = items.filter {
                        it.task.section == TaskSection.NEW
                    },
                    reviewTasks = items.filter {
                        it.task.section == TaskSection.REVIEW
                    },
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DailyOverviewUiState(selectedDate = today),
        )

    fun showPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun showNextDay() {
        selectedDate.value = selectedDate.value.plusDays(1)
    }

    fun showToday() {
        selectedDate.value = LocalDate.now()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }
}

class DailyOverviewViewModelFactory(
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DailyOverviewViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return DailyOverviewViewModel(repository) as T
    }
}
