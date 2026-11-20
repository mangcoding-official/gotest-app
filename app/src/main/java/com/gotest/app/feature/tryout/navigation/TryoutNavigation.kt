package com.gotest.app.feature.tryout.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gotest.app.feature.tryout.ui.catalog.TryoutCategoryRoute
import com.gotest.app.feature.tryout.ui.catalog.TryoutNavigation
import com.gotest.app.feature.tryout.ui.result.TryoutResultNavigation
import com.gotest.app.feature.tryout.ui.result.TryoutResultRoute
import com.gotest.app.feature.tryout.ui.runner.TryoutRunnerRoute
import com.gotest.app.feature.tryout.ui.session.TryoutSessionRoute
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.PackageLeaderboardDestination
import com.gotest.app.navigation.TryoutCategoryDestination
import com.gotest.app.navigation.TryoutResultDestination
import com.gotest.app.navigation.TryoutRunnerDestination
import com.gotest.app.navigation.TryoutSessionDestination

/** Where the Try Out catalog leads, from the Paket tab and a category. */
fun NavController.tryoutNavigation() =
    TryoutNavigation(
        onOpenCategory = { categoryId -> navigate(TryoutCategoryDestination(categoryId)) { launchSingleTop = true } },
        onOpenRunner = { attemptId, name -> navigate(TryoutRunnerDestination(attemptId, name)) },
        onOpenResult = { attemptId -> navigate(TryoutResultDestination(attemptId)) },
        onOpenHistory = { attemptId -> navigate(HistoryDetailDestination(attemptId.toString())) },
    )

/** Try Out (T-026 A2c): a category, a session, the runner, and the result. */
fun NavGraphBuilder.tryoutScreens(navController: NavController) {
    composable<TryoutCategoryDestination> {
        TryoutCategoryRoute(onBack = navController::navigateUp, navigation = navController.tryoutNavigation())
    }
    composable<TryoutSessionDestination> {
        TryoutSessionRoute(
            onBack = navController::navigateUp,
            onOpenRunner = { attemptId, name -> navController.navigate(TryoutRunnerDestination(attemptId, name)) },
            onOpenResult = { attemptId -> navController.navigate(TryoutResultDestination(attemptId)) },
        )
    }
    composable<TryoutRunnerDestination> { entry ->
        val route = entry.toRoute<TryoutRunnerDestination>()
        TryoutRunnerRoute(
            packageName = route.packageName,
            onOpenResult = {
                navController.navigate(TryoutResultDestination(route.attemptId)) {
                    popUpTo<TryoutRunnerDestination> { inclusive = true }
                }
            },
            onLeave = navController::navigateUp,
        )
    }
    composable<TryoutResultDestination> {
        TryoutResultRoute(
            navigation =
                TryoutResultNavigation(
                    onBack = navController::navigateUp,
                    onOpenPackageBoard = { packageId ->
                        navController.navigate(PackageLeaderboardDestination(packageId.toString()))
                    },
                    onOpenSession = { sessionId -> navController.navigate(TryoutSessionDestination(sessionId)) },
                ),
        )
    }
}
