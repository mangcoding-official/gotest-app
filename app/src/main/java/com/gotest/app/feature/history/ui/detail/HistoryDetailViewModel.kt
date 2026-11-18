package com.gotest.app.feature.history.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.navigation.HistoryDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val detail: HistoryDetail? = null,
)

/** Detail Hasil (web: HistoryResultModal for Try Out, PracticeResultModal for Latihan). */
@HiltViewModel
class HistoryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: HistoryPagesRepository,
    ) : ViewModel() {
        // The destination keeps a String id: Beranda, Cari and Notifikasi open it too.
        private val attemptId = savedStateHandle.toRoute<HistoryDetailDestination>().attemptId.toLongOrNull()

        private val _uiState = MutableStateFlow(HistoryDetailUiState())
        val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            val id = attemptId
            if (id == null) {
                _uiState.update { HistoryDetailUiState(isLoading = false, loadFailed = true) }
                return
            }
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                repository
                    .detail(id)
                    .onSuccess { detail ->
                        _uiState.update { HistoryDetailUiState(isLoading = false, detail = detail) }
                    }.onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }
    }
