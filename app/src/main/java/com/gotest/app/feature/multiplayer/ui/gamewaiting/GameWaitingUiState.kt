package com.gotest.app.feature.multiplayer.ui.gamewaiting

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.PlayerState

sealed interface GameWaitingUiState {
    data object Loading : GameWaitingUiState

    data object Error : GameWaitingUiState

    @Immutable
    data class Success(
        val players: List<GamePlayer>,
        /** "Sisa waktu permainan". */
        val remainingSeconds: Int,
        /** Wall clock used for "Selesai 2 menit lalu". */
        val nowMillis: Long,
    ) : GameWaitingUiState {
        val finishedCount: Int get() = players.count { it.state == PlayerState.Finished }
    }
}

sealed interface GameWaitingUiEvent {
    data object Retry : GameWaitingUiEvent
}

sealed interface GameWaitingEffect {
    /** Everybody finished or the time ran out. */
    data class ResultReady(
        val code: String,
    ) : GameWaitingEffect
}
