package com.gotest.app.feature.exam.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.navigation.ExamResultDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class ExamResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        examRepository: ExamRepository,
    ) : ViewModel() {
        private val attemptId = savedStateHandle.toRoute<ExamResultDestination>().attemptId
        private val refresh = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ExamResultUiState> =
            refresh
                .flatMapLatest {
                    examRepository
                        .observeResult(attemptId)
                        .map<_, ExamResultUiState> { ExamResultUiState.Success(it) }
                        .onStart { emit(ExamResultUiState.Loading) }
                        .catch { emit(ExamResultUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExamResultUiState.Loading)

        fun onEvent(event: ExamResultUiEvent) {
            when (event) {
                ExamResultUiEvent.Retry -> refresh.update { it + 1 }
            }
        }
    }
