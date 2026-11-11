package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.CreateBlock
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.PlayerState

/** Main Bareng page content. An empty history is shown inside its card, so there is no Empty state. */
sealed interface MultiplayerUiState {
    data object Loading : MultiplayerUiState

    data object Error : MultiplayerUiState

    @Immutable
    data class Success(
        val history: List<GameHistoryItem>,
        val isPremium: Boolean,
        /** The game the student is already in; blocks creating or joining another. */
        val activeGame: Game?,
    ) : MultiplayerUiState
}

/** Dialogs and the join sheet, kept apart from the page so they survive a reload. */
@Immutable
data class MultiplayerOverlayState(
    val dialog: CreateBlock? = null,
    val joinSheet: JoinSheetState? = null,
)

@Immutable
data class JoinSheetState(
    val code: String = "",
    val status: JoinStatus = JoinStatus.Idle,
) {
    val isComplete: Boolean get() = code.length == GameRules.CODE_LENGTH
}

/** Where the join sheet is in "Gabung dengan kode". */
sealed interface JoinStatus {
    data object Idle : JoinStatus

    data object Searching : JoinStatus

    data class Found(
        val game: Game,
        val isJoining: Boolean = false,
    ) : JoinStatus

    data object NotFound : JoinStatus

    data object AlreadyStarted : JoinStatus

    data object Full : JoinStatus

    data object Inactive : JoinStatus

    data class InAnotherGame(
        val game: Game,
    ) : JoinStatus
}

/** Which screen of a running game to reopen. */
enum class GameScreen { WaitingRoom, Play, AwaitingOthers }

fun Game.resumeScreen(): GameScreen =
    when {
        status == GameStatus.Waiting -> GameScreen.WaitingRoom
        currentUser?.state == PlayerState.Finished -> GameScreen.AwaitingOthers
        else -> GameScreen.Play
    }

sealed interface MultiplayerUiEvent {
    data object Retry : MultiplayerUiEvent

    data object CreateGameClick : MultiplayerUiEvent

    data object DismissDialog : MultiplayerUiEvent

    data object UpgradeClick : MultiplayerUiEvent

    data object OpenJoinSheet : MultiplayerUiEvent

    data object DismissJoinSheet : MultiplayerUiEvent

    data class CodeChange(
        val code: String,
    ) : MultiplayerUiEvent

    data object SearchGame : MultiplayerUiEvent

    data object JoinGame : MultiplayerUiEvent

    data class ReturnToGame(
        val game: Game,
    ) : MultiplayerUiEvent
}

sealed interface MultiplayerEffect {
    data object OpenCreateGame : MultiplayerEffect

    data object OpenUpgrade : MultiplayerEffect

    data class OpenGame(
        val code: String,
        val screen: GameScreen,
    ) : MultiplayerEffect
}
