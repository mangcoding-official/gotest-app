package com.gotest.app.feature.tryout.ui.runner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.domain.answerPayload
import com.gotest.app.feature.practice.domain.isAnswered
import com.gotest.app.feature.practice.domain.savedAnswer
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerViewModel
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.ruang.data.ExamClock
import com.gotest.app.feature.tryout.domain.TryoutActiveSection
import com.gotest.app.feature.tryout.domain.TryoutAttempt
import com.gotest.app.feature.tryout.domain.TryoutItem
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutSectionSummary
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

/** A message above the question, as the web's `requestMessage` words it. */
sealed interface TryoutMessage {
    data class Server(
        val text: String,
    ) : TryoutMessage

    /** "Jawaban soal {n} tidak tersimpan. {reason}". */
    data class Refused(
        val number: Int,
        val reason: String?,
    ) : TryoutMessage

    /** "{reason} Pilihan Anda dipertahankan; periksa koneksi lalu coba lagi." */
    data class NotSaved(
        val offline: Boolean,
        val reason: String?,
    ) : TryoutMessage

    data object Offline : TryoutMessage

    /** "Status Try Out belum dapat diperbarui." */
    data object StatusFailed : TryoutMessage

    data object FlagFailed : TryoutMessage

    data object StaleDrafts : TryoutMessage

    data object KeepAnswering : TryoutMessage
}

enum class TryoutDialog { None, Action, Exit, End }

@Immutable
data class TryoutRunnerUiState(
    val attempt: TryoutAttempt? = null,
    val selectedItemId: Long? = null,
    /** By item id: what the student picked, saved or still on its way. */
    val answers: Map<Long, AnswerValue> = emptyMap(),
    val flags: Map<Long, Boolean> = emptyMap(),
    val message: TryoutMessage? = null,
    val isSaving: Boolean = false,
    val hasUnsaved: Boolean = false,
    val isFinishing: Boolean = false,
    val isBeginning: Boolean = false,
    val dialog: TryoutDialog = TryoutDialog.None,
    val secondsLeft: Long? = null,
    /** Cancelled (410) or no longer a member (403): the server's sentence, nothing more to do. */
    val stopped: String? = null,
) {
    val section: TryoutActiveSection? get() = attempt?.activeSection
    val items: List<TryoutItem> get() = section?.items.orEmpty()
    val activeItem: TryoutItem? get() = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull()
    val currentIndex: Int get() = activeItem?.let { items.indexOf(it) } ?: -1
    val previousItem: TryoutItem? get() = items.getOrNull(currentIndex - 1)?.takeIf { currentIndex > 0 }
    val nextItem: TryoutItem? get() = if (currentIndex >= 0) items.getOrNull(currentIndex + 1) else null
    val isLastQuestion: Boolean get() = currentIndex >= 0 && currentIndex == items.lastIndex
    val timeUp: Boolean get() = secondsLeft == 0L

    /** Answers and ragu-ragu may change: the attempt is read, time is left, and it was not stopped. */
    val canChange: Boolean get() = attempt != null && !timeUp && stopped == null
    val sectionIndex: Int get() = attempt?.sections?.indexOfFirst { it.id == section?.id } ?: -1
    val isLastSection: Boolean get() =
        attempt?.let { sectionIndex < 0 || sectionIndex == it.sections.lastIndex }
            ?: true

    fun answered(item: TryoutItem): Boolean = isAnswered(item.asPractice(), answers[item.id])

    val answeredCount: Int get() = items.count(::answered)
}

/** The answer helpers read a Try Out item as a Latihan one (same three shapes). */
fun TryoutItem.asPractice() =
    PracticeItem(
        id = id,
        questionId = questionId ?: id,
        displayOrder = displayOrder,
        answeredAt = answeredAt,
        questionType = question.questionType,
        selectedOption = selectedOption,
        selectedOptions = selectedOptions,
        statementAnswers = statementAnswers,
    )

sealed interface TryoutRunnerEffect {
    data object OpenResult : TryoutRunnerEffect

    /** Cancelled before the first subtest (DEC-090), or "Keluar sementara": back to where it was opened. */
    data object Leave : TryoutRunnerEffect
}

