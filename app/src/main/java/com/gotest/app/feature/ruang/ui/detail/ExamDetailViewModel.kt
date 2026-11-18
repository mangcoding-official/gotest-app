package com.gotest.app.feature.ruang.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.data.parseServerTime
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.RuangRepository
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Why "Mulai Ujian" did not go on: the server's refusal, no connection, or a declined pinning. */
sealed interface StartProblem {
    data class Server(
        val message: String,
    ) : StartProblem

    data object Offline : StartProblem
}

@Immutable
data class ExamDetailUiState(
    val isLoading: Boolean = true,
    val detail: ExamDetail? = null,
    val isStarting: Boolean = false,
    val problem: StartProblem? = null,
    /** When this phone's sign-in ends, shown when it is less than a day away (API-K4, A2b-7). */
    val signInEndsSoon: Instant? = null,
)

/** The start page of one Ujian schedule (web: Ruang/Exams/Show). */
@HiltViewModel
class ExamDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val exams: ExamRepository,
        private val ruang: RuangRepository,
        private val local: ExamLocalStore,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<UjianDetailDestination>()
        val code: String = route.code

        private val _uiState = MutableStateFlow(ExamDetailUiState())
        val uiState: StateFlow<ExamDetailUiState> = _uiState.asStateFlow()

        private val _openWork = Channel<Long>(Channel.BUFFERED)

        /** The attempt to open once started: the work screen. */
        val openWork: Flow<Long> = _openWork.receiveAsFlow()

        private val _startFailed = Channel<Unit>(Channel.CONFLATED)

        /** Mulai failed: the screen lets go of the pinned screen (T-026 A2b review 4). */
        val startFailed: Flow<Unit> = _startFailed.receiveAsFlow()

        fun load() {
            _uiState.update { it.copy(isLoading = it.detail == null) }
            viewModelScope.launch {
                val result = exams.detail(code, route.scheduleId)
                _uiState.update { it.copy(isLoading = false, detail = result.getOrNull() ?: it.detail) }
                val endsAt = ruang.signInEndsAt().getOrNull()?.let(::parseServerTime)
                val soon = endsAt?.takeIf { it.isBefore(Instant.now().plus(1, ChronoUnit.DAYS)) }
                _uiState.update { it.copy(signInEndsSoon = soon) }
            }
        }

        /** Called once the screen is pinned. A running attempt goes straight to its work screen. */
        fun start() {
            val state = _uiState.value
            val detail = state.detail
            // One Mulai at a time (T-026 A2b review 3).
            if (detail == null || state.isStarting) return
            val running = detail.attemptId
            if (running != null) {
                _openWork.trySend(running)
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
                        _startFailed.trySend(Unit)
                    }
            }
        }
    }
