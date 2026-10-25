package com.learningcalendar.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.CalendarDateLogic
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.StudyTask
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: StudyTaskRepository,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    private val restoredMonth = savedStateHandle
        .get<String>(DISPLAYED_MONTH_KEY)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    private val restoredDate = savedStateHandle
        .get<String>(SELECTED_DATE_KEY)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private val initialMonth = restoredMonth ?: YearMonth.from(restoredDate ?: today)
    private val initialSelectedDate = restoredDate
        ?.takeIf { YearMonth.from(it) == initialMonth }
        ?: if (YearMonth.from(today) == initialMonth) {
            today
        } else {
            initialMonth.atDay(1)
        }

    private data class SelectionState(
        val month: YearMonth,
        val date: LocalDate,
    )

    private val selection = MutableStateFlow(
        SelectionState(initialMonth, initialSelectedDate),
    )
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 3)
    val saving: StateFlow<Boolean> = isSaving
    val feedback = _feedback.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = selection
        .flatMapLatest { current ->
            val grid = CalendarDateLogic.monthGrid(current.month, today)
            combine(
                repository.observeTaskCounts(
                    startDate = grid.first().date,
                    endDate = grid.last().date,
                ),
                repository.observeTasksForDate(current.date),
            ) { counts, selectedTasks ->
                CalendarUiState.create(
                    displayedMonth = current.month,
                    selectedDate = current.date,
                    today = today,
                    taskCounts = counts,
                    selectedTasks = selectedTasks,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState.create(
                displayedMonth = initialMonth,
                selectedDate = initialSelectedDate,
                today = today,
                taskCounts = emptyMap(),
            ),
        )

    init {
        persistSelection(initialMonth, initialSelectedDate)
    }

    fun selectDate(date: LocalDate) {
        updateSelection(YearMonth.from(date), date)
    }

    fun showPreviousMonth() {
        val targetMonth = CalendarDateLogic.shiftMonth(selection.value.month, -1)
        updateSelection(
            targetMonth,
            CalendarDateLogic.selectionForMonth(targetMonth, selection.value.date),
        )
    }

    fun showNextMonth() {
        val targetMonth = CalendarDateLogic.shiftMonth(selection.value.month, 1)
        updateSelection(
            targetMonth,
            CalendarDateLogic.selectionForMonth(targetMonth, selection.value.date),
        )
    }

    fun showToday() {
        updateSelection(YearMonth.from(today), today)
    }

    fun createTodayTask(
        title: String,
        startTime: LocalTime?,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = "卡片已创建",
        onResult = onResult,
        onSuccess = ::showToday,
    ) {
        repository.createTask(
            title = title,
            scheduledDate = today,
            startTime = startTime,
            section = TaskSection.NEW,
        )
    }

    fun setTaskCompleted(
        task: StudyTask,
        completed: Boolean,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = if (!completed && task.spacedRepetitionEnabled) {
            "卡片已取消完成，二三七复习安排已移除"
        } else {
            "卡片状态已更新"
        },
        onResult = onResult,
    ) {
        repository.setTaskCompleted(task.id, completed)
    }

    fun enableRepetition(taskId: String, onResult: (Boolean) -> Unit) = launchWrite(
        successText = "已添加第 2、3、7 天复习",
        onResult = onResult,
    ) {
        repository.enableSpacedRepetition(taskId)
    }

    fun cancelRepetition(taskId: String, onResult: (Boolean) -> Unit) = launchWrite(
        successText = "已移除二三七复习安排",
        onResult = onResult,
    ) {
        repository.cancelSpacedRepetition(taskId)
    }

    private fun launchWrite(
        successText: String,
        onResult: (Boolean) -> Unit,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        if (isSaving.value) {
            onResult(false)
            return
        }
        isSaving.value = true
        viewModelScope.launch {
            try {
                block()
                onSuccess()
                _feedback.emit(UiFeedback(successText))
                onResult(true)
            } catch (error: Exception) {
                _feedback.emit(
                    UiFeedback(
                        text = error.toChineseUserMessage(),
                        isError = true,
                    ),
                )
                onResult(false)
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun updateSelection(targetMonth: YearMonth, selectedDate: LocalDate) {
        selection.value = SelectionState(targetMonth, selectedDate)
        persistSelection(targetMonth, selectedDate)
    }

    private fun persistSelection(month: YearMonth, date: LocalDate) {
        savedStateHandle[DISPLAYED_MONTH_KEY] = month.toString()
        savedStateHandle[SELECTED_DATE_KEY] = date.toString()
    }

    private companion object {
        const val DISPLAYED_MONTH_KEY = "displayed_month"
        const val SELECTED_DATE_KEY = "selected_date"
    }
}

class CalendarViewModelFactory(
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return CalendarViewModel(
            savedStateHandle = extras.createSavedStateHandle(),
            repository = repository,
        ) as T
    }
}
