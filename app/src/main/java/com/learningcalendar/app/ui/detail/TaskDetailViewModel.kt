package com.learningcalendar.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.ui.UiFeedback
import com.learningcalendar.app.ui.toChineseUserMessage
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: StudyTask?,
    val cards: List<TaskContentCard>,
    val isLoading: Boolean,
)

class TaskDetailViewModel(
    private val taskId: String,
    private val repository: StudyTaskRepository,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 4)
    val feedback = _feedback.asSharedFlow()
    val saving: StateFlow<Boolean> = isSaving

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(taskId),
        repository.observeCards(taskId),
    ) { task, cards ->
        TaskDetailUiState(
            task = task,
            cards = cards,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState(
            task = null,
            cards = emptyList(),
            isLoading = true,
        ),
    )

    fun updateTask(
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

    fun enableRepetition(onResult: (Boolean) -> Unit = {}) = launchWrite(
        successText = "已添加第 2、3、7 天复习",
        onResult = onResult,
    ) {
        repository.enableSpacedRepetition(taskId)
    }

    fun cancelRepetition(onResult: (Boolean) -> Unit = {}) = launchWrite(
        successText = "已移除二三七复习安排",
        onResult = onResult,
    ) {
        repository.cancelSpacedRepetition(taskId)
    }

    fun deleteTask(onResult: (Boolean) -> Unit) = launchWrite(
        successText = "卡片已删除",
        onResult = onResult,
    ) {
        repository.deleteTask(taskId)
    }

    fun addCard(
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = "内容卡片已添加",
        onResult = onResult,
    ) {
        repository.addCard(taskId, objective, initialAction, acceptanceCriteria)
    }

    fun updateCard(
        cardId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        successText = "内容卡片已保存",
        onResult = onResult,
    ) {
        repository.updateCard(cardId, objective, initialAction, acceptanceCriteria)
    }

    fun deleteCard(cardId: String, onResult: (Boolean) -> Unit) = launchWrite(
        successText = "内容卡片已删除",
        onResult = onResult,
    ) {
        repository.deleteCard(cardId)
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

class TaskDetailViewModelFactory(
    private val taskId: String,
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskDetailViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return TaskDetailViewModel(taskId, repository) as T
    }
}
