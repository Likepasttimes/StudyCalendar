package com.learningcalendar.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.learningcalendar.app.data.sync.ComputerSyncRepository
import com.learningcalendar.app.ui.toChineseUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComputerSyncUiState(
    val host: String = "",
    val port: String = "8765",
    val pairingCode: String = "",
    val isSyncing: Boolean = false,
    val resultMessage: String? = null,
    val errorMessage: String? = null,
)

class ComputerSyncViewModel(
    private val repository: ComputerSyncRepository,
) : ViewModel() {
    private val savedConnection = repository.savedConnection()
    private val _uiState = MutableStateFlow(
        ComputerSyncUiState(
            host = savedConnection.host,
            port = savedConnection.port.ifBlank { "8765" },
        ),
    )
    val uiState: StateFlow<ComputerSyncUiState> = _uiState.asStateFlow()

    fun setHost(host: String) {
        _uiState.update { it.copy(host = host, errorMessage = null) }
    }

    fun setPort(port: String) {
        _uiState.update {
            it.copy(port = port.filter(Char::isDigit).take(5), errorMessage = null)
        }
    }

    fun setPairingCode(pairingCode: String) {
        _uiState.update {
            it.copy(
                pairingCode = pairingCode.filter(Char::isDigit).take(6),
                errorMessage = null,
            )
        }
    }

    fun sync() {
        val state = _uiState.value
        if (state.isSyncing) return
        if (state.host.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请填写电脑地址") }
            return
        }
        if (state.pairingCode.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入电脑界面显示的 6 位配对码") }
            return
        }

        _uiState.update {
            it.copy(
                isSyncing = true,
                resultMessage = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.syncToComputer(
                    host = state.host,
                    portText = state.port,
                    pairingCode = state.pairingCode,
                )
            }.onSuccess { result ->
                val sizeText = when {
                    result.uploadedBytes >= 1024 * 1024 ->
                        "%.1f MB".format(result.uploadedBytes / 1024.0 / 1024.0)
                    result.uploadedBytes >= 1024 ->
                        "%.1f KB".format(result.uploadedBytes / 1024.0)
                    else -> "${result.uploadedBytes} B"
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        resultMessage = "同步完成：${result.taskCount} 个任务卡、" +
                            "${result.contentCardCount} 张内容卡、" +
                            "${result.attachmentCount} 个附件（$sizeText）",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = error.toChineseUserMessage(),
                    )
                }
            }
        }
    }
}

class ComputerSyncViewModelFactory(
    private val repository: ComputerSyncRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ComputerSyncViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ComputerSyncViewModel(repository) as T
    }
}
