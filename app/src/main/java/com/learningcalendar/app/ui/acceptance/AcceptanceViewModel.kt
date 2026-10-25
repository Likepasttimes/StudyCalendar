package com.learningcalendar.app.ui.acceptance

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.ui.UiFeedback
import com.learningcalendar.app.ui.toChineseUserMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AcceptanceUiState(
    val card: TaskContentCard?,
    val task: StudyTask?,
    val snapshots: List<LearningSnapshot>,
    val isLoading: Boolean,
)

class AcceptanceViewModel(
    private val cardId: String,
    private val repository: StudyTaskRepository,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 3)
    val feedback = _feedback.asSharedFlow()
    val saving: StateFlow<Boolean> = isSaving

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AcceptanceUiState> =
        combine(
            repository.observeCard(cardId),
            repository.observeSnapshots(cardId),
        ) { card, snapshots -> card to snapshots }
            .flatMapLatest { (card, snapshots) ->
                if (card == null) {
                    flowOf(
                        AcceptanceUiState(
                            card = null,
                            task = null,
                            snapshots = snapshots,
                            isLoading = false,
                        ),
                    )
                } else {
                    repository.observeTask(card.taskId).map { task ->
                        AcceptanceUiState(
                            card = card,
                            task = task,
                            snapshots = snapshots,
                            isLoading = false,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AcceptanceUiState(
                    card = null,
                    task = null,
                    snapshots = emptyList(),
                    isLoading = true,
                ),
            )

    fun setCompleted(completed: Boolean) {
        updateAcceptance(completed = completed, masteryLevel = null)
    }

    fun selectMastery(level: Int) {
        updateAcceptance(completed = true, masteryLevel = level)
    }

    fun importSnapshot(
        sourceUri: Uri,
        kind: SnapshotKind,
    ) {
        if (isSaving.value) {
            return
        }
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.importSnapshot(
                    cardId = cardId,
                    sourceUri = sourceUri,
                    kind = kind,
                )
                _feedback.emit(
                    UiFeedback(
                        when (kind) {
                            SnapshotKind.PHOTO -> "照片已保存到本地"
                            SnapshotKind.AUDIO -> "录音已保存到本地"
                            SnapshotKind.FILE -> "文件已保存到本地"
                        },
                    ),
                )
            } catch (error: Exception) {
                _feedback.emit(
                    UiFeedback(
                        text = error.toChineseUserMessage(),
                        isError = true,
                    ),
                )
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun updateAcceptance(completed: Boolean, masteryLevel: Int?) {
        if (isSaving.value) {
            return
        }
        isSaving.value = true
        viewModelScope.launch {
            try {
                repository.setCardAcceptance(
                    cardId = cardId,
                    completed = completed,
                    masteryLevel = masteryLevel,
                )
                _feedback.emit(
                    UiFeedback(
                        if (completed) "验收状态已保存" else "已取消完成并清除掌握度",
                    ),
                )
            } catch (error: Exception) {
                _feedback.emit(
                    UiFeedback(
                        text = error.toChineseUserMessage(),
                        isError = true,
                    ),
                )
            } finally {
                isSaving.value = false
            }
        }
    }
}

class AcceptanceViewModelFactory(
    private val cardId: String,
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AcceptanceViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return AcceptanceViewModel(cardId, repository) as T
    }
}
