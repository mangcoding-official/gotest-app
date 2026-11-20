package com.gotest.app.feature.practice.ui.runner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.answerPayload
import com.gotest.app.feature.practice.domain.isAnswered
import com.gotest.app.feature.practice.domain.savedAnswer
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.ruang.data.ExamClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** A message under the question, as the web's `requestMessage` words it. */
sealed interface RunnerMessage {
    data class Server(
        val text: String,
    ) : RunnerMessage

    /** "Jawaban soal {n} tidak tersimpan. {reason}". */
    data class Refused(
        val number: Int,
        val reason: String?,
    ) : RunnerMessage

    data object Offline : RunnerMessage

    data object LoadFailed : RunnerMessage

    data object OpenFailed : RunnerMessage

    data object SaveFailed : RunnerMessage

    data object SubmitFailed : RunnerMessage
}

enum class RunnerDialog { None, Exit, Submit, Skip }

@Immutable
data class PracticeRunnerUiState(
    val attempt: PracticeAttempt? = null,
    val question: ActiveQuestion? = null,
    val selectedItemId: Long? = null,
    val advancingTo: Long? = null,
    val activationFailedFor: Long? = null,
    /** By `question_id`: what the student picked, saved or still on its way. */
    val answers: Map<Long, AnswerValue> = emptyMap(),
    val message: RunnerMessage? = null,
    /** The display order of a question whose time ran out ("Waktu soal {n} habis…"). */
    val timedOutNotice: Int? = null,
    val isSaving: Boolean = false,
    val isSubmitting: Boolean = false,
    val dialog: RunnerDialog = RunnerDialog.None,
    /** The Latihan ended while an answer was on its way: which question lost it. */
    val finishedAfterRejection: Int? = null,
    val secondsLeft: Long? = null,
    /** Cancelled by the Gotest team (410) or no longer a member (403): the server's sentence, nothing more to do. */
    val stopped: String? = null,
) {
    val items: List<PracticeItem> get() = attempt?.items.orEmpty()
    val activeItem: PracticeItem? get() = items.firstOrNull { it.id == selectedItemId }
    val questionReady: Boolean get() = question != null && activeItem != null && question.itemId == activeItem?.id
    val itemClosed: Boolean get() = activeItem?.closed == true
    val timeUp: Boolean get() = secondsLeft == 0L
    val currentIndex: Int get() = items.indexOfFirst { it.id == selectedItemId }
    val previousItem: PracticeItem? get() = items.getOrNull(currentIndex - 1)?.takeIf { currentIndex > 0 }
    val nextItem: PracticeItem? get() = if (currentIndex >= 0) items.getOrNull(currentIndex + 1) else null
    val answeredCount: Int get() = items.count { isAnswered(it, answers[it.questionId]) }
    val activeAnswer: AnswerValue? get() = activeItem?.let { answers[it.questionId] }

    /** The last question answered: "Selesai" opens the confirmation instead of a next button. */
    val canFinishHere: Boolean
        get() = attempt != null && activeItem != null && nextItem == null && isAnswered(activeItem!!, activeAnswer)
}

sealed interface PracticeRunnerEffect {
    data object OpenResult : PracticeRunnerEffect

    /** "Simpan & Lanjutkan Nanti": back to where the Latihan was opened from. */
    data object Leave : PracticeRunnerEffect
}

