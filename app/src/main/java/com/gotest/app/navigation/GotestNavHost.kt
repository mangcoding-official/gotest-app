package com.gotest.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.gotest.app.feature.account.navigation.accountScreens
import com.gotest.app.feature.assistant.navigation.assistantScreens
import com.gotest.app.feature.auth.navigation.authScreens
import com.gotest.app.feature.exam.navigation.examScreens
import com.gotest.app.feature.history.navigation.historyScreens
import com.gotest.app.feature.home.navigation.homeScreens
import com.gotest.app.feature.leaderboard.navigation.leaderboardScreens
import com.gotest.app.feature.multiplayer.navigation.multiplayerScreens
import com.gotest.app.feature.studypackage.navigation.studyPackageScreens
import com.gotest.app.feature.upgrade.navigation.upgradeScreens

/** Every destination from the design audit, registered by its feature's `xxxScreens()` builder. */
@Composable
fun GotestNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = OnboardingDestination,
        modifier = modifier,
    ) {
        authScreens(navController)
        accountScreens(navController)
        homeScreens(navController)
        examScreens(navController)
        studyPackageScreens(navController)
        historyScreens(navController)
        leaderboardScreens(navController)
        upgradeScreens(navController)
        assistantScreens(navController)
        multiplayerScreens(navController)
    }
}
