package com.gotest.app.feature.leaderboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.leaderboard.ui.leaderboard.LeaderboardRoute
import com.gotest.app.feature.leaderboard.ui.packageleaderboard.PackageLeaderboardRoute
import com.gotest.app.navigation.LeaderboardDestination
import com.gotest.app.navigation.PackageLeaderboardDestination

/** Peringkat tab (Global / Per Paket) and Leaderboard Paket. */
fun NavGraphBuilder.leaderboardScreens(navController: NavController) {
    composable<LeaderboardDestination> {
        LeaderboardRoute(
            onOpenPackage = { packageId -> navController.navigate(PackageLeaderboardDestination(packageId)) },
        )
    }
    composable<PackageLeaderboardDestination> {
        PackageLeaderboardRoute(onBack = navController::navigateUp)
    }
}
