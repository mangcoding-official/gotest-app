package com.gotest.app.feature.multiplayer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Subscription
import com.gotest.app.feature.multiplayer.domain.CreateBlock
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.ui.hub.resumeScreen
import com.gotest.app.navigation.GameDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: MultiplayerRepository,
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<GameDetailDestination>().gameCode
        private val refresh = MutableStateFlow(0)

        val uiState: StateFlow<GameDetailUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        repository.observeGame(code),
                        subscriptionRepository.observeSubscription(),
                        repository.observeActiveGame(),
                        ::successState,
                    ).onStart { emit(GameDetailUiState.Loading) }
                        .catch { emit(GameDetailUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameDetailUiState.Loading)

        private val _dialog = MutableStateFlow<CreateBlock?>(null)
        val dialog: StateFlow<CreateBlock?> = _dialog.asStateFlow()

        private val _effects = Channel<GameDetailEffect>(Channel.BUFFERED)
        val effects: Flow<GameDetailEffect> = _effects.receiveAsFlow()

        fun onEvent(event: GameDetailUiEvent) {
            when (event) {
                GameDetailUiEvent.Retry -> {
                    refresh.update { it + 1 }
                }

                GameDetailUiEvent.RecreateClick -> {
                    onRecreateClick()
                }

                GameDetailUiEvent.DismissDialog -> {
                    _dialog.value = null
                }

                GameDetailUiEvent.UpgradeClick -> {
                    closeAndSend(GameDetailEffect.OpenUpgrade)
                }

                GameDetailUiEvent.JoinWithCodeClick -> {
                    closeAndSend(GameDetailEffect.OpenJoinSheet)
                }

                is GameDetailUiEvent.ReturnToGame -> {
                    closeAndSend(GameDetailEffect.OpenGame(event.game.code, event.game.resumeScreen()))
                }
            }
        }

        /** Same rules as the hub's "Buat Permainan"; the prefilled form then checks the stock. */
        private fun onRecreateClick() {
            val state = uiState.value as? GameDetailUiState.Success ?: return
            val block = GameRules.createBlock(state.isPremium, state.activeGame)
            if (block != null) {
                _dialog.value = block
            } else {
                viewModelScope.launch { _effects.send(GameDetailEffect.OpenCreateGame(code)) }
            }
        }

        private fun closeAndSend(effect: GameDetailEffect) {
            _dialog.value = null
            viewModelScope.launch { _effects.send(effect) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

private fun successState(
    game: Game,
    subscription: Subscription,
    activeGame: Game?,
): GameDetailUiState =
    GameDetailUiState.Success(
        game = game,
        createdAt = game.createdAtMillis.toLocalDateTime(),
        cancelledAt = game.cancelledAtMillis?.toLocalDateTime(),
        isPremium = subscription.isPremium,
        activeGame = activeGame,
    )

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
