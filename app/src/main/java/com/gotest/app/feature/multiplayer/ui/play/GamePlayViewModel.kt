package com.gotest.app.feature.multiplayer.ui.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSession
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.PlayerState
import com.gotest.app.feature.multiplayer.domain.clockTicks
import com.gotest.app.navigation.GamePlayDestination
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the student did locally; merged with the loaded session into the UI state. */
private data class PlayProgress(
    val initialized: Boolean = false,
    val currentIndex: Int = 0,
    val answers: Map<String, QuestionAnswer> = emptyMap(),
    /** Question ids answered while offline, not sent yet. */
    val pending: Set<String> = emptySet(),
    val sheet: PlaySheet? = null,
    val submitting: Boolean = false,
    val submitted: Boolean = false,
)

private sealed interface SessionLoad {
    data object Loading : SessionLoad

    data object Failed : SessionLoad

    data class Loaded(
        val session: GameSession,
    ) : SessionLoad
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GamePlayViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MultiplayerRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<GamePlayDestination>().gameCode
        private val refresh = MutableStateFlow(0)
        private val progress = MutableStateFlow(PlayProgress())
        private val online = MutableStateFlow(true)

        private val _effects = Channel<GamePlayEffect>(Channel.BUFFERED)
        val effects: Flow<GamePlayEffect> = _effects.receiveAsFlow()

        private val session: StateFlow<SessionLoad> =
            refresh
                .flatMapLatest {
                    flow<SessionLoad> {
                        emit(SessionLoad.Loading)
                        emit(SessionLoad.Loaded(repository.loadSession(code)))
                    }.catch { emit(SessionLoad.Failed) }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionLoad.Loading)

        val uiState: StateFlow<GamePlayUiState> =
            combine(session, progress, clockTicks(), online) { load, local, now, isOnline ->
                when (load) {
                    SessionLoad.Loading -> GamePlayUiState.Loading
                    SessionLoad.Failed -> GamePlayUiState.Error
                    is SessionLoad.Loaded -> load.session.toUiState(local, now, isOnline)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GamePlayUiState.Loading)

        init {
            session.onEach { if (it is SessionLoad.Loaded) onSessionLoaded(it.session) }.launchIn(viewModelScope)
            repository.connection
                .onEach { connected ->
                    online.value = connected
                    if (connected) flushPending()
                }.launchIn(viewModelScope)
        }

        fun onEvent(event: GamePlayUiEvent) {
            when (event) {
                GamePlayUiEvent.Retry -> refresh.update { it + 1 }
                is GamePlayUiEvent.Answer -> answer(event.answer)
                GamePlayUiEvent.Next -> move(1)
                GamePlayUiEvent.Previous -> move(-1)
                is GamePlayUiEvent.GoTo -> goTo(event.index)
                GamePlayUiEvent.OpenNavigator -> progress.update { it.copy(sheet = PlaySheet.Navigator) }
                GamePlayUiEvent.OpenFinish -> progress.update { it.copy(sheet = PlaySheet.ConfirmFinish) }
                GamePlayUiEvent.DismissSheet -> progress.update { it.copy(sheet = null) }
                GamePlayUiEvent.ConfirmFinish -> submit()
            }
        }

        private suspend fun onSessionLoaded(session: GameSession) {
            val game = session.game
            val alreadyDone = game.status == GameStatus.Finished || game.currentUser?.state == PlayerState.Finished
            if (alreadyDone) {
                progress.update { it.copy(submitted = true) }
                _effects.send(GamePlayEffect.Submitted(code))
                return
            }
            progress.update { if (it.initialized) it else it.copy(initialized = true, answers = session.answers) }
            // Time is up for everybody at the same moment: send whatever is answered.
            val endsAt = game.endsAtMillis ?: return
            viewModelScope.launch {
                clockTicks().first { it >= endsAt }
                submit()
            }
        }

        private fun answer(answer: QuestionAnswer) {
            val loaded = session.value as? SessionLoad.Loaded ?: return
            val question = loaded.session.questions[progress.value.currentIndex]
            progress.update { it.copy(answers = it.answers + (question.id to answer)) }
            if (online.value) {
                viewModelScope.launch { repository.saveAnswer(code, question.id, answer) }
            } else {
                progress.update { it.copy(pending = it.pending + question.id) }
            }
        }

        private fun move(step: Int) {
            val loaded = session.value as? SessionLoad.Loaded ?: return
            val target = progress.value.currentIndex + step
            if (target > loaded.session.questions.lastIndex) {
                progress.update { it.copy(sheet = PlaySheet.ConfirmFinish) }
            } else {
                goTo(target)
            }
        }

        private fun goTo(index: Int) {
            val loaded = session.value as? SessionLoad.Loaded ?: return
            val target = index.coerceIn(0, loaded.session.questions.lastIndex)
            progress.update { it.copy(currentIndex = target, sheet = null) }
        }

        private suspend fun flushPending() {
            val local = progress.value
            local.pending.forEach { id -> local.answers[id]?.let { repository.saveAnswer(code, id, it) } }
            progress.update { it.copy(pending = it.pending - local.pending) }
        }

        private fun submit() {
            val local = progress.value
            if (local.submitting || local.submitted) return
            progress.update { it.copy(submitting = true) }
            viewModelScope.launch {
                flushPending()
                repository.submitAnswers(code)
                progress.update { it.copy(submitting = false, submitted = true, sheet = null) }
                _effects.send(GamePlayEffect.Submitted(code))
            }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

private fun GameSession.toUiState(
    local: PlayProgress,
    now: Long,
    isOnline: Boolean,
): GamePlayUiState {
    val endsAt = game.endsAtMillis
    val numbers = questions.filter { it.id in local.pending }.map { it.number }
    return GamePlayUiState.Success(
        type = game.type,
        opponentName = game.opponent?.name,
        playerCount = game.players.size,
        questions = questions,
        currentIndex = local.currentIndex.coerceIn(0, questions.lastIndex),
        answers = local.answers,
        remainingSeconds =
            endsAt?.let { GameRules.secondsUntil(it, now) }
                ?: (game.settings.durationMinutes * GameRules.SECONDS_PER_MINUTE),
        isOffline = !isOnline,
        unsavedQuestionNumbers = numbers,
        sheet = local.sheet,
        isSubmitting = local.submitting,
    )
}
