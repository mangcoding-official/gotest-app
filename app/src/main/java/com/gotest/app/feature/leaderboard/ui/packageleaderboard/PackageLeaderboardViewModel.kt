package com.gotest.app.feature.leaderboard.ui.packageleaderboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.model.PackageLeaderboard
import com.gotest.app.navigation.PackageLeaderboardDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val PODIUM_SIZE = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PackageLeaderboardViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val leaderboardRepository: LeaderboardRepository,
    ) : ViewModel() {
        private val packageId = savedStateHandle.toRoute<PackageLeaderboardDestination>().packageId
        private val refreshKey = MutableStateFlow(0)

        private val _effects = Channel<PackageLeaderboardEffect>(Channel.BUFFERED)
        val effects: Flow<PackageLeaderboardEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<PackageLeaderboardUiState> =
            refreshKey
                .flatMapLatest {
                    leaderboardRepository
                        .observePackageLeaderboard(packageId)
                        .map(::toUiState)
                        .onStart { emit(PackageLeaderboardUiState.Loading) }
                        .catch { emit(PackageLeaderboardUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    PackageLeaderboardUiState.Loading,
                )

        fun onEvent(event: PackageLeaderboardUiEvent) {
            when (event) {
                PackageLeaderboardUiEvent.SearchClick -> {
                    viewModelScope.launch { _effects.send(PackageLeaderboardEffect.ShowComingSoon) }
                }

                PackageLeaderboardUiEvent.Retry -> {
                    refreshKey.update { it + 1 }
                }
            }
        }

        private fun toUiState(leaderboard: PackageLeaderboard): PackageLeaderboardUiState {
            val header =
                PackageLeaderboardHeader(
                    title = leaderboard.title,
                    examLabel = leaderboard.examLabel,
                    questionCount = leaderboard.questionCount,
                    durationMinutes = leaderboard.durationMinutes,
                )
            if (leaderboard.entries.isEmpty()) return PackageLeaderboardUiState.Empty(header)
            val (podium, rows) = leaderboard.entries.sortedBy { it.rank }.partition { it.rank <= PODIUM_SIZE }
            return PackageLeaderboardUiState.Success(header = header, podium = podium, rows = rows)
        }
    }
