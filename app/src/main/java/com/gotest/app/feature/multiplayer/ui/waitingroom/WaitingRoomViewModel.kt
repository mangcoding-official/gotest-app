package com.gotest.app.feature.multiplayer.ui.waitingroom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.feature.multiplayer.domain.CancelReason
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.clockTicks
import com.gotest.app.feature.multiplayer.domain.recreateGame
import com.gotest.app.navigation.WaitingRoomDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaitingRoomViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MultiplayerRepository,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<WaitingRoomDestination>().gameCode
        private val refresh = MutableStateFlow(0)
        private val busy = MutableStateFlow(false)
        private var startAnnounced = false

        private val _effects = Channel<WaitingRoomEffect>(Channel.BUFFERED)
        val effects: Flow<WaitingRoomEffect> = _effects.receiveAsFlow()

        private val game: Flow<Game?> =
            refresh
                .flatMapLatest {
                    repository
                        .observeGame(code)
                        .map<Game, Game?> { it }
                        .catch { emit(null) }
                }.onEach { game ->
                    // Everybody moves to the questions together once the host presses "Mulai".
                    val playing = game?.status == GameStatus.Started && game.currentUser != null
                    if (playing && !startAnnounced) {
                        startAnnounced = true
                        _effects.send(WaitingRoomEffect.GameStarted(code))
                    }
                }

        val uiState: StateFlow<WaitingRoomUiState> =
            combine(game, clockTicks(), busy) { game, now, isBusy ->
                if (game == null) {
                    WaitingRoomUiState.Error
                } else {
                    WaitingRoomUiState.Success(
                        game = game,
                        autoCancelSeconds = game.autoCancelAtMillis?.let { GameRules.secondsUntil(it, now) } ?: 0,
                        nowMillis = now,
                        isBusy = isBusy,
                        dialog = game.dialog(),
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WaitingRoomUiState.Loading)

        fun onEvent(event: WaitingRoomUiEvent) {
            when (event) {
                WaitingRoomUiEvent.Retry -> refresh.update { it + 1 }
                WaitingRoomUiEvent.Start -> runBusy { start() }
                WaitingRoomUiEvent.Leave -> runBusy { leave() }
                WaitingRoomUiEvent.Recreate -> runBusy { recreate() }
            }
        }

        private suspend fun start() {
            repository.startGame(code).onFailure { _effects.send(WaitingRoomEffect.ActionFailed) }
        }

        private suspend fun leave() {
            val state = uiState.value as? WaitingRoomUiState.Success
            if (state?.isHost == true) repository.cancelGame(code) else repository.leaveGame(code)
            _effects.send(WaitingRoomEffect.Left)
        }

        private suspend fun recreate() {
            if (!subscriptionRepository.observeSubscription().first().isPremium) {
                _effects.send(WaitingRoomEffect.UpgradeRequired)
                return
            }
            repository
                .recreateGame(code)
                .onSuccess { _effects.send(WaitingRoomEffect.Recreated(it)) }
                .onFailure { _effects.send(WaitingRoomEffect.ActionFailed) }
        }

        private fun runBusy(action: suspend () -> Unit) {
            if (busy.value) return
            viewModelScope.launch {
                busy.value = true
                try {
                    action()
                } finally {
                    busy.value = false
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

private fun Game.dialog(): WaitingRoomDialog? =
    when {
        status != GameStatus.Cancelled -> null
        cancelReason == CancelReason.AutoCancel -> WaitingRoomDialog.AutoCancelled
        !isHostedByCurrentUser -> WaitingRoomDialog.CancelledByHost
        else -> null
    }
