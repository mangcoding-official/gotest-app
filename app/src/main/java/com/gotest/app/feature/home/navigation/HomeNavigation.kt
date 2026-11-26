package com.gotest.app.feature.home.navigation

import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.gotest.app.BuildConfig
import com.gotest.app.core.config.AppFeatures
import com.gotest.app.feature.home.ui.home.HomeActions
import com.gotest.app.feature.home.ui.home.HomeRoute
import com.gotest.app.feature.home.ui.notification.NotificationRoute
import com.gotest.app.feature.home.ui.search.SearchActions
import com.gotest.app.feature.home.ui.search.SearchRoute
import com.gotest.app.navigation.CreateGameDestination
import com.gotest.app.navigation.HelpTopicDestination
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.HomeDestination
import com.gotest.app.navigation.MultiplayerDestination
import com.gotest.app.navigation.NotificationDestination
import com.gotest.app.navigation.PracticeDetailDestination
import com.gotest.app.navigation.PracticeResultDestination
import com.gotest.app.navigation.PracticeRunnerDestination
import com.gotest.app.navigation.PromoDestination
import com.gotest.app.navigation.RuangDestination
import com.gotest.app.navigation.SearchDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.TryoutResultDestination
import com.gotest.app.navigation.TryoutRunnerDestination
import com.gotest.app.navigation.UpgradePlanDestination
import com.gotest.app.navigation.navigateToTab

/** Beranda (Dashboard tab), Pencarian and Notifikasi. */
fun NavGraphBuilder.homeScreens(navController: NavController) {
    composable<HomeDestination> {
        val uriHandler = LocalUriHandler.current
        HomeRoute(
            actions =
                HomeActions(
                    onSearch = { navController.navigate(SearchDestination) },
                    onNotifications =
                        { navController.navigate(NotificationDestination) }.takeIf { AppFeatures.NOTIFICATION_INBOX },
                    onBrowsePackages = { navController.openTab(TopLevelDestination.StudyPackage) },
                    onSeeAllActivity = { navController.openTab(TopLevelDestination.History) },
                    onOpenPracticeRunner = { id, name -> navController.navigate(PracticeRunnerDestination(id, name)) },
                    onOpenPracticeResult = { id, name -> navController.navigate(PracticeResultDestination(id, name)) },
                    onOpenTryoutRunner = { id, name -> navController.navigate(TryoutRunnerDestination(id, name)) },
                    onOpenTryoutResult = { id -> navController.navigate(TryoutResultDestination(id)) },
                    onOpenRuang = { code -> navController.navigate(RuangDestination(code)) },
                    // The portal is on gotest.id only (A2b-2).
                    onOpenPortal = { code -> uriHandler.openUri(webAddress("/mitra/$code")) },
                    onOpenLink = { href -> uriHandler.openUri(webAddress(href)) },
                    onJoinGame = { navController.navigate(MultiplayerDestination(openJoinSheet = true)) },
                    onCreateGame = { navController.navigate(CreateGameDestination()) },
                    onOpenPromo =
                        { promoId: String -> navController.navigate(PromoDestination(promoId)) }
                            .takeIf { AppFeatures.PAYMENTS },
                ),
        )
    }
    composable<SearchDestination> {
        SearchRoute(
            actions =
                SearchActions(
                    onBack = navController::navigateUp,
                    onOpenPracticeRunner = { id, name -> navController.navigate(PracticeRunnerDestination(id, name)) },
                    onOpenPracticeResult = { id, name -> navController.navigate(PracticeResultDestination(id, name)) },
                    onOpenTryoutRunner = { id, name -> navController.navigate(TryoutRunnerDestination(id, name)) },
                    onOpenTryoutResult = { id -> navController.navigate(TryoutResultDestination(id)) },
                    onOpenHelpTopic = { slug -> navController.navigate(HelpTopicDestination(slug)) },
                ),
        )
    }
    if (AppFeatures.NOTIFICATION_INBOX) notificationScreen(navController)
}

private fun NavGraphBuilder.notificationScreen(navController: NavController) {
    composable<NotificationDestination> {
        NotificationRoute(
            onBack = navController::navigateUp,
            onOpenResult = { attemptId -> navController.navigate(HistoryDetailDestination(attemptId)) },
            onOpenPracticePackage = { packageId -> navController.navigate(PracticeDetailDestination(packageId)) },
            onOpenLeaderboard = { navController.openTab(TopLevelDestination.Leaderboard) },
            onUpgrade = { navController.navigate(UpgradePlanDestination) },
        )
    }
}

/** A gotest.id path joined with the server's address; an https address stays as it is. */
internal fun webAddress(href: String): String =
    if (href.startsWith("/")) BuildConfig.API_BASE_URL.trimEnd('/') + href else href

private fun NavController.openTab(tab: TopLevelDestination) = navigateToTab(tab)
