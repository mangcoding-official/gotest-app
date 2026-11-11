package com.gotest.app.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.history.ui.detail.HistoryDetailRoute
import com.gotest.app.feature.history.ui.history.HistoryRoute
import com.gotest.app.navigation.DiscussionSummaryDestination
import com.gotest.app.navigation.HistoryDestination
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.PackageLeaderboardDestination

/** Riwayat tab and Detail Hasil. */
fun NavGraphBuilder.historyScreens(navController: NavController) {
    composable<HistoryDestination> {
        HistoryRoute(
            onViewResult = { attemptId -> navController.navigate(HistoryDetailDestination(attemptId)) },
        )
    }
    composable<HistoryDetailDestination> {
        HistoryDetailRoute(
            onBack = navController::navigateUp,
            onOpenDiscussion = { attemptId -> navController.navigate(DiscussionSummaryDestination(attemptId)) },
            onOpenPackageLeaderboard = { packageId ->
                navController.navigate(PackageLeaderboardDestination(packageId))
            },
        )
    }
}
