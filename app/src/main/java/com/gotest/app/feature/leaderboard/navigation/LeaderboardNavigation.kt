package com.gotest.app.feature.leaderboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gotest.app.feature.leaderboard.ui.leaderboard.LeaderboardRoute
import com.gotest.app.feature.leaderboard.ui.packageleaderboard.PackageLeaderboardRoute
import com.gotest.app.feature.leaderboard.ui.packagesearch.PackageLeaderboardSearchRoute
import com.gotest.app.navigation.LeaderboardDestination
import com.gotest.app.navigation.PackageLeaderboardDestination
import com.gotest.app.navigation.PackageLeaderboardSearchDestination

/** Peringkat tab (Global / Per Paket), Leaderboard Paket and its participant search. */
fun NavGraphBuilder.leaderboardScreens(navController: NavController) {
    composable<LeaderboardDestination> {
        LeaderboardRoute(
            onOpenPackage = { packageId -> navController.navigate(PackageLeaderboardDestination(packageId)) },
        )
    }
    composable<PackageLeaderboardDestination> { entry ->
        val packageId = entry.toRoute<PackageLeaderboardDestination>().packageId
        PackageLeaderboardRoute(
            onBack = navController::navigateUp,
            onSearch = {
                navController.navigate(PackageLeaderboardSearchDestination(packageId)) { launchSingleTop = true }
            },
        )
    }
    composable<PackageLeaderboardSearchDestination> {
        PackageLeaderboardSearchRoute(onBack = navController::navigateUp)
    }
}
