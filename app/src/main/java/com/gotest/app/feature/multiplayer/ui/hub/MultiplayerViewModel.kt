package com.gotest.app.feature.multiplayer.ui.hub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Subscription
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.JoinLookup
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.navigation.MultiplayerDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MultiplayerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MultiplayerRepository,
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val refresh = MutableStateFlow(0)

        val uiState: StateFlow<MultiplayerUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        repository.observeHistory(),
                        subscriptionRepository.observeSubscription(),
                        repository.observeActiveGame(),
                        ::successState,
                    ).catch { emit(MultiplayerUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MultiplayerUiState.Loading)

        private val openJoinSheet = savedStateHandle.toRoute<MultiplayerDestination>().openJoinSheet
        private val _overlay =
            MutableStateFlow(MultiplayerOverlayState(joinSheet = if (openJoinSheet) JoinSheetState() else null))
        val overlay: StateFlow<MultiplayerOverlayState> = _overlay.asStateFlow()

        private val _effects = Channel<MultiplayerEffect>(Channel.BUFFERED)
        val effects: Flow<MultiplayerEffect> = _effects.receiveAsFlow()

        private var lookupJob: Job? = null

        fun onEvent(event: MultiplayerUiEvent) {
            when (event) {
                MultiplayerUiEvent.Retry -> {
                    refresh.update { it + 1 }
                }

                MultiplayerUiEvent.CreateGameClick -> {
                    onCreateGameClick()
                }

                MultiplayerUiEvent.DismissDialog -> {
                    _overlay.update { it.copy(dialog = null) }
                }

                MultiplayerUiEvent.UpgradeClick -> {
                    closeAndSend(MultiplayerEffect.OpenUpgrade)
                }

                MultiplayerUiEvent.OpenJoinSheet -> {
                    _overlay.update { it.copy(dialog = null, joinSheet = JoinSheetState()) }
                }

                MultiplayerUiEvent.DismissJoinSheet -> {
                    closeJoinSheet()
                }

                is MultiplayerUiEvent.CodeChange -> {
                    onCodeChange(event.code)
                }

                MultiplayerUiEvent.SearchGame -> {
                    search()
                }

                MultiplayerUiEvent.JoinGame -> {
                    join()
                }

                is MultiplayerUiEvent.ReturnToGame -> {
                    closeAndSend(MultiplayerEffect.OpenGame(event.game.code, event.game.resumeScreen()))
                }
            }
        }

        /** Creating needs Premium and no other running game (frames 13961 and 14405). */
        private fun onCreateGameClick() {
            val state = uiState.value as? MultiplayerUiState.Success ?: return
            val activeGame = state.activeGame
            when {
                !state.isPremium -> _overlay.update { it.copy(dialog = HubDialog.PremiumRequired) }
                activeGame != null -> _overlay.update { it.copy(dialog = HubDialog.InAnotherGame(activeGame)) }
                else -> viewModelScope.launch { _effects.send(MultiplayerEffect.OpenCreateGame) }
            }
        }

        private fun onCodeChange(raw: String) {
            val code = GameRules.normalizeCode(raw)
            val sheet = _overlay.value.joinSheet
            if (sheet == null || code == sheet.code) return
            lookupJob?.cancel()
            _overlay.update { it.copy(joinSheet = JoinSheetState(code = code)) }
            if (code.length == GameRules.CODE_LENGTH) search()
        }

        private fun search() {
            val sheet = _overlay.value.joinSheet
            if (sheet == null || !sheet.isComplete || sheet.status == JoinStatus.Searching) return
            updateSheet { it.copy(status = JoinStatus.Searching) }
            lookupJob =
                viewModelScope.launch {
                    val status = repository.findGame(sheet.code).toStatus()
                    updateSheet { if (it.code == sheet.code) it.copy(status = status) else it }
                }
        }

        private fun join() {
            val sheet = _overlay.value.joinSheet
            val found = sheet?.status as? JoinStatus.Found
            if (sheet == null || found == null || found.isJoining) return
            updateSheet { it.copy(status = found.copy(isJoining = true)) }
            viewModelScope.launch {
                repository
                    .joinGame(sheet.code)
                    .onSuccess {
                        closeAndSend(MultiplayerEffect.OpenGame(found.game.code, GameScreen.WaitingRoom))
                    }.onFailure {
                        // The lobby changed in the meantime (filled up, started, …): show why.
                        val status = repository.findGame(sheet.code).toStatus()
                        updateSheet { it.copy(status = status) }
                    }
            }
        }

        private fun closeJoinSheet() {
            lookupJob?.cancel()
            _overlay.update { it.copy(joinSheet = null) }
        }

        private fun closeAndSend(effect: MultiplayerEffect) {
            lookupJob?.cancel()
            _overlay.value = MultiplayerOverlayState()
            viewModelScope.launch { _effects.send(effect) }
        }

        private fun updateSheet(transform: (JoinSheetState) -> JoinSheetState) {
            _overlay.update { overlay -> overlay.copy(joinSheet = overlay.joinSheet?.let(transform)) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

private fun successState(
    history: List<GameHistoryItem>,
    subscription: Subscription,
    activeGame: Game?,
): MultiplayerUiState =
    MultiplayerUiState.Success(history = history, isPremium = subscription.isPremium, activeGame = activeGame)

private fun JoinLookup.toStatus(): JoinStatus =
    when (this) {
        is JoinLookup.Found -> JoinStatus.Found(game)
        JoinLookup.NotFound -> JoinStatus.NotFound
        JoinLookup.AlreadyStarted -> JoinStatus.AlreadyStarted
        JoinLookup.Full -> JoinStatus.Full
        JoinLookup.Inactive -> JoinStatus.Inactive
        is JoinLookup.InAnotherGame -> JoinStatus.InAnotherGame(activeGame)
    }
