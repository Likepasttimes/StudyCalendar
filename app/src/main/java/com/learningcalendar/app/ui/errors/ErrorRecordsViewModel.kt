package com.learningcalendar.app.ui.errors

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.ErrorRecord
import com.learningcalendar.app.domain.ErrorScreenshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.ui.UiFeedback
import com.learningcalendar.app.ui.toChineseUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ErrorRecordCardUi(
    val record: ErrorRecord,
    val screenshots: List<ErrorScreenshot>,
)

data class ErrorRecordsUiState(
    val card: TaskContentCard?,
    val records: List<ErrorRecordCardUi>,
    val isLoading: Boolean,
)

class ErrorRecordsViewModel(
    private val cardId: String,
    private val repository: StudyTaskRepository,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 4)
    val feedback = _feedback.asSharedFlow()
    val saving: StateFlow<Boolean> = isSaving

    val uiState: StateFlow<ErrorRecordsUiState> = combine(
        repository.observeCard(cardId),
        repository.observeErrorRecords(cardId),
        repository.observeErrorScreenshotsForCard(cardId),
    ) { card, records, screenshots ->
        ErrorRecordsUiState(
            card = card,
            records = records.map { record ->
                ErrorRecordCardUi(
                    record = record,
                    screenshots = screenshots.filter { it.errorRecordId == record.id },
                )
            },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ErrorRecordsUiState(
            card = null,
            records = emptyList(),
            isLoading = true,
        ),
    )

    fun createRecord(
        errorPoint: String,
        description: String,
        photoUris: List<Uri>,
        fileUris: List<Uri>,
        onResult: (Boolean) -> Unit,
    ) = launchWrite("错误记录卡片已创建", onResult) {
        repository.createErrorRecordWithAttachments(
            cardId = cardId,
            errorPoint = errorPoint,
            description = description,
            photoUris = photoUris,
            fileUris = fileUris,
        )
    }

    fun updateRecord(
        recordId: String,
        errorPoint: String,
        description: String,
        onResult: (Boolean) -> Unit,
    ) = launchWrite("错误记录卡片已保存", onResult) {
        repository.updateErrorRecord(recordId, errorPoint, description)
    }

    fun deleteRecord(recordId: String, onResult: (Boolean) -> Unit) = launchWrite(
        "错误记录卡片已删除",
        onResult,
    ) {
        repository.deleteErrorRecord(recordId)
    }

    fun importAttachment(
        recordId: String,
        sourceUri: Uri,
        kind: SnapshotKind,
        onResult: (Boolean) -> Unit,
    ) = launchWrite(
        if (kind == SnapshotKind.PHOTO) "照片已保存到本地" else "文件已保存到本地",
        onResult,
    ) {
        repository.importErrorAttachment(recordId, sourceUri, kind)
    }

    fun deleteScreenshot(screenshotId: String, onResult: (Boolean) -> Unit) = launchWrite(
        "截图已删除",
        onResult,
    ) {
        repository.deleteErrorScreenshot(screenshotId)
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

class ErrorRecordsViewModelFactory(
    private val cardId: String,
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ErrorRecordsViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ErrorRecordsViewModel(cardId, repository) as T
    }
}
