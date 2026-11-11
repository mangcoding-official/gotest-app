package com.gotest.app.feature.multiplayer.ui.detail

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.CancelReason
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.ui.hub.GameScreen
import java.time.LocalDateTime

/** Why the game has no result; picks the status card's explanation (frames 15606, 15891 and 23999). */
enum class CancelExplanation {
    /** The student hosted the game and pressed "Batalkan Permainan". */
    CancelledByYou,

    /** Another host cancelled it while the student waited. */
    CancelledByHost,

    /** The waiting room timed out with fewer than two players. */
    TooFewPlayers,

    /** The waiting room timed out with enough players, but nobody started. */
    NotStarted,
}

/** "Detail Permainan" of a game without a result. One game, so there is no Empty state. */
sealed interface GameDetailUiState {
    data object Loading : GameDetailUiState

    data object Error : GameDetailUiState

    @Immutable
    data class Success(
        val game: Game,
        val createdAt: LocalDateTime,
        val cancelledAt: LocalDateTime?,
        val isPremium: Boolean,
        /** The game the student is already in; blocks creating another. */
        val activeGame: Game?,
    ) : GameDetailUiState {
        val explanation: CancelExplanation
            get() =
                when {
                    game.cancelReason == CancelReason.ByHost && game.isHostedByCurrentUser -> {
                        CancelExplanation.CancelledByYou
                    }

                    game.cancelReason == CancelReason.ByHost -> {
                        CancelExplanation.CancelledByHost
                    }

                    game.players.size < game.type.minPlayers -> {
                        CancelExplanation.TooFewPlayers
                    }

                    else -> {
                        CancelExplanation.NotStarted
                    }
                }

        /** Marks "Dibatalkan" with "(otomatis)". */
        val isAutoCancelled: Boolean get() = game.cancelReason == CancelReason.AutoCancel

        /** Only the host spent game stock, so only they get "Stok permainan sudah dikembalikan". */
        val stockReturned: Boolean get() = game.isHostedByCurrentUser
    }
}

sealed interface GameDetailUiEvent {
    data object Retry : GameDetailUiEvent

    /** "Buat Ulang dengan Pengaturan Sama". */
    data object RecreateClick : GameDetailUiEvent

    data object DismissDialog : GameDetailUiEvent

    data object UpgradeClick : GameDetailUiEvent

    data object JoinWithCodeClick : GameDetailUiEvent

    data class ReturnToGame(
        val game: Game,
    ) : GameDetailUiEvent
}

sealed interface GameDetailEffect {
    /** "Buat permainan" prefilled with the settings of [copyFromGameCode]. */
    data class OpenCreateGame(
        val copyFromGameCode: String,
    ) : GameDetailEffect

    data object OpenUpgrade : GameDetailEffect

    /** "Gabung dengan Kode" from the Premium dialog: the hub with its join sheet open. */
    data object OpenJoinSheet : GameDetailEffect

    data class OpenGame(
        val code: String,
        val screen: GameScreen,
    ) : GameDetailEffect
}
