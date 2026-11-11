package com.gotest.app.feature.assistant.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.gotest.app.feature.assistant.ui.assistant.AssistantRoute
import com.gotest.app.navigation.AssistantDestination
import com.gotest.app.navigation.PracticeDetailDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.TryoutDetailDestination
import com.gotest.app.navigation.navigateToTab

/** "Asisten Gotest", opened from the AI button of the bottom bar. */
fun NavGraphBuilder.assistantScreens(navController: NavController) {
    composable<AssistantDestination> {
        AssistantRoute(
            onBack = navController::navigateUp,
            onOpenPractice = { packageId -> navController.navigate(PracticeDetailDestination(packageId)) },
            onOpenTryout = { packageId -> navController.navigate(TryoutDetailDestination(packageId)) },
            onBrowseTryouts = { navController.leaveAssistantForTab(TopLevelDestination.StudyPackage) },
            onOpenLeaderboard = { navController.leaveAssistantForTab(TopLevelDestination.Leaderboard) },
        )
    }
}

/**
 * Closes the assistant before switching tabs, so the tab switch doesn't save it into the previous
 * tab's state.
 */
private fun NavController.leaveAssistantForTab(tab: TopLevelDestination) {
    popBackStack<AssistantDestination>(inclusive = true)
    navigateToTab(tab)
}