/**
 * The Latihan runner (web Practice/Runner, PracticeAttemptController): the server opens each question, every answer
 * is saved right away in order, the clock is the server's, and a Latihan timed per question goes in order (linear).
 * It follows Runner.tsx step by step.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class PracticeRunnerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: PracticeRepository,
        private val elapsed: ElapsedClock,
    ) : ViewModel() {
        // Read by name (the PracticeRunnerDestination field), so the view model also runs in unit tests.
        val attemptId: Long = checkNotNull(savedStateHandle[ARG_ATTEMPT])

        private val _uiState = MutableStateFlow(PracticeRunnerUiState())
        val uiState: StateFlow<PracticeRunnerUiState> = _uiState.asStateFlow()

        private val _effects = Channel<PracticeRunnerEffect>(Channel.BUFFERED)
        val effects: Flow<PracticeRunnerEffect> = _effects.receiveAsFlow()

        private data class Draft(
            val value: AnswerValue,
            val questionType: String?,
            val displayOrder: Int,
        )

        private val drafts = mutableMapOf<Long, Draft>()
        private val saveLock = Mutex()
        private var savesInFlight = 0
        private var loadSequence = 0
        private var loadErrorShown = false
        private var activationTarget: Long? = null
        private var rejectedAnswer: Int? = null
        private var submitting = false

        /** The Latihan is over for this screen (result opened, cancelled, or refused): no more reads. */
        private var ended = false
        private var resumedBefore = false
        private var clock: ExamClock? = null

        /** When the last attempt read was sent: `server_now` counts from here, as the web's `clockAnchor`. */
        private var anchorMs = 0L
        private var poller: Job? = null
        private var ticker: Job? = null
        private var closer: Job? = null

        init {
            viewModelScope.launch { loadAttempt() }
        }

        /** The screen is shown: read once when it comes back, then every 15 seconds, as the web does. */
        fun onResume() {
            poller?.cancel()
            if (ended) return
            val comingBack = resumedBefore
            resumedBefore = true
            poller =
                viewModelScope.launch {
                    if (comingBack) loadAttempt()
                    while (isActive) {
                        delay(POLL_MS)
                        loadAttempt()
                    }
                }
        }

        fun onPause() {
            poller?.cancel()
        }

        private suspend fun loadAttempt() {
            if (ended) return
            // Only the latest read may change the screen, so a slow one never overwrites a newer state.
            val sequence = ++loadSequence
            val requestedAt = elapsed.elapsedMs()
            val result = repository.attempt(attemptId)
            if (sequence != loadSequence || ended) return
            result
                .onSuccess { next -> if (next.running) show(next, requestedAt) else finished() }
                .onFailure { error ->
                    if (stopBy(error)) return@onFailure
                    loadErrorShown = true
                    _uiState.update { it.copy(message = requestMessage(error, RunnerMessage.LoadFailed)) }
                }
        }

        /**
         * The Latihan ended: explain a lost answer first, otherwise open the result, once. The reads stop here, so
         * the time-up reader never sends the result again (Ardy's review of A2c, item 6). `trySend`, because the
         * reader calling this is cancelled by [end].
         */
        private fun finished() {
            end()
            val lost = rejectedAnswer
            if (lost != null) {
                _uiState.update { it.copy(finishedAfterRejection = lost) }
            } else {
                _effects.trySend(PracticeRunnerEffect.OpenResult)
            }
        }

        private fun show(
            next: PracticeAttempt,
            requestedAt: Long,
        ) {
            rejectedAnswer = null
            val answers =
                next.items
                    .mapNotNull { item ->
                        (drafts[item.questionId]?.value ?: savedAnswer(item))?.let {
                            item.questionId to
                                it
                        }
                    }.toMap()
            _uiState.update {
                it.copy(
                    attempt = next,
                    answers = answers,
                    message = it.message.takeUnless { loadErrorShown },
                )
            }
            loadErrorShown = false
            anchorMs = requestedAt
            chooseTarget()
            resetClock()
        }

        /** Which question is on screen: linear follows the server, free keeps the student's choice. */
        private fun chooseTarget() {
            val state = _uiState.value
            val attempt = state.attempt ?: return
            val items = attempt.items
            if (attempt.linear) {
                chooseLinear(state, attempt)
            } else {
                val target =
                    items.firstOrNull { it.id == state.selectedItemId }
                        ?: items.firstOrNull { it.id == attempt.activeItemId }
                        ?: items.firstOrNull()
                _uiState.update { it.copy(selectedItemId = target?.id ?: it.selectedItemId) }
            }
            openSelected()
        }

        /** The active question, or the next one once it is closed; "Waktu soal n habis" when time took it. */
        private fun chooseLinear(
            state: PracticeRunnerUiState,
            attempt: PracticeAttempt,
        ) {
            val items = attempt.items
            val active = items.firstOrNull { it.id == attempt.activeItemId }
            val advancingTo = state.advancingTo?.takeUnless { it == active?.id }
            var notice = state.timedOutNotice
            val target =
                when {
                    advancingTo != null -> items.firstOrNull { it.id == advancingTo }
                    active == null -> items.firstOrNull()
                    !active.closed -> active
                    else -> items.firstOrNull { it.displayOrder > active.displayOrder }
                }
            val movedOn = target != null && target.id != state.selectedItemId
            if (advancingTo == null && active?.timedOut == true && movedOn) notice = active.displayOrder
            _uiState.update {
                it.copy(
                    advancingTo = advancingTo,
                    timedOutNotice = notice,
                    selectedItemId =
                        target?.id ?: it.selectedItemId,
                )
            }
        }

        /** Opens the chosen question once; a failed opening waits for "Coba Lagi". */
        private fun openSelected() {
            val state = _uiState.value
            val itemId = state.selectedItemId ?: return
            if (state.question?.itemId == itemId || activationTarget == itemId) return
            activate(itemId)
        }

        private fun activate(itemId: Long) {
            activationTarget = itemId
            _uiState.update { it.copy(activationFailedFor = null) }
            viewModelScope.launch {
                repository
                    .activate(attemptId, itemId)
                    .onSuccess { question ->
                        if (activationTarget == itemId) _uiState.update { it.copy(question = question) }
                    }.onFailure { error ->
                        if (stopBy(error)) return@onFailure
                        if (activationTarget == itemId) {
                            _uiState.update {
                                it.copy(
                                    activationFailedFor = itemId,
                                    advancingTo = null,
                                    message = requestMessage(error, RunnerMessage.OpenFailed),
                                )
                            }
                        }
                    }
                loadAttempt()
            }
        }

        fun retryActivation() {
            val itemId = _uiState.value.selectedItemId ?: return
            activationTarget = null
            _uiState.update { it.copy(message = null) }
            activate(itemId)
        }

        /** The countdown of the package, or of the question on screen when timed per question. */
        private fun resetClock() {
            ticker?.cancel()
            closer?.cancel()
            val attempt = _uiState.value.attempt ?: return
            val deadline = deadlineOf(attempt, _uiState.value.selectedItemId)
            clock =
                if (attempt.timerEnabled &&
                    deadline != null
                ) {
                    ExamClock.of(deadline, attempt.serverNow, anchorMs)
                } else {
                    null
                }
            tick()
            if (clock == null) return
            ticker =
                viewModelScope.launch {
                    var left = countdownLeft() ?: 0
                    while (isActive && left > 0) {
                        delay((left % SECOND).let { if (it == 0L) SECOND else it } + TICK_SLACK_MS)
                        tick()
                        left = countdownLeft() ?: 0
                    }
                    scheduleClose()
                }
        }

        private fun deadlineOf(
            attempt: PracticeAttempt,
            selectedItemId: Long?,
        ): String? =
            if (attempt.timerMode == PracticeAttempt.TIMER_PACKAGE) {
                attempt.deadlineAt
            } else {
                attempt.items.firstOrNull { it.id == selectedItemId }?.questionDeadlineAt
            }

        /** Milliseconds before the countdown reaches zero, half a second before the server's deadline. */
        private fun countdownLeft(): Long? = clock?.millisLeft(elapsed.elapsedMs())?.minus(COUNTDOWN_LEAD_MS)

        private fun tick() {
            val left = countdownLeft()
            _uiState.update { it.copy(secondsLeft = left?.let { ms -> (ms.coerceAtLeast(0) + SECOND - 1) / SECOND }) }
        }

        /** At zero, ask the server to close the question once its deadline passed, then every second. */
        private fun scheduleClose() {
            closer?.cancel()
            closer =
                viewModelScope.launch {
                    val serverLeft = (countdownLeft() ?: return@launch) + COUNTDOWN_LEAD_MS
                    delay(if (serverLeft > 0) serverLeft + CLOSE_SLACK_MS else SECOND)
                    while (isActive && _uiState.value.timeUp) {
                        loadAttempt()
                        delay(SECOND)
                    }
                }
        }

        fun onEvent(event: PracticeRunnerEvent) {
            when (event) {
                is PracticeRunnerEvent.SelectOption -> persist(AnswerValue.Single(event.index))
                is PracticeRunnerEvent.ToggleOption -> toggle(event.index)
                is PracticeRunnerEvent.StatementAnswer -> statement(event.row, event.label)
                is PracticeRunnerEvent.GoTo -> navigateTo(event.itemId)
                PracticeRunnerEvent.RetryActivation -> retryActivation()
                is PracticeRunnerEvent.ShowDialog -> _uiState.update { it.copy(dialog = event.dialog) }
                else -> onStep(event)
            }
        }

        /** Moving between questions and finishing. */
        private fun onStep(event: PracticeRunnerEvent) {
            when (event) {
                PracticeRunnerEvent.Previous -> _uiState.value.previousItem?.let { navigateTo(it.id) }
                PracticeRunnerEvent.Next -> _uiState.value.nextItem?.let { navigateTo(it.id) }
                PracticeRunnerEvent.Advance -> advanceOrAsk()
                PracticeRunnerEvent.ConfirmSkip -> advance()
                PracticeRunnerEvent.ConfirmSubmit -> submit()
                PracticeRunnerEvent.SaveAndExit -> saveAndExit()
                PracticeRunnerEvent.OpenResult -> _effects.trySend(PracticeRunnerEffect.OpenResult)
                PracticeRunnerEvent.Leave -> _effects.trySend(PracticeRunnerEffect.Leave)
                else -> Unit
            }
        }

        private fun persist(value: AnswerValue) {
            val state = _uiState.value
            val item = state.activeItem
            val question = state.question
            val open = state.questionReady && !state.itemClosed && !state.timeUp && state.stopped == null
            if (item == null || question == null || !open) return
            drafts[item.questionId] = Draft(value, question.questionType ?: item.questionType, item.displayOrder)
            _uiState.update { it.copy(answers = it.answers + (item.questionId to value), message = null) }
            viewModelScope.launch { enqueue(item.questionId) }
        }

        private fun toggle(index: Int) {
            val current = (_uiState.value.activeAnswer as? AnswerValue.Multiple)?.options.orEmpty()
            persist(AnswerValue.Multiple(if (index in current) current - index else current + index))
        }

        private fun statement(
            row: Int,
            label: Int,
        ) {
            val rows =
                _uiState.value.question
                    ?.content
                    ?.statements
                    ?.size ?: 0
            val current = (_uiState.value.activeAnswer as? AnswerValue.Statement)?.labels.orEmpty()
            val next = List(rows) { index -> if (index == row) label else current.getOrNull(index) }
            persist(AnswerValue.Statement(next))
        }

        /** One save at a time, in order (web `saveQueue`). */
        private suspend fun enqueue(questionId: Long): Boolean {
            savesInFlight++
            _uiState.update { it.copy(isSaving = true) }
            return try {
                saveLock.withLock { flush(questionId) }
            } finally {
                savesInFlight--
                _uiState.update { it.copy(isSaving = savesInFlight > 0) }
            }
        }

        private suspend fun flush(questionId: Long): Boolean {
            val draft = drafts[questionId]
            return when {
                ended -> false
                draft == null -> true
                else -> send(questionId, draft)
            }
        }

        private suspend fun send(
            questionId: Long,
            draft: Draft,
        ): Boolean {
            val result = repository.answer(attemptId, questionId, answerPayload(draft.questionType, draft.value))
            val error = result.exceptionOrNull()
            if (stopBy(error)) return false
            val permanent = error is ApiError && error.status in PERMANENT
            when {
                result.isSuccess || permanent -> {
                    // Refused for good (time up, locked, invalid): drop it so the runner never stays blocked.
                    if (drafts[questionId] === draft) drafts.remove(questionId)
                }

                else -> {
                    _uiState.update { it.copy(message = requestMessage(error, RunnerMessage.SaveFailed)) }
                }
            }
            if (permanent) {
                rejectedAnswer = draft.displayOrder
                _uiState.update {
                    it.copy(
                        message = RunnerMessage.Refused(draft.displayOrder, firstMessage(error)),
                    )
                }
                viewModelScope.launch { loadAttempt() }
            }
            return result.isSuccess || permanent
        }

        /** Waits for the saves on their way, then tries again those that failed for lack of a connection. */
        private suspend fun ensureSaved(): Boolean {
            saveLock.withLock { }
            for (questionId in drafts.keys.toList()) {
                if (!enqueue(questionId)) return false
            }
            return true
        }

        private fun navigateTo(itemId: Long) {
            viewModelScope.launch {
                if (!ensureSaved()) return@launch
                _uiState.update { it.copy(timedOutNotice = null, selectedItemId = itemId) }
                resetClock()
                openSelected()
            }
        }

        private fun advanceOrAsk() {
            val state = _uiState.value
            val item = state.activeItem ?: return
            if (isAnswered(
                    item,
                    state.activeAnswer,
                )
            ) {
                advance()
            } else {
                _uiState.update { it.copy(dialog = RunnerDialog.Skip) }
            }
        }

        private fun advance() {
            val next = _uiState.value.nextItem ?: return
            _uiState.update { it.copy(dialog = RunnerDialog.None) }
            viewModelScope.launch {
                if (!ensureSaved()) return@launch
                _uiState.update { it.copy(timedOutNotice = null, advancingTo = next.id) }
                chooseTarget()
            }
        }

        private fun submit() {
            if (_uiState.value.attempt == null || submitting) return
            submitting = true
            _uiState.update { it.copy(dialog = RunnerDialog.None, isSubmitting = true, message = null) }
            viewModelScope.launch {
                if (!ensureSaved()) {
                    submitting = false
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }
                repository
                    .submit(attemptId)
                    .onSuccess {
                        end()
                        _effects.send(PracticeRunnerEffect.OpenResult)
                    }.onFailure { error ->
                        submitting = false
                        if (stopBy(error)) return@onFailure
                        _uiState.update {
                            it.copy(isSubmitting = false, message = requestMessage(error, RunnerMessage.SubmitFailed))
                        }
                    }
            }
        }

        /** Nothing more happens on this screen: the reads and both clocks stop. */
        private fun end() {
            ended = true
            poller?.cancel()
            ticker?.cancel()
            closer?.cancel()
        }

        /**
         * 410 `cancelled` (DEC-118) or 403 `not_member` (T-039): the attempt cannot go on. Stop everything and show the
         * server's sentence; the student can only leave (Ardy's review of A2c, items 1 and 2).
         */
        private fun stopBy(error: Throwable?): Boolean {
            if (error !is ApiError || error.status !in GONE) return false
            end()
            drafts.clear()
            _uiState.update {
                it.copy(
                    stopped = error.message,
                    message = null,
                    dialog = RunnerDialog.None,
                    isSaving = false,
                    isSubmitting = false,
                )
            }
            return true
        }

        private fun saveAndExit() {
            viewModelScope.launch {
                if (!ensureSaved()) return@launch
                _uiState.update { it.copy(dialog = RunnerDialog.None) }
                _effects.send(PracticeRunnerEffect.Leave)
            }
        }

        companion object {
            const val ARG_ATTEMPT = "attemptId"
            private const val POLL_MS = 15_000L
            private const val SECOND = 1_000L
            private const val COUNTDOWN_LEAD_MS = 500L
            private const val TICK_SLACK_MS = 10L
            private const val CLOSE_SLACK_MS = 250L
            private val PERMANENT = setOf(404, 409, 422)

            /** Refusals that end the attempt for this student: cancelled (410) or no longer a member (403). */
            private val GONE = setOf(ApiError.FORBIDDEN, 410)
            private const val UNPROCESSABLE = 422

            /**
             * Web `requestMessage`: a 422 says why, no connection says so, anything else the fallback. A 403 or 410
             * says the server's sentence too (the web redirects with it instead).
             */
            fun requestMessage(
                error: Throwable?,
                fallback: RunnerMessage,
            ): RunnerMessage =
                when {
                    error !is ApiError -> RunnerMessage.Offline
                    error.status == UNPROCESSABLE -> firstMessage(error)?.let(RunnerMessage::Server) ?: fallback
                    error.status in GONE && error.message.isNotBlank() -> RunnerMessage.Server(error.message)
                    else -> fallback
                }

            fun firstMessage(error: ApiError): String? =
                if (error.status ==
                    UNPROCESSABLE
                ) {
                    error.fieldErrors.values
                        .firstOrNull()
                        ?.firstOrNull() ?: error.message
                } else {
                    null
                }
        }
    }

sealed interface PracticeRunnerEvent {
    data class SelectOption(
        val index: Int,
    ) : PracticeRunnerEvent

    data class ToggleOption(
        val index: Int,
    ) : PracticeRunnerEvent

    data class StatementAnswer(
        val row: Int,
        val label: Int,
    ) : PracticeRunnerEvent

    data class GoTo(
        val itemId: Long,
    ) : PracticeRunnerEvent

    data object Previous : PracticeRunnerEvent

    data object Next : PracticeRunnerEvent

    /** "Lanjut ke Soal Berikutnya" of a linear Latihan: asks first when the question is not answered. */
    data object Advance : PracticeRunnerEvent

    data object ConfirmSkip : PracticeRunnerEvent

    data object RetryActivation : PracticeRunnerEvent

    data class ShowDialog(
        val dialog: RunnerDialog,
    ) : PracticeRunnerEvent

    data object ConfirmSubmit : PracticeRunnerEvent

    data object SaveAndExit : PracticeRunnerEvent

    data object OpenResult : PracticeRunnerEvent

    /** "Kembali" once the Latihan was stopped: leave without saving anything. */
    data object Leave : PracticeRunnerEvent
}
