package com.learningcalendar.app.ui.weekly

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.WeekRange
import com.learningcalendar.app.domain.WeekSummaryDocument
import com.learningcalendar.app.ui.UiFeedback
import com.learningcalendar.app.ui.toChineseUserMessage
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekSummaryDetailUiState(
    val weekRange: WeekRange,
    val tasks: List<StudyTask>,
    val monthlyDocuments: List<WeekSummaryDocument>,
    val isLoading: Boolean,
)

data class WeeklySummaryOverviewUiState(
    val weekRange: WeekRange,
    val displayedMonth: YearMonth,
    val monthlyDocuments: List<WeekSummaryDocument>,
    val isLoading: Boolean,
)

class WeeklySummaryOverviewViewModel(
    repository: StudyTaskRepository,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val weekStart = MutableStateFlow(WeekRange.containing(today).startDate)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WeeklySummaryOverviewUiState> = weekStart
        .flatMapLatest { selectedWeekStart ->
            val displayedMonth = YearMonth.from(selectedWeekStart)
            val zone = ZoneId.systemDefault()
            val startMillis = displayedMonth.atDay(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
            val endMillis = displayedMonth.plusMonths(1)
                .atDay(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli() - 1

            repository.observeWeekSummaryDocumentsUploadedBetween(startMillis, endMillis)
                .map { documents ->
                    WeeklySummaryOverviewUiState(
                        weekRange = WeekRange.containing(selectedWeekStart),
                        displayedMonth = displayedMonth,
                        monthlyDocuments = documents,
                        isLoading = false,
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklySummaryOverviewUiState(
                weekRange = WeekRange.containing(today),
                displayedMonth = YearMonth.from(today),
                monthlyDocuments = emptyList(),
                isLoading = true,
            ),
        )

    fun previousWeek() {
        weekStart.value = weekStart.value.minusDays(7)
    }

    fun nextWeek() {
        weekStart.value = weekStart.value.plusDays(7)
    }
}

class WeeklySummaryOverviewViewModelFactory(
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeeklySummaryOverviewViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return WeeklySummaryOverviewViewModel(repository) as T
    }
}

class WeekSummaryViewModel(
    weekStartDate: LocalDate,
    private val repository: StudyTaskRepository,
) : ViewModel() {
    private val weekRange = WeekRange.containing(weekStartDate)
    private val isSaving = MutableStateFlow(false)
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 4)
    val feedback = _feedback.asSharedFlow()
    val saving: StateFlow<Boolean> = isSaving

    val uiState: StateFlow<WeekSummaryDetailUiState> = combine(
        repository.observeTasksBetween(weekRange.startDate, weekRange.endDate),
        repository.observeWeekSummaryDocumentsForMonth(YearMonth.from(weekRange.startDate)),
    ) { tasks, monthlyDocuments ->
        WeekSummaryDetailUiState(
            weekRange = weekRange,
            tasks = tasks,
            monthlyDocuments = monthlyDocuments,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeekSummaryDetailUiState(
            weekRange = weekRange,
            tasks = emptyList(),
            monthlyDocuments = emptyList(),
            isLoading = true,
        ),
    )

    fun importDocument(
        sourceUri: Uri,
        onResult: (Boolean) -> Unit,
    ) = launchWrite("周总结文档已上传", onResult) {
        repository.importWeekSummaryDocument(weekRange.startDate, sourceUri)
    }

    fun deleteDocument(documentId: String, onResult: (Boolean) -> Unit) = launchWrite(
        "周总结文档已删除",
        onResult,
    ) {
        repository.deleteWeekSummaryDocument(documentId)
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

class WeekSummaryViewModelFactory(
    private val weekStartDate: LocalDate,
    private val repository: StudyTaskRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeekSummaryViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return WeekSummaryViewModel(weekStartDate, repository) as T
    }
}
