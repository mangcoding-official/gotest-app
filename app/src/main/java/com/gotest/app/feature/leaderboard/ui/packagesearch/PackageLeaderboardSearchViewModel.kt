package com.gotest.app.feature.leaderboard.ui.packagesearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.model.PackageLeaderboard
import com.gotest.app.navigation.PackageLeaderboardSearchDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Filters the package's participants by name as the student types; the list is loaded once. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PackageLeaderboardSearchViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val leaderboardRepository: LeaderboardRepository,
    ) : ViewModel() {
        private val packageId = savedStateHandle.toRoute<PackageLeaderboardSearchDestination>().packageId
        private val refreshKey = MutableStateFlow(0)

        // Kept apart from uiState so the search field updates synchronously while typing.
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        val uiState: StateFlow<PackageLeaderboardSearchUiState> =
            refreshKey
                .flatMapLatest {
                    combine(leaderboardRepository.observePackageLeaderboard(packageId), _query, ::toUiState)
                        .onStart { emit(PackageLeaderboardSearchUiState.Loading) }
                        .catch { emit(PackageLeaderboardSearchUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    PackageLeaderboardSearchUiState.Loading,
                )

        fun onEvent(event: PackageLeaderboardSearchUiEvent) {
            when (event) {
                is PackageLeaderboardSearchUiEvent.QueryChange -> _query.value = event.query
                PackageLeaderboardSearchUiEvent.ClearQuery -> _query.value = ""
                PackageLeaderboardSearchUiEvent.Retry -> refreshKey.update { it + 1 }
            }
        }

        private fun toUiState(
            leaderboard: PackageLeaderboard,
            query: String,
        ): PackageLeaderboardSearchUiState {
            val needle = query.trim()
            return when {
                leaderboard.entries.isEmpty() -> {
                    PackageLeaderboardSearchUiState.Empty
                }

                needle.isEmpty() -> {
                    PackageLeaderboardSearchUiState.Idle
                }

                else -> {
                    PackageLeaderboardSearchUiState.Success(
                        packageTitle = leaderboard.title,
                        results =
                            leaderboard.entries
                                .filter { it.name.contains(needle, ignoreCase = true) }
                                .sortedBy { it.rank },
                    )
                }
            }
        }
    }
