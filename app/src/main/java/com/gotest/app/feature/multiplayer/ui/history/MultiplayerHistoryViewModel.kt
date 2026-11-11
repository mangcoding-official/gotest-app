package com.gotest.app.feature.multiplayer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MultiplayerHistoryViewModel
    @Inject
    constructor(
        private val repository: MultiplayerRepository,
    ) : ViewModel() {
        private val refresh = MutableStateFlow(0)
        private val filter = MutableStateFlow(GameHistoryFilter.All)

        val uiState: StateFlow<MultiplayerHistoryUiState> =
            refresh
                .flatMapLatest {
                    combine(repository.observeHistory(), filter, ::buildState)
                        .onStart { emit(MultiplayerHistoryUiState.Loading) }
                        .catch { emit(MultiplayerHistoryUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    MultiplayerHistoryUiState.Loading,
                )

        fun onEvent(event: MultiplayerHistoryUiEvent) {
            when (event) {
                is MultiplayerHistoryUiEvent.FilterSelect -> filter.value = event.filter
                MultiplayerHistoryUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** The history arrives newest first, so the months and the rows inside them keep that order. */
private fun buildState(
    history: List<GameHistoryItem>,
    filter: GameHistoryFilter,
): MultiplayerHistoryUiState {
    if (history.isEmpty()) return MultiplayerHistoryUiState.Empty
    val months =
        history
            .filter { filter.type == null || it.settings.type == filter.type }
            .groupBy { YearMonth.from(it.playedAt) }
            .map { (month, games) -> GameHistoryMonth(month, games) }
    return MultiplayerHistoryUiState.Success(filter = filter, stats = history.stats(), months = months)
}

private fun List<GameHistoryItem>.stats(): GameHistoryStats =
    GameHistoryStats(
        games = size,
        duelWins = count { it.settings.type == GameType.Duel && it.outcome == HistoryOutcome.Won },
        roomWins = count { (it.outcome as? HistoryOutcome.Ranked)?.rank == 1 },
    )
