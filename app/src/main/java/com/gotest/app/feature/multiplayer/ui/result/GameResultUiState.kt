package com.gotest.app.feature.multiplayer.ui.result

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.DuelOutcome
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameType

sealed interface GameResultUiState {
    data object Loading : GameResultUiState

    data object Error : GameResultUiState

    @Immutable
    data class Success(
        val result: GameResult,
        val isReplaying: Boolean = false,
    ) : GameResultUiState {
        val type: GameType get() = result.settings.type

        /** Menang / Kalah / Seri for duels; null for rooms. */
        val duelOutcome: DuelOutcome? get() = if (type == GameType.Duel) GameRules.duelOutcome(result) else null
    }
}

sealed interface GameResultUiEvent {
    data object Retry : GameResultUiEvent

    /** "Main Lagi": same settings, Premium only. */
    data object PlayAgain : GameResultUiEvent
}

sealed interface GameResultEffect {
    data object UpgradeRequired : GameResultEffect

    data class Replayed(
        val code: String,
    ) : GameResultEffect

    data object Failed : GameResultEffect
}
