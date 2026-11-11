package com.gotest.app.feature.multiplayer.ui.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameType
import java.time.YearMonth

/** Filter pills of "Riwayat Main Bareng", in the design's order. */
enum class GameHistoryFilter(
    @param:StringRes val labelRes: Int,
    /** Games of this type only; null keeps every game. */
    val type: GameType?,
) {
    All(R.string.multiplayer_history_filter_all, null),
    Duel(R.string.multiplayer_type_duel, GameType.Duel),
    Room(R.string.multiplayer_type_room, GameType.Room),
}

/** The stats row; always counts the whole history, whatever the filter. */
data class GameHistoryStats(
    val games: Int,
    val duelWins: Int,
    /** Rooms the student finished first in ("Juara room"). */
    val roomWins: Int,
)

/** Games played in one calendar month ("Oktober 2026"), newest first. */
@Immutable
data class GameHistoryMonth(
    val month: YearMonth,
    val games: List<GameHistoryItem>,
) {
    val key: String get() = month.toString()
}

sealed interface MultiplayerHistoryUiState {
    data object Loading : MultiplayerHistoryUiState

    data object Error : MultiplayerHistoryUiState

    /** The student has not finished or cancelled any game yet. */
    data object Empty : MultiplayerHistoryUiState

    /** [months] is empty when the filter matches nothing. */
    @Immutable
    data class Success(
        val filter: GameHistoryFilter,
        val stats: GameHistoryStats,
        val months: List<GameHistoryMonth>,
    ) : MultiplayerHistoryUiState
}

sealed interface MultiplayerHistoryUiEvent {
    data class FilterSelect(
        val filter: GameHistoryFilter,
    ) : MultiplayerHistoryUiEvent

    data object Retry : MultiplayerHistoryUiEvent
}
