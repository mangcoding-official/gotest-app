package com.gotest.app.feature.leaderboard.ui.packageleaderboard

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.LeaderboardEntry

/** Package facts shown in the app bar and the chips, available even with no participants. */
@Immutable
data class PackageLeaderboardHeader(
    val title: String,
    val examLabel: String,
    val questionCount: Int,
    val durationMinutes: Int,
)

sealed interface PackageLeaderboardUiState {
    data object Loading : PackageLeaderboardUiState

    data object Error : PackageLeaderboardUiState

    /** Nobody has finished the package yet. */
    data class Empty(
        val header: PackageLeaderboardHeader,
    ) : PackageLeaderboardUiState

    @Immutable
    data class Success(
        val header: PackageLeaderboardHeader,
        /** Ranks 1–3, shown on the podium. */
        val podium: List<LeaderboardEntry>,
        /** Rank 4 onwards, including the student's own row. */
        val rows: List<LeaderboardEntry>,
    ) : PackageLeaderboardUiState
}

sealed interface PackageLeaderboardUiEvent {
    /** Participant search has no design yet. */
    data object SearchClick : PackageLeaderboardUiEvent

    data object Retry : PackageLeaderboardUiEvent
}

sealed interface PackageLeaderboardEffect {
    data object ShowComingSoon : PackageLeaderboardEffect
}
