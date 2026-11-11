package com.gotest.app.feature.multiplayer.ui.waitingroom

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.Game

sealed interface WaitingRoomUiState {
    data object Loading : WaitingRoomUiState

    data object Error : WaitingRoomUiState

    @Immutable
    data class Success(
        val game: Game,
        /** "Batal otomatis dalam". */
        val autoCancelSeconds: Int,
        /** Wall clock used for "Bergabung 2 menit lalu". */
        val nowMillis: Long,
        val isBusy: Boolean = false,
        val dialog: WaitingRoomDialog? = null,
    ) : WaitingRoomUiState {
        val isHost: Boolean get() = game.isHostedByCurrentUser
    }
}

enum class WaitingRoomDialog {
    /** Frame 15572, shown to players. */
    CancelledByHost,

    /** Frame 15857. */
    AutoCancelled,
}

sealed interface WaitingRoomUiEvent {
    data object Retry : WaitingRoomUiEvent

    data object Start : WaitingRoomUiEvent

    /** Host: "Batalkan Permainan". Player: "Keluar dari Ruang Tunggu". */
    data object Leave : WaitingRoomUiEvent

    /** "Buat Ulang" after the auto-cancel. */
    data object Recreate : WaitingRoomUiEvent
}

sealed interface WaitingRoomEffect {
    data class GameStarted(
        val code: String,
    ) : WaitingRoomEffect

    data object Left : WaitingRoomEffect

    data class Recreated(
        val code: String,
    ) : WaitingRoomEffect

    /** "Buat Ulang" needs Premium. */
    data object UpgradeRequired : WaitingRoomEffect

    data object ActionFailed : WaitingRoomEffect
}
