package com.gotest.app.feature.leaderboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.leaderboard.ui.leaderboard.LeaderboardRoute
import com.gotest.app.feature.leaderboard.ui.packageboard.PackageBoardRoute
import com.gotest.app.navigation.LeaderboardDestination
import com.gotest.app.navigation.PackageLeaderboardDestination

/** Leaderboard tab (Global, Per Paket, Latihan) and one package's board. */
fun NavGraphBuilder.leaderboardScreens(navController: NavController) {
    composable<LeaderboardDestination> {
        LeaderboardRoute(
            onOpenPackage = { packageId, practice ->
                navController.navigate(PackageLeaderboardDestination(packageId.toString(), practice))
            },
        )
    }
    composable<PackageLeaderboardDestination> {
        PackageBoardRoute(onBack = navController::navigateUp)
    }
}
