package com.gotest.app.feature.multiplayer.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import com.gotest.app.feature.multiplayer.ui.hub.CreateGameDraft
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.ruang.data.ExamClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the Main Bareng screens can ask for; [MainBarengViewModel] does it (state hoisting for the screens). */
@Suppress("TooManyFunctions")
interface MainBarengActions {
    fun load()

    fun openCreate()

    fun openJoin(code: String = "")

    fun closeDialog()

    fun resume()

    fun viewResult(code: String)

    fun updateDraft(change: (CreateGameDraft) -> CreateGameDraft)

    fun create()

    fun onJoinCode(code: String)

    fun submitJoin()

    fun startGame()

    fun cancelGame()

    fun leaveLobby()

    fun createAgain()

    fun onPlay(event: PlayEvent)

    fun playAgain()

    fun backToHub(fromWaiting: Boolean = false)
}

/**
 * Main Bareng in one space (web MainBareng/Index): the hub, "Buat permainan" and "Gabung dengan kode", the waiting
 * room, the shared countdown, answering, the waiting screen, and the result. The waiting room, the waiting
 * screen, and a live result are read again every 3 seconds while the screen is shown (DEC-085, API-K9).
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class MainBarengViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MainBarengRepository,
        private val elapsed: ElapsedClock,
    ) : ViewModel(),
        MainBarengActions {
        /** The Mitra code of this space, or null for Gotest.id. Read by name, so unit tests can set it. */
        val space: String? = savedStateHandle[ARG_SPACE]
        private val initialCode: String? = savedStateHandle[ARG_CODE]
        private val action: String? = savedStateHandle[ARG_ACTION]
        private var pendingResume = action == ACTION_RESUME
        private var pendingCreate = action == ACTION_CREATE

        private val _uiState = MutableStateFlow(MainBarengUiState())
        val uiState: StateFlow<MainBarengUiState> = _uiState.asStateFlow()

        private val _effects = Channel<MainBarengEffect>(Channel.BUFFERED)
        val effects: Flow<MainBarengEffect> = _effects.receiveAsFlow()

        private val play =
            GamePlay(
                scope = viewModelScope,
                repository = repository,
                state = _uiState,
                onFinished = ::afterFinish,
                onLeft = { toHub(HubFlash.LeftGame) },
            )

        /** When the game on screen was read: `serverNow` counts from here. */
        private var anchorMs = 0L
        private var ticker: Job? = null
        private var poller: Job? = null
        private var shown = false
        private var countdownDone = false

        init {
            load()
        }

        override fun load() {
            _uiState.update { it.copy(loading = true, loadFailed = false) }
            viewModelScope.launch {
                repository
                    .hub(space)
                    .onSuccess { page ->
                        _uiState.update { it.copy(loading = false, hub = page) }
                        refreshHub()
                        if (initialCode != null || action == ACTION_JOIN) openJoin(initialCode.orEmpty())
                    }.onFailure { _uiState.update { it.copy(loading = false, loadFailed = true) } }
            }
        }

        // Lifecycle: the reads stop while the app is in the background (API-K9).

        fun onResume() {
            shown = true
            updatePolling()
        }

        fun onPause() {
            shown = false
            poller?.cancel()
        }

        // The hub.

        private suspend fun refreshHub() {
            val current = viewModelScope.async { repository.active() }
            val past = repository.history(space)
            val active = current.await()
            if (active.isFailure || past.isFailure) {
                _uiState.update { it.copy(flash = HubFlash.HubFailed) }
                return
            }
            val game = active.getOrNull()
            _uiState.update { it.copy(active = game, history = past.getOrDefault(emptyList())) }
            if (pendingResume) {
                pendingResume = false
                game?.let(::resumeGame)
            }
            if (pendingCreate) {
                pendingCreate = false
                val dialog =
                    when {
                        !_uiState.value.canCreate -> HubDialog.Premium
                        game != null -> HubDialog.Busy
                        else -> HubDialog.Create
                    }
                _uiState.update { it.copy(dialog = dialog, create = CreateStatus()) }
            }
        }

        override fun openCreate() {
            val state = _uiState.value
            val dialog =
                when {
                    !state.canCreate -> HubDialog.Premium
                    state.active != null -> HubDialog.Busy
                    else -> HubDialog.Create
                }
            _uiState.update { it.copy(flash = null, dialog = dialog, create = CreateStatus()) }
        }

        override fun openJoin(code: String) {
            _uiState.update { it.copy(flash = null, dialog = HubDialog.Join, join = JoinForm(code = code)) }
            if (code.length == GameRules.CODE_LENGTH) lookup(code)
        }

        override fun closeDialog() {
            val state = _uiState.value
            if (state.create.submitting || state.join.busy) return
            _uiState.update { it.copy(dialog = HubDialog.None) }
        }

        /** Back to the game the viewer is still in, wherever it stands. */
        override fun resume() {
            _uiState.update { it.copy(dialog = HubDialog.None) }
            viewModelScope.launch {
                val current = repository.active().getOrNull()
                if (current == null) refreshHub() else resumeGame(current)
            }
        }

        private fun resumeGame(current: GameState) {
            val there = current.organization?.code
            if (there != space) {
                _effects.trySend(MainBarengEffect.OpenSpace(there))
                return
            }
            showGame(current, elapsed.elapsedMs())
            go(
                when {
                    current.waiting -> GameScreen.Lobby

                    current.youFinished -> GameScreen.Waiting

                    // Before the shared start this shows the rest of the countdown; after it, it goes straight on.
                    else -> GameScreen.Countdown
                },
            )
        }

        override fun viewResult(code: String) =
            runAction {
                val review = repository.result(code).getOrThrow()
                showGame(review.game, elapsed.elapsedMs())
                _uiState.update { it.copy(result = review.result, liveResult = false) }
                go(GameScreen.Result)
            }

        // Buat permainan.

        /** A new choice in the form; a refusal shown for the old choice goes away (web `setError(null)`). */
        override fun updateDraft(change: (CreateGameDraft) -> CreateGameDraft) {
            _uiState.update { it.copy(draft = change(it.draft), create = it.create.copy(problem = null)) }
        }

        override fun create() {
            val state = _uiState.value
            val settings = state.draft.settings(state.hub?.options.orEmpty()) ?: return
            if (state.create.submitting) return
            _uiState.update { it.copy(create = CreateStatus(submitting = true)) }
            viewModelScope.launch {
                val requestedAt = elapsed.elapsedMs()
                repository
                    .create(space, settings)
                    .onSuccess { game ->
                        showGame(game, requestedAt)
                        _uiState.update { it.copy(dialog = HubDialog.None, create = CreateStatus()) }
                        go(GameScreen.Lobby)
                    }.onFailure { error ->
                        _uiState.update { it.copy(create = CreateStatus(problem = GameProblem.of(error))) }
                    }
            }
        }

        // Gabung dengan kode.

        override fun onJoinCode(code: String) {
            val clean = GameRules.normalizeCode(code)
            _uiState.update { it.copy(join = it.join.copy(code = clean, problem = null, found = null)) }
            if (clean.length == GameRules.CODE_LENGTH) lookup(clean)
        }

        /** "Cari Permainan", or "Gabung" once a game was found. */
        override fun submitJoin() {
            val join = _uiState.value.join
            if (join.busy) return
            if (join.found != null) join(join.found.code) else lookup(join.code)
        }

        private fun lookup(code: String) {
            if (code.length != GameRules.CODE_LENGTH) {
                _uiState.update { it.copy(join = it.join.copy(problem = GameProblem.ShortCode)) }
                return
            }
            _uiState.update { it.copy(join = it.join.copy(busy = true, problem = null, found = null)) }
            viewModelScope.launch {
                val result = repository.lookup(code)
                _uiState.update {
                    it.copy(
                        join =
                            it.join.copy(
                                busy = false,
                                found = result.getOrNull(),
                                problem = result.exceptionOrNull()?.let(GameProblem::of),
                            ),
                    )
                }
            }
        }

        private fun join(code: String) {
            _uiState.update { it.copy(join = it.join.copy(busy = true, problem = null)) }
            viewModelScope.launch {
                val requestedAt = elapsed.elapsedMs()
                repository
                    .join(code)
                    .onSuccess { joined ->
                        _uiState.update { it.copy(dialog = HubDialog.None, join = JoinForm()) }
                        showGame(joined, requestedAt)
                        resumeGame(joined)
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(join = it.join.copy(busy = false, found = null, problem = GameProblem.of(error)))
                        }
                    }
            }
        }

        // The waiting room.

        override fun startGame() = runAction { handleState(repository.start(currentCode()).getOrThrow()) }

        override fun cancelGame() = runAction { handleState(repository.cancel(currentCode()).getOrThrow()) }

        override fun leaveLobby() =
            runAction {
                repository.leave(currentCode()).getOrThrow()
                toHub(HubFlash.LeftLobby)
            }

        /** "Buat Ulang" after the waiting room expired: the hub with "Buat permainan" open. */
        override fun createAgain() {
            toHub()
            _uiState.update { it.copy(dialog = HubDialog.Create, create = CreateStatus()) }
        }

        // Answering.

        override fun onPlay(event: PlayEvent) {
            when (event) {
                is PlayEvent.Select -> play.select(event.option)
                is PlayEvent.Toggle -> play.toggle(event.option)
                is PlayEvent.Statement -> play.statement(event.row, event.label)
                is PlayEvent.GoTo -> play.goTo(event.index)
                is PlayEvent.Modal -> play.showModal(event.modal)
                PlayEvent.Finish -> play.finish()
                PlayEvent.Leave -> play.leave()
            }
        }

        private fun startPlaying() {
            val code = _uiState.value.game?.code ?: return
            viewModelScope.launch {
                repository
                    .questions(code)
                    .onSuccess { questions ->
                        play.begin(code, questions)
                        go(GameScreen.Play)
                    }.onFailure {
                        // The time ran out during the countdown or the connection dropped; the state says which.
                        val next = repository.state(code).getOrNull()
                        if (next?.finished == true) showResult() else toHub(HubFlash.QuestionsFailed)
                    }
            }
        }

        private suspend fun afterFinish(next: GameState) {
            showGame(next, elapsed.elapsedMs())
            if (next.finished) showResult() else go(GameScreen.Waiting)
        }

        private suspend fun showResult() {
            val code = _uiState.value.game?.code ?: return
            val requestedAt = elapsed.elapsedMs()
            repository.result(code).onSuccess { review ->
                play.stop()
                showGame(review.game, requestedAt)
                _uiState.update { it.copy(result = review.result, liveResult = true, play = null) }
                go(GameScreen.Result)
            }
        }

        // The result.

        /** "Main Lagi" for the host, "Gabung Lagi" for the others: the next game's waiting room. */
        override fun playAgain() =
            runAction {
                val requestedAt = elapsed.elapsedMs()
                val next = repository.playAgain(currentCode()).getOrThrow()
                showGame(next, requestedAt)
                _uiState.update { it.copy(result = null, play = null) }
                go(GameScreen.Lobby)
            }

        /** "Kembali ke Main Bareng"; from the waiting screen the result waits in the history. */
        override fun backToHub(fromWaiting: Boolean) = toHub(if (fromWaiting) HubFlash.ResultLater else null)

        private fun toHub(flash: HubFlash? = null) {
            play.stop()
            _uiState.update {
                it.copy(
                    game = null,
                    play = null,
                    result = null,
                    error = null,
                    busy = false,
                    dialog = HubDialog.None,
                    flash = flash,
                )
            }
            go(GameScreen.Hub)
            viewModelScope.launch { refreshHub() }
        }

        // The game on screen, its countdown, and the reads every 3 seconds.

        private fun currentCode(): String = checkNotNull(_uiState.value.game?.code)

        /** Web `run`: one action at a time, its refusal in the box of the screen. */
        private fun runAction(action: suspend () -> Unit) {
            if (_uiState.value.busy) return
            _uiState.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                runCatching { action() }.onFailure { error ->
                    _uiState.update { it.copy(error = GameProblem.of(error)) }
                }
                _uiState.update { it.copy(busy = false) }
            }
        }

        private fun showGame(
            game: GameState,
            requestedAt: Long,
        ) {
            anchorMs = requestedAt
            _uiState.update { it.copy(game = game) }
            restartTicker()
        }

        /** Web `handleState`: the waiting room moves to the countdown, the waiting screen to the result. */
        private fun handleState(next: GameState) {
            showGame(next, elapsed.elapsedMs())
            val screen = _uiState.value.screen
            if (next.running && screen == GameScreen.Lobby) go(GameScreen.Countdown)
            if (next.finished && (screen == GameScreen.Waiting || screen == GameScreen.Play)) {
                viewModelScope.launch { showResult() }
            }
            updatePolling()
        }

        private fun go(screen: GameScreen) {
            if (screen == GameScreen.Countdown) countdownDone = false
            _uiState.update { it.copy(screen = screen) }
            restartTicker()
            updatePolling()
        }

        /** Only where the screen waits on others: the waiting room, the waiting screen, and a live result. */
        private fun pollingWanted(): Boolean {
            val state = _uiState.value
            val game = state.game ?: return false
            val watchResult =
                state.screen == GameScreen.Result && state.liveResult && !game.youAreHost && game.nextGameCode == null
            val waits = state.screen == GameScreen.Lobby || state.screen == GameScreen.Waiting || watchResult
            return shown && waits && !game.cancelled
        }

        private fun updatePolling() {
            if (!pollingWanted()) {
                poller?.cancel()
                poller = null
                return
            }
            if (poller?.isActive == true) return
            poller =
                viewModelScope.launch {
                    do {
                        delay(POLL_MS)
                        _uiState.value.game
                            ?.code
                            ?.let { readAgain(it) }
                    } while (isActive && pollingWanted())
                }
        }

        private suspend fun readAgain(code: String) {
            val requestedAt = elapsed.elapsedMs()
            repository
                .state(code)
                .onSuccess { next ->
                    anchorMs = requestedAt
                    handleState(next)
                }.onFailure { error ->
                    // A member taken out of the game (left the Mitra) goes back to the hub.
                    if (GameProblem.of(error).kind == GameProblem.NOT_FOUND) toHub(HubFlash.Removed)
                }
        }

        private fun restartTicker() {
            ticker?.cancel()
            tick()
            ticker =
                viewModelScope.launch {
                    while (isActive && target() != null) {
                        delay(TICK_MS)
                        tick()
                    }
                }
        }

        private fun target(): String? {
            val state = _uiState.value
            val game = state.game ?: return null
            return when (state.screen) {
                GameScreen.Lobby -> game.lobbyExpiresAt
                GameScreen.Countdown -> game.startedAt
                GameScreen.Play, GameScreen.Waiting -> game.endsAt
                else -> null
            }
        }

        private fun tick() {
            val target = target()
            val serverNow = _uiState.value.game?.serverNow
            val left =
                if (target != null && serverNow != null) {
                    val millis = ExamClock.of(target, serverNow, anchorMs).millisLeft(elapsed.elapsedMs())
                    (millis.coerceAtLeast(0) + SECOND - 1) / SECOND
                } else {
                    null
                }
            _uiState.update { it.copy(secondsLeft = left) }
            when (_uiState.value.screen) {
                GameScreen.Countdown -> {
                    if (left == 0L && !countdownDone) {
                        countdownDone = true
                        startPlaying()
                    }
                }

                GameScreen.Play -> {
                    if (left == 0L) play.finish()
                }

                else -> {}
            }
        }

        companion object {
            const val ARG_SPACE = "space"
            const val ARG_CODE = "joinCode"
            const val ARG_ACTION = "action"
            const val ACTION_JOIN = "gabung"
            const val ACTION_CREATE = "buat"
            const val ACTION_RESUME = "lanjut"
            private const val POLL_MS = 3_000L
            private const val TICK_MS = 250L
            private const val SECOND = 1_000L
        }
    }

/** What the student does while answering. */
sealed interface PlayEvent {
    data class Select(
        val option: Int,
    ) : PlayEvent

    data class Toggle(
        val option: Int,
    ) : PlayEvent

    data class Statement(
        val row: Int,
        val label: Int,
    ) : PlayEvent

    data class GoTo(
        val index: Int,
    ) : PlayEvent

    data class Modal(
        val modal: PlayModal,
    ) : PlayEvent

    data object Finish : PlayEvent

    data object Leave : PlayEvent
}
