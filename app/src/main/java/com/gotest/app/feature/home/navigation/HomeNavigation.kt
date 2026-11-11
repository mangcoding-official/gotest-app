package com.gotest.app.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.home.ui.home.HomeRoute
import com.gotest.app.feature.home.ui.notification.NotificationRoute
import com.gotest.app.feature.home.ui.search.SearchRoute
import com.gotest.app.navigation.CreateGameDestination
import com.gotest.app.navigation.ExamDestination
import com.gotest.app.navigation.HelpDestination
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.HomeDestination
import com.gotest.app.navigation.MultiplayerDestination
import com.gotest.app.navigation.NotificationDestination
import com.gotest.app.navigation.PracticeDetailDestination
import com.gotest.app.navigation.PromoDestination
import com.gotest.app.navigation.SearchDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.TryoutDetailDestination
import com.gotest.app.navigation.UpgradePlanDestination
import com.gotest.app.navigation.navigateToTab

/** Beranda (Dashboard tab), Pencarian and Notifikasi. */
fun NavGraphBuilder.homeScreens(navController: NavController) {
    composable<HomeDestination> {
        HomeRoute(
            onSearch = { navController.navigate(SearchDestination) },
            onNotifications = { navController.navigate(NotificationDestination) },
            onContinue = { packageId -> navController.navigate(ExamDestination(packageId)) },
            onBrowsePackages = { navController.openTab(TopLevelDestination.StudyPackage) },
            onOpenPackage = navController::navigateToPackage,
            onSeeAllActivity = { navController.openTab(TopLevelDestination.History) },
            onOpenResult = { attemptId -> navController.navigate(HistoryDetailDestination(attemptId)) },
            onJoinGame = { navController.navigate(MultiplayerDestination(openJoinSheet = true)) },
            onCreateGame = { navController.navigate(CreateGameDestination()) },
            onOpenPromo = { promoId -> navController.navigate(PromoDestination(promoId)) },
        )
    }
    composable<SearchDestination> {
        SearchRoute(
            onBack = navController::navigateUp,
            onOpenPackage = navController::navigateToPackage,
            onOpenResult = { attemptId -> navController.navigate(HistoryDetailDestination(attemptId)) },
            // Articles open Pusat Bantuan; there is no per-article screen in the design.
            onOpenArticle = { navController.navigate(HelpDestination) },
        )
    }
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

/** Latihan and tryout packages have different detail screens. */
private fun NavController.navigateToPackage(
    packageId: String,
    type: PackageType,
) {
    when (type) {
        PackageType.Practice -> navigate(PracticeDetailDestination(packageId))
        PackageType.Tryout -> navigate(TryoutDetailDestination(packageId))
    }
}

private fun NavController.openTab(tab: TopLevelDestination) = navigateToTab(tab)
