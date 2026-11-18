package com.gotest.app.feature.ruang.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.navigation.UjianDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Why "Mulai Ujian" did not go on: the server's refusal, no connection, or a declined pinning. */
sealed interface StartProblem {
    data class Server(
        val message: String,
    ) : StartProblem

    data object Offline : StartProblem

    data object NotPinned : StartProblem
}

@Immutable
data class ExamDetailUiState(
    val isLoading: Boolean = true,
    val detail: ExamDetail? = null,
    val isStarting: Boolean = false,
    val problem: StartProblem? = null,
)

/** The start page of one Ujian schedule (web: Ruang/Exams/Show). */
@HiltViewModel
class ExamDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val exams: ExamRepository,
        private val local: ExamLocalStore,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<UjianDetailDestination>()
        val code: String = route.code

        private val _uiState = MutableStateFlow(ExamDetailUiState())
        val uiState: StateFlow<ExamDetailUiState> = _uiState.asStateFlow()

        private val _openWork = Channel<Long>(Channel.BUFFERED)

        /** The attempt to open once started: the work screen. */
        val openWork: Flow<Long> = _openWork.receiveAsFlow()

        fun load() {
            _uiState.update { it.copy(isLoading = it.detail == null) }
            viewModelScope.launch {
                val result = exams.detail(code, route.scheduleId)
                _uiState.update { it.copy(isLoading = false, detail = result.getOrNull() ?: it.detail) }
            }
        }

        fun onPinRefused() {
            _uiState.update { it.copy(problem = StartProblem.NotPinned) }
        }

        /** Called once the screen is pinned. A running attempt goes straight to its work screen. */
        fun start() {
            val detail = _uiState.value.detail ?: return
            detail.attemptId?.let { attempt ->
                _openWork.trySend(attempt)
                return
            }
            _uiState.update { it.copy(isStarting = true, problem = null) }
            viewModelScope.launch {
                exams
                    .start(code, detail.scheduleId)
                    .onSuccess { started ->
                        started.deviceToken?.let { local.saveDeviceToken(started.attemptId, it) }
                        _uiState.update { it.copy(isStarting = false) }
                        _openWork.send(started.attemptId)
                    }.onFailure { error ->
                        val problem =
                            if (error is ApiError) {
                                StartProblem.Server(
                                    error.message,
                                )
                            } else {
                                StartProblem.Offline
                            }
                        _uiState.update { it.copy(isStarting = false, problem = problem) }
                    }
            }
        }
    }
