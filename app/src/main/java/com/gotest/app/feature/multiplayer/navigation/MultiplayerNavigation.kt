package com.gotest.app.feature.multiplayer.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.multiplayer.ui.create.CreateGameRoute
import com.gotest.app.feature.multiplayer.ui.gamewaiting.GameWaitingRoute
import com.gotest.app.feature.multiplayer.ui.hub.GameScreen
import com.gotest.app.feature.multiplayer.ui.hub.MultiplayerRoute
import com.gotest.app.feature.multiplayer.ui.play.GamePlayRoute
import com.gotest.app.feature.multiplayer.ui.result.GameResultRoute
import com.gotest.app.feature.multiplayer.ui.waitingroom.WaitingRoomRoute
import com.gotest.app.navigation.CreateGameDestination
import com.gotest.app.navigation.GamePlayDestination
import com.gotest.app.navigation.GameResultDestination
import com.gotest.app.navigation.GameWaitingDestination
import com.gotest.app.navigation.MultiplayerDestination
import com.gotest.app.navigation.UpgradePlanDestination
import com.gotest.app.navigation.WaitingRoomDestination

/**
 * Main Bareng: hub, create, waiting room, play, waiting for the others and the result. Each step
 * of a running game replaces the previous one, so Back from the result returns to the hub.
 */
fun NavGraphBuilder.multiplayerScreens(navController: NavController) {
    composable<MultiplayerDestination> {
        MultiplayerRoute(
            onBack = navController::navigateUp,
            onCreateGame = { navController.navigate(CreateGameDestination) },
            onUpgrade = { navController.navigate(UpgradePlanDestination) },
            onOpenGame = { code, screen -> navController.navigate(screen.destination(code)) },
            onOpenResult = { navController.navigate(GameResultDestination(it)) },
        )
    }
    composable<CreateGameDestination> {
        CreateGameRoute(
            onBack = navController::navigateUp,
            onOpenWaitingRoom = { code ->
                navController.replaceWith<CreateGameDestination>(WaitingRoomDestination(code))
            },
        )
    }
    composable<WaitingRoomDestination> {
        WaitingRoomRoute(
            onBack = navController::navigateUp,
            onGameStart = { code -> navController.replaceWith<WaitingRoomDestination>(GamePlayDestination(code)) },
            onBackToHub = navController::navigateBackToMultiplayer,
            onOpenWaitingRoom = { code ->
                navController.replaceWith<WaitingRoomDestination>(WaitingRoomDestination(code))
            },
            onUpgrade = { navController.navigate(UpgradePlanDestination) },
        )
    }
    composable<GamePlayDestination> {
        GamePlayRoute(
            onFinish = { code -> navController.replaceWith<GamePlayDestination>(GameWaitingDestination(code)) },
        )
    }
    composable<GameWaitingDestination> {
        GameWaitingRoute(
            onBack = navController::navigateBackToMultiplayer,
            onResultReady = { code -> navController.replaceWith<GameWaitingDestination>(GameResultDestination(code)) },
        )
    }
    composable<GameResultDestination> {
        GameResultRoute(
            onBackToHub = navController::navigateBackToMultiplayer,
            onUpgrade = { navController.navigate(UpgradePlanDestination) },
            onReplay = { code -> navController.replaceWith<GameResultDestination>(WaitingRoomDestination(code)) },
        )
    }
}

private fun GameScreen.destination(code: String): Any =
    when (this) {
        GameScreen.WaitingRoom -> WaitingRoomDestination(code)
        GameScreen.Play -> GamePlayDestination(code)
        GameScreen.AwaitingOthers -> GameWaitingDestination(code)
    }

/** Opens [destination] in place of the current [T] screen (a new entry, so a new code gets a new ViewModel). */
private inline fun <reified T : Any> NavController.replaceWith(destination: Any) {
    navigate(destination) { popUpTo<T> { inclusive = true } }
}

/** "Kembali ke Main Bareng": pops back to the hub, or opens it when the game was entered elsewhere. */
fun NavController.navigateBackToMultiplayer() {
    if (!popBackStack<MultiplayerDestination>(inclusive = false)) {
        val current = currentDestination?.id
        navigate(MultiplayerDestination()) {
            if (current != null) popUpTo(current) { inclusive = true }
            launchSingleTop = true
        }
    }
}
