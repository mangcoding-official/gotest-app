package com.gotest.app.feature.history.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageLeaderboard
import com.gotest.app.navigation.HistoryDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** The design's ranking card lists the top five participants. */
private const val RANKING_PREVIEW_SIZE = 5

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val examRepository: ExamRepository,
        private val leaderboardRepository: LeaderboardRepository,
    ) : ViewModel() {
        private val attemptId = savedStateHandle.toRoute<HistoryDetailDestination>().attemptId
        private val refreshKey = MutableStateFlow(0)

        val uiState: StateFlow<HistoryDetailUiState> =
            refreshKey
                .flatMapLatest {
                    // History tells whether the attempt exists and which package it belongs to,
                    // so the result and the package leaderboard can load in parallel.
                    examRepository
                        .observeHistory()
                        .map { history -> history.firstOrNull { it.attemptId == attemptId }?.packageId }
                        .distinctUntilChanged()
                        .flatMapLatest { packageId ->
                            if (packageId == null) {
                                flowOf(HistoryDetailUiState.Empty)
                            } else {
                                combine(
                                    examRepository.observeResult(attemptId),
                                    leaderboardRepository.observePackageLeaderboard(packageId),
                                    ::toSuccess,
                                )
                            }
                        }.onStart { emit(HistoryDetailUiState.Loading) }
                        .catch { emit(HistoryDetailUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryDetailUiState.Loading)

        fun onEvent(event: HistoryDetailUiEvent) {
            when (event) {
                HistoryDetailUiEvent.Retry -> refreshKey.update { it + 1 }
            }
        }

        private fun toSuccess(
            result: AttemptResult,
            leaderboard: PackageLeaderboard,
        ): HistoryDetailUiState {
            val entries = leaderboard.entries.sortedBy { it.rank }
            val top = entries.take(RANKING_PREVIEW_SIZE)
            // Keep the student's own row visible even when they are outside the top five.
            val me = entries.firstOrNull { it.isCurrentUser }
            val shown = if (me != null && me !in top) top + me else top
            return HistoryDetailUiState.Success(
                result = result,
                ranking =
                    shown.map { entry ->
                        HistoryRankingRow(
                            rank = entry.rank,
                            name = entry.name,
                            completedOn = if (entry.isCurrentUser) result.completedOn else null,
                            durationSeconds = entry.finishSeconds,
                            score = entry.points,
                            isCurrentUser = entry.isCurrentUser,
                        )
                    },
            )
        }
    }
