package com.gotest.app.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.history.ui.detail.HistoryDetailRoute
import com.gotest.app.feature.history.ui.history.HistoryRoute
import com.gotest.app.navigation.HistoryDestination
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.PackageLeaderboardDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.navigateToTab

/** Riwayat tab and Detail Hasil. */
fun NavGraphBuilder.historyScreens(navController: NavController) {
    composable<HistoryDestination> {
        HistoryRoute(
            onOpenResult = { attemptId -> navController.navigate(HistoryDetailDestination(attemptId.toString())) },
        )
    }
    composable<HistoryDetailDestination> {
        HistoryDetailRoute(
            onBack = navController::navigateUp,
            onOpenPackageBoard = { packageId ->
                if (packageId != null) {
                    navController.navigate(PackageLeaderboardDestination(packageId.toString()))
                } else {
                    navController.navigateToTab(TopLevelDestination.Leaderboard)
                }
            },
        )
    }
}