/**
 * The Try Out runner (web Tryout/Runner, TryoutAttemptController): a section introduction before each timer, the
 * questions of the running section, answers saved one by one, ragu-ragu, the section clock on the server, and the
 * next section starting by itself when time runs out (DEC-062 to DEC-067). It follows Runner.tsx step by step.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class TryoutRunnerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TryoutRepository,
        private val elapsed: ElapsedClock,
    ) : ViewModel() {
        val attemptId: Long = checkNotNull(savedStateHandle[ARG_ATTEMPT])

        private val _uiState = MutableStateFlow(TryoutRunnerUiState())
        val uiState: StateFlow<TryoutRunnerUiState> = _uiState.asStateFlow()

        private val _effects = Channel<TryoutRunnerEffect>(Channel.BUFFERED)
        val effects: Flow<TryoutRunnerEffect> = _effects.receiveAsFlow()

        private val drafts = mutableMapOf<Long, AnswerValue>()
        private val saveLock = Mutex()
        private var loadSequence = 0
        private var anchorMs = 0L
        private var clock: ExamClock? = null
        private var poller: Job? = null
        private var ticker: Job? = null

        /** The Try Out is over for this screen (result, leave, or refused): no more reads. */
        private var ended = false
        private var resumedBefore = false

        init {
            viewModelScope.launch { load() }
        }

        /** The screen is shown: read once when it comes back, then every 15 seconds, as the web does. */
        fun onResume() {
            poller?.cancel()
            if (ended) return
            val comingBack = resumedBefore
            resumedBefore = true
            poller =
                viewModelScope.launch {
                    if (comingBack) load()
                    while (isActive) {
                        delay(POLL_MS)
                        load()
                    }
                }
        }

        fun onPause() {
            poller?.cancel()
        }

        private suspend fun load() {
            if (ended) return
            val sequence = ++loadSequence
            val requestedAt = elapsed.elapsedMs()
            val result = repository.attempt(attemptId)
            if (sequence != loadSequence || ended) return
            result
                .onSuccess { apply(it, requestedAt) }
                .onFailure { error ->
                    if (stopBy(error)) return@onFailure
                    _uiState.update { it.copy(message = requestMessage(error, TryoutMessage.StatusFailed)) }
                }
        }

        private fun apply(
            next: TryoutAttempt,
            requestedAt: Long,
        ) {
            if (!next.running) {
                // Once: the reads stop, so the time-up reader never sends it again (Ardy's review of A2c, item 6).
                end()
                _effects.trySend(
                    if (next.status ==
                        TryoutAttempt.STATUS_CANCELLED
                    ) {
                        TryoutRunnerEffect.Leave
                    } else {
                        TryoutRunnerEffect.OpenResult
                    },
                )
                return
            }
            // An answer waiting for a question outside the running section can never be saved: its section ended.
            val active =
                next.activeSection
                    ?.items
                    .orEmpty()
                    .map { it.id }
                    .toSet()
            val stale = drafts.keys.filterNot { it in active }
            stale.forEach(drafts::remove)
            val answers =
                next.activeSection
                    ?.items
                    .orEmpty()
                    .mapNotNull { item ->
                        (
                            drafts[item.id]
                                ?: savedAnswer(item.asPractice())
                        )?.let { item.id to it }
                    }.toMap()
            _uiState.update {
                it.copy(
                    attempt = next,
                    answers = answers,
                    flags =
                        next.activeSection
                            ?.items
                            .orEmpty()
                            .associate { item -> item.id to item.isFlagged },
                    hasUnsaved = drafts.isNotEmpty(),
                    message = if (stale.isNotEmpty()) TryoutMessage.StaleDrafts else it.message,
                    selectedItemId = it.selectedItemId?.takeIf { id -> id in active },
                )
            }
            anchorMs = requestedAt
            resetClock()
        }

        private fun resetClock() {
            ticker?.cancel()
            val attempt = _uiState.value.attempt
            val deadline = attempt?.activeSection?.deadlineAt
            val serverNow = attempt?.serverNow
            clock = if (deadline != null && serverNow != null) ExamClock.of(deadline, serverNow, anchorMs) else null
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
                    // At zero, ask the server to close the section once its deadline passed, then every second.
                    val serverLeft = (countdownLeft() ?: 0) + COUNTDOWN_LEAD_MS
                    delay(if (serverLeft > 0) serverLeft + CLOSE_SLACK_MS else SECOND)
                    while (isActive && _uiState.value.timeUp) {
                        load()
                        delay(SECOND)
                    }
                }
        }

        private fun countdownLeft(): Long? = clock?.millisLeft(elapsed.elapsedMs())?.minus(COUNTDOWN_LEAD_MS)

        private fun tick() {
            val left = countdownLeft()
            _uiState.update { it.copy(secondsLeft = left?.let { ms -> (ms.coerceAtLeast(0) + SECOND - 1) / SECOND }) }
        }

        fun onEvent(event: TryoutRunnerEvent) {
            when (event) {
                is TryoutRunnerEvent.SelectOption -> persist(AnswerValue.Single(event.index))
                is TryoutRunnerEvent.ToggleOption -> toggle(event.index)
                is TryoutRunnerEvent.StatementAnswer -> statement(event.row, event.label)
                is TryoutRunnerEvent.GoTo -> navigateTo(event.itemId)
                TryoutRunnerEvent.ToggleFlag -> toggleFlag()
                is TryoutRunnerEvent.ShowDialog -> _uiState.update { it.copy(dialog = event.dialog) }
                else -> onStep(event)
            }
        }

        private fun onStep(event: TryoutRunnerEvent) {
            when (event) {
                TryoutRunnerEvent.Previous -> {
                    _uiState.value.previousItem?.let { navigateTo(it.id) }
                }

                TryoutRunnerEvent.Next -> {
                    _uiState.value.nextItem?.let { navigateTo(it.id) }
                }

                TryoutRunnerEvent.Begin -> {
                    begin()
                }

                TryoutRunnerEvent.ConfirmAction -> {
                    finishSection()
                }

                TryoutRunnerEvent.ConfirmEnd -> {
                    finishAll()
                }

                TryoutRunnerEvent.Leave -> {
                    _effects.trySend(TryoutRunnerEffect.Leave)
                }

                else -> {}
            }
        }

        private fun persist(value: AnswerValue) {
            val state = _uiState.value
            val item = state.activeItem
            if (!state.canChange || item == null) return
            drafts[item.id] = value
            _uiState.update {
                it.copy(answers = it.answers + (item.id to value), hasUnsaved = true, isSaving = true, message = null)
            }
            viewModelScope.launch { saveLock.withLock { save(item) } }
        }

        /** One save at a time; the latest value of the item goes (web `pendingSaveQueue`). */
        private suspend fun save(item: TryoutItem) {
            val latest = drafts[item.id]
            if (latest != null && !ended) {
                val result = repository.answer(attemptId, item.id, answerPayload(item.question.questionType, latest))
                val error = result.exceptionOrNull()
                if (stopBy(error)) return
                val permanent = error is ApiError && error.status in PERMANENT
                if (result.isSuccess || permanent) {
                    if (drafts[item.id] === latest) drafts.remove(item.id)
                }
                when {
                    permanent -> {
                        _uiState.update {
                            it.copy(
                                message = TryoutMessage.Refused(item.displayOrder, firstMessage(error)),
                            )
                        }
                    }

                    error != null -> {
                        _uiState.update {
                            it.copy(
                                message =
                                    TryoutMessage.NotSaved(
                                        error !is ApiError,
                                        (error as? ApiError)?.let(::firstMessage),
                                    ),
                            )
                        }
                    }
                }
                _uiState.update { it.copy(hasUnsaved = drafts.isNotEmpty()) }
                if (result.isSuccess || permanent) load()
            }
            _uiState.update { it.copy(isSaving = false) }
        }

        private fun toggle(index: Int) {
            val item = _uiState.value.activeItem ?: return
            val current = (_uiState.value.answers[item.id] as? AnswerValue.Multiple)?.options.orEmpty()
            persist(AnswerValue.Multiple(if (index in current) current - index else current + index))
        }

        private fun statement(
            row: Int,
            label: Int,
        ) {
            val item = _uiState.value.activeItem ?: return
            val rows =
                item.question.content
                    ?.statements
                    ?.size ?: 0
            val current = (_uiState.value.answers[item.id] as? AnswerValue.Statement)?.labels.orEmpty()
            persist(
                AnswerValue.Statement(
                    List(rows) { index ->
                        if (index ==
                            row
                        ) {
                            label
                        } else {
                            current.getOrNull(index)
                        }
                    },
                ),
            )
        }

        /** Waits for the saves on their way; any answer still waiting blocks (web `ensureAnswersSaved`). */
        private suspend fun ensureSaved(): Boolean {
            saveLock.withLock { }
            if (drafts.isEmpty()) return true
            _uiState.update { it.copy(message = TryoutMessage.KeepAnswering) }
            return false
        }

        private fun navigateTo(itemId: Long) {
            viewModelScope.launch { if (ensureSaved()) _uiState.update { it.copy(selectedItemId = itemId) } }
        }

        private fun toggleFlag() {
            val state = _uiState.value
            val item = state.activeItem
            if (!state.canChange || item == null) return
            val next = !(state.flags[item.id] ?: false)
            _uiState.update { it.copy(flags = it.flags + (item.id to next)) }
            viewModelScope.launch {
                repository.flag(attemptId, item.id, next).onFailure { error ->
                    if (stopBy(error)) return@onFailure
                    _uiState.update {
                        it.copy(
                            flags = it.flags + (item.id to !next),
                            message = requestMessage(error, TryoutMessage.FlagFailed),
                        )
                    }
                    if (error is ApiError && error.status in PERMANENT) load()
                }
            }
        }

        private fun begin() {
            if (_uiState.value.attempt == null || _uiState.value.isBeginning) return
            _uiState.update { it.copy(isBeginning = true, message = null) }
            viewModelScope.launch {
                val requestedAt = elapsed.elapsedMs()
                repository
                    .begin(attemptId)
                    .onSuccess { next ->
                        loadSequence++
                        _uiState.update { it.copy(selectedItemId = null) }
                        apply(next, requestedAt)
                    }.onFailure { error ->
                        if (!stopBy(error)) {
                            _uiState.update { it.copy(message = requestMessage(error, TryoutMessage.StatusFailed)) }
                        }
                    }
                _uiState.update { it.copy(isBeginning = false) }
            }
        }

        /** Akhiri Subtes & Lanjut, or Selesaikan Try Out on the last section. */
        private fun finishSection() {
            if (_uiState.value.attempt == null || _uiState.value.isFinishing) return
            _uiState.update { it.copy(dialog = TryoutDialog.None, isFinishing = true) }
            viewModelScope.launch {
                if (ensureSaved()) {
                    val requestedAt = elapsed.elapsedMs()
                    repository
                        .finishSection(attemptId, _uiState.value.section?.id)
                        .onSuccess { next ->
                            loadSequence++
                            _uiState.update { it.copy(selectedItemId = null, message = null) }
                            apply(next, requestedAt)
                        }.onFailure { error ->
                            if (!stopBy(error)) {
                                _uiState.update { it.copy(message = requestMessage(error, TryoutMessage.StatusFailed)) }
                            }
                        }
                }
                _uiState.update { it.copy(isFinishing = false) }
            }
        }

        /** Akhiri seluruh Try Out: the result, or back when it was cancelled before the first section (DEC-090). */
        private fun finishAll() {
            _uiState.update { it.copy(dialog = TryoutDialog.None) }
            viewModelScope.launch {
                if (!ensureSaved()) return@launch
                _uiState.update { it.copy(isFinishing = true, message = null) }
                repository
                    .submit(attemptId)
                    .onSuccess { next ->
                        // The reader may have seen the end first and sent it already (A2c re-review, 2).
                        if (ended) return@onSuccess
                        end()
                        _effects.send(
                            if (next.status ==
                                TryoutAttempt.STATUS_CANCELLED
                            ) {
                                TryoutRunnerEffect.Leave
                            } else {
                                TryoutRunnerEffect.OpenResult
                            },
                        )
                    }.onFailure { error ->
                        if (!stopBy(error)) {
                            _uiState.update { it.copy(message = requestMessage(error, TryoutMessage.StatusFailed)) }
                        }
                    }
                _uiState.update { it.copy(isFinishing = false) }
            }
        }

        /** Nothing more happens on this screen: the reads and the clock stop. */
        private fun end() {
            ended = true
            poller?.cancel()
            ticker?.cancel()
        }

        /**
         * 410 `cancelled` (DEC-081, DEC-118) or 403 `not_member` (T-040): the attempt cannot go on. Stop everything
         * and show the server's sentence; the student can only leave (Ardy's review of A2c, items 1 and 2).
         */
        private fun stopBy(error: Throwable?): Boolean {
            if (error !is ApiError || !PracticeRunnerViewModel.stops(error)) return false
            end()
            drafts.clear()
            _uiState.update {
                it.copy(
                    stopped = error.message,
                    message = null,
                    dialog = TryoutDialog.None,
                    hasUnsaved = false,
                    isSaving = false,
                    isFinishing = false,
                    isBeginning = false,
                )
            }
            return true
        }

        companion object {
            const val ARG_ATTEMPT = "attemptId"
            private const val POLL_MS = 15_000L
            private const val SECOND = 1_000L
            private const val COUNTDOWN_LEAD_MS = 500L
            private const val TICK_SLACK_MS = 10L
            private const val CLOSE_SLACK_MS = 250L
            private const val UNPROCESSABLE = 422
            private val PERMANENT = setOf(404, 409, 422)

            /** Refusals that end the attempt for this student: cancelled (410) or no longer a member (403). */
            private val GONE = setOf(ApiError.FORBIDDEN, 410)

            /** A 422 says why, a 403 or 410 says the server's sentence, no connection says so. */
            fun requestMessage(
                error: Throwable?,
                fallback: TryoutMessage,
            ): TryoutMessage =
                when {
                    error !is ApiError -> TryoutMessage.Offline
                    error.status == UNPROCESSABLE -> firstMessage(error)?.let(TryoutMessage::Server) ?: fallback
                    error.status in GONE && error.message.isNotBlank() -> TryoutMessage.Server(error.message)
                    else -> fallback
                }

            fun firstMessage(error: Throwable?): String? =
                (error as? ApiError)?.takeIf { it.status == UNPROCESSABLE }?.let {
                    it.fieldErrors.values
                        .firstOrNull()
                        ?.firstOrNull()
                        ?: it.message
                }
        }
    }

