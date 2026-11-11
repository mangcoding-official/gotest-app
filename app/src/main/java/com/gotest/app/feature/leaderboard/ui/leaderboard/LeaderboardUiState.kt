package com.gotest.app.feature.leaderboard.ui.leaderboard

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.model.PackageRanking
import java.time.LocalDateTime

/** Segments of the "Mobile/Segmented Control" at the top of the tab. */
enum class LeaderboardTab(
    @param:StringRes val labelRes: Int,
) {
    Global(R.string.leaderboard_tab_global),
    PerPackage(R.string.leaderboard_tab_package),
}

/** Global segment (frame 3616). */
sealed interface GlobalLeaderboardUiState {
    data object Loading : GlobalLeaderboardUiState

    data object Error : GlobalLeaderboardUiState

    data object Empty : GlobalLeaderboardUiState

    @Immutable
    data class Success(
        val myRank: Int,
        val myRankChange: Int,
        val totalUsers: Int,
        /** Ranks 1–3, shown on the podium. */
        val podium: List<LeaderboardEntry>,
        /** Rank 4 onwards, including the student's own row. */
        val rows: List<LeaderboardEntry>,
    ) : GlobalLeaderboardUiState
}

/** Per Paket segment (frame 9392). */
sealed interface PackageRankingsUiState {
    data object Loading : PackageRankingsUiState

    data object Error : PackageRankingsUiState

    /** The student has not finished any tryout with a leaderboard yet. */
    data object Empty : PackageRankingsUiState

    @Immutable
    data class Success(
        val rankings: List<PackageRanking>,
        val updatedAt: LocalDateTime,
    ) : PackageRankingsUiState
}

/**
 * Each segment loads and retries on its own; [selectedTab] picks the one on screen.
 * [isInfoVisible] shows the "Tentang Leaderboard" sheet.
 */
@Immutable
data class LeaderboardUiState(
    val selectedTab: LeaderboardTab = LeaderboardTab.Global,
    val global: GlobalLeaderboardUiState = GlobalLeaderboardUiState.Loading,
    val packages: PackageRankingsUiState = PackageRankingsUiState.Loading,
    val isInfoVisible: Boolean = false,
)

sealed interface LeaderboardUiEvent {
    data class TabSelect(
        val tab: LeaderboardTab,
    ) : LeaderboardUiEvent

    data object InfoClick : LeaderboardUiEvent

    /** "Mengerti", or the sheet swiped away. */
    data object InfoDismiss : LeaderboardUiEvent

    data object RetryGlobal : LeaderboardUiEvent

    data object RetryPackages : LeaderboardUiEvent
}
