package com.gotest.app.feature.multiplayer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.core.config.AppFeatures
import com.gotest.app.feature.multiplayer.ui.MainBarengRoute
import com.gotest.app.feature.multiplayer.ui.MainBarengViewModel
import com.gotest.app.navigation.MultiplayerDestination
import com.gotest.app.navigation.UpgradePlanDestination

/**
 * Main Bareng: one screen per space, as the web page is (hub, waiting room, countdown, answering, waiting, result).
 * A game of another space opens that space's screen in place of this one, to go on with it (web `?aksi=lanjut`).
 */
fun NavGraphBuilder.multiplayerScreens(navController: NavController) {
    composable<MultiplayerDestination> { entry ->
        MainBarengRoute(
            onBack = navController::navigateUp,
            onOpenSpace = { space ->
                navController.navigate(
                    MultiplayerDestination(space = space, action = MainBarengViewModel.ACTION_RESUME),
                ) {
                    popUpTo(entry.destination.id) { inclusive = true }
                }
            },
            onUpgrade = { navController.navigate(UpgradePlanDestination) }.takeIf { AppFeatures.PAYMENTS },
        )
    }
}