/** The sections still to come, plus the running one (web end dialog). */
fun TryoutRunnerUiState.remainingSections(): Int =
    attempt?.sections.orEmpty().count { it.status == TryoutSectionSummary.PENDING } + if (section != null) 1 else 0

/** Same condition as the server's cancellation (DEC-090): standalone, no subtest begun yet. */
fun TryoutRunnerUiState.endCancels(): Boolean =
    attempt?.inSession == false && section == null && attempt.sections.all { it.status == TryoutSectionSummary.PENDING }

sealed interface TryoutRunnerEvent {
    data class SelectOption(
        val index: Int,
    ) : TryoutRunnerEvent

    data class ToggleOption(
        val index: Int,
    ) : TryoutRunnerEvent

    data class StatementAnswer(
        val row: Int,
        val label: Int,
    ) : TryoutRunnerEvent

    data class GoTo(
        val itemId: Long,
    ) : TryoutRunnerEvent

    data object Previous : TryoutRunnerEvent

    data object Next : TryoutRunnerEvent

    data object ToggleFlag : TryoutRunnerEvent

    data object Begin : TryoutRunnerEvent

    data class ShowDialog(
        val dialog: TryoutDialog,
    ) : TryoutRunnerEvent

    data object ConfirmAction : TryoutRunnerEvent

    data object ConfirmEnd : TryoutRunnerEvent

    data object Leave : TryoutRunnerEvent
}
