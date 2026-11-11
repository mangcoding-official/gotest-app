package com.gotest.app.feature.multiplayer.ui.gamewaiting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.PlayerState
import com.gotest.app.feature.multiplayer.domain.clockTicks
import com.gotest.app.navigation.GameWaitingDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameWaitingViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: MultiplayerRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<GameWaitingDestination>().gameCode
        private val refresh = MutableStateFlow(0)
        private var resultAnnounced = false

        private val _effects = Channel<GameWaitingEffect>(Channel.BUFFERED)
        val effects: Flow<GameWaitingEffect> = _effects.receiveAsFlow()

        private val game: Flow<Game?> =
            refresh
                .flatMapLatest {
                    repository
                        .observeGame(code)
                        .map<Game, Game?> { it }
                        .catch { emit(null) }
                }.onEach { game ->
                    if (game?.status == GameStatus.Finished && !resultAnnounced) {
                        resultAnnounced = true
                        _effects.send(GameWaitingEffect.ResultReady(code))
                    }
                }

        val uiState: StateFlow<GameWaitingUiState> =
            combine(game, clockTicks()) { game, now ->
                if (game == null) {
                    GameWaitingUiState.Error
                } else {
                    GameWaitingUiState.Success(
                        // The student first, then whoever finished, then those still answering.
                        players =
                            game.players.sortedWith(
                                compareByDescending<GamePlayer> { it.isCurrentUser }
                                    .thenByDescending { it.state == PlayerState.Finished },
                            ),
                        remainingSeconds = game.endsAtMillis?.let { GameRules.secondsUntil(it, now) } ?: 0,
                        nowMillis = now,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameWaitingUiState.Loading)

        fun onEvent(event: GameWaitingUiEvent) {
            when (event) {
                GameWaitingUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
