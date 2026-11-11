package com.gotest.app.feature.leaderboard.ui.packagesearch

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.LeaderboardEntry

sealed interface PackageLeaderboardSearchUiState {
    data object Loading : PackageLeaderboardSearchUiState

    data object Error : PackageLeaderboardSearchUiState

    /** Nobody has finished the package yet, so there is no one to search. */
    data object Empty : PackageLeaderboardSearchUiState

    /** Nothing typed yet. */
    data object Idle : PackageLeaderboardSearchUiState

    /** Participants whose name contains the query, by rank; [results] is empty when nobody matches. */
    @Immutable
    data class Success(
        val packageTitle: String,
        val results: List<LeaderboardEntry>,
    ) : PackageLeaderboardSearchUiState
}

sealed interface PackageLeaderboardSearchUiEvent {
    data class QueryChange(
        val query: String,
    ) : PackageLeaderboardSearchUiEvent

    data object ClearQuery : PackageLeaderboardSearchUiEvent

    data object Retry : PackageLeaderboardSearchUiEvent
}
