package com.gotest.app.feature.multiplayer.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.recreateGame
import com.gotest.app.navigation.GameResultDestination
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GameResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MultiplayerRepository,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<GameResultDestination>().gameCode
        private val refresh = MutableStateFlow(0)
        private val replaying = MutableStateFlow(false)

        private val _effects = Channel<GameResultEffect>(Channel.BUFFERED)
        val effects: Flow<GameResultEffect> = _effects.receiveAsFlow()

        private val result: Flow<GameResult?> =
            refresh.flatMapLatest {
                repository
                    .observeResult(code)
                    .map<GameResult, GameResult?> { it }
                    .catch { emit(null) }
            }

        val uiState: StateFlow<GameResultUiState> =
            combine(result, replaying) { result, isReplaying ->
                if (result == null) GameResultUiState.Error else GameResultUiState.Success(result, isReplaying)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GameResultUiState.Loading)

        fun onEvent(event: GameResultUiEvent) {
            when (event) {
                GameResultUiEvent.Retry -> refresh.update { it + 1 }
                GameResultUiEvent.PlayAgain -> playAgain()
            }
        }

        private fun playAgain() {
            if (replaying.value) return
            viewModelScope.launch {
                replaying.value = true
                if (subscriptionRepository.observeSubscription().first().isPremium) {
                    repository
                        .recreateGame(code)
                        .onSuccess { _effects.send(GameResultEffect.Replayed(it)) }
                        .onFailure { _effects.send(GameResultEffect.Failed) }
                } else {
                    _effects.send(GameResultEffect.UpgradeRequired)
                }
                replaying.value = false
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
