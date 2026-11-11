package com.gotest.app.feature.leaderboard.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.model.GlobalLeaderboard
import com.gotest.app.core.model.PackageRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val PODIUM_SIZE = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LeaderboardViewModel
    @Inject
    constructor(
        private val leaderboardRepository: LeaderboardRepository,
    ) : ViewModel() {
        private val selectedTab = MutableStateFlow(LeaderboardTab.Global)
        private val globalRefreshKey = MutableStateFlow(0)
        private val packagesRefreshKey = MutableStateFlow(0)
        private val infoVisible = MutableStateFlow(false)

        private val globalState: Flow<GlobalLeaderboardUiState> =
            globalRefreshKey.flatMapLatest {
                leaderboardRepository
                    .observeGlobal()
                    .map(::toGlobalState)
                    .onStart { emit(GlobalLeaderboardUiState.Loading) }
                    .catch { emit(GlobalLeaderboardUiState.Error) }
            }

        private val packagesState: Flow<PackageRankingsUiState> =
            packagesRefreshKey.flatMapLatest {
                leaderboardRepository
                    .observePackageRankings()
                    .map(::toPackagesState)
                    .onStart { emit(PackageRankingsUiState.Loading) }
                    .catch { emit(PackageRankingsUiState.Error) }
            }

        val uiState: StateFlow<LeaderboardUiState> =
            combine(selectedTab, globalState, packagesState, infoVisible, ::LeaderboardUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LeaderboardUiState())

        fun onEvent(event: LeaderboardUiEvent) {
            when (event) {
                is LeaderboardUiEvent.TabSelect -> {
                    selectedTab.value = event.tab
                }

                LeaderboardUiEvent.InfoClick -> {
                    infoVisible.value = true
                }

                LeaderboardUiEvent.InfoDismiss -> {
                    infoVisible.value = false
                }

                LeaderboardUiEvent.RetryGlobal -> {
                    globalRefreshKey.update { it + 1 }
                }

                LeaderboardUiEvent.RetryPackages -> {
                    packagesRefreshKey.update { it + 1 }
                }
            }
        }

        private fun toGlobalState(leaderboard: GlobalLeaderboard): GlobalLeaderboardUiState {
            if (leaderboard.entries.isEmpty()) return GlobalLeaderboardUiState.Empty
            val (podium, rows) = leaderboard.entries.sortedBy { it.rank }.partition { it.rank <= PODIUM_SIZE }
            return GlobalLeaderboardUiState.Success(
                myRank = leaderboard.myRank,
                myRankChange = leaderboard.myRankChange,
                totalUsers = leaderboard.totalUsers,
                podium = podium,
                rows = rows,
            )
        }

        private fun toPackagesState(rankings: List<PackageRanking>): PackageRankingsUiState =
            if (rankings.isEmpty()) {
                PackageRankingsUiState.Empty
            } else {
                // TODO(api): use the server's "last updated" time once the API returns it.
                PackageRankingsUiState.Success(rankings = rankings, updatedAt = LocalDateTime.now())
            }
    }
