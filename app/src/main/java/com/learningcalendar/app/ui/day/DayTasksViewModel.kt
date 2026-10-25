package com.learningcalendar.app.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.ui.UiFeedback
import com.learningcalendar.app.ui.toChineseUserMessage
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayTasksUiState(
    val date: LocalDate,
    val today: LocalDate,
    val newTasks: List<StudyTask>,
    val reviewTasks: List<StudyTask>,
    val completedTasks: List<StudyTask>,
    val isLoading: Boolean,
) {
    companion object {
        fun create(
            date: LocalDate,
            today: LocalDate,
            tasks: List<StudyTask>,
        ) = DayTasksUiState(
            date = date,
            today = today,
            newTasks = tasks.filter {
                !it.isCompleted && it.section == TaskSection.NEW
            },
            reviewTasks = tasks.filter {
                !it.isCompleted && it.section == TaskSection.REVIEW
            },
            completedTasks = tasks.filter(StudyTask::isCompleted),
            isLoading = false,
        )
    }
}

class DayTasksViewModel(
    private val date: LocalDate,
    private val repository: StudyTaskRepository,
    todayProvider: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 4)
    val feedback = _feedback.asSharedFlow()
    val saving: StateFlow<Boolean> = isSaving

    val uiState: StateFlow<DayTasksUiState> = repository.observeTasksForDate(date)
        .map { tasks ->
            DayTasksUiState.create(date, todayProvider(), tasks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DayTasksUiState(
                date = date,
                today = todayProvider(),
                newTasks = emptyList(),
                reviewTasks = emptyList(),
                completedTasks = emptyList(),
                isLoading = true,
            ),
        )

    fun createTask(
        section: TaskSection,
        title: String,
        startTime: LocalTime?,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = "卡片已创建",
        onResult = onResult,
    ) {
        repository.createTask(
            title = title,
            scheduledDate = date,
            startTime = startTime,
            section = section,
        )
    }

    fun updateTask(
        taskId: String,
        title: String,
        startTime: LocalTime?,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = "卡片信息已保存",
        onResult = onResult,
    ) {
        repository.updateTask(taskId, title, startTime)
    }

    fun setCompleted(
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

    fun enableRepetition(taskId: String, onResult: (Boolean) -> Unit = {}) = launchWrite(
        successText = "已添加第 2、3、7 天复习",
        onResult = onResult,
    ) {
        repository.enableSpacedRepetition(taskId)
    }

    fun cancelRepetition(taskId: String, onResult: (Boolean) -> Unit = {}) = launchWrite(
        successText = "已移除二三七复习安排",
        onResult = onResult,
    ) {
        repository.cancelSpacedRepetition(taskId)
    }

    fun deleteTask(taskId: String, onResult: (Boolean) -> Unit) = launchWrite(
        successText = "卡片已删除",
        onResult = onResult,
    ) {
        repository.deleteTask(taskId)
    }

    private fun launchWrite(
        successText: String,
        onResult: (Boolean) -> Unit,
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
}

class DayTasksViewModelFactory(
    private val date: LocalDate,
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DayTasksViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return DayTasksViewModel(date, repository) as T
    }
}
