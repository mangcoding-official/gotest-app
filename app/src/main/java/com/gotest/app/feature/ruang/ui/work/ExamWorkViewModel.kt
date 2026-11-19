package com.gotest.app.feature.ruang.ui.work

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.ruang.data.ExamAnswerQueue
import com.gotest.app.feature.ruang.data.ExamClock
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.data.ViolationReporter
import com.gotest.app.feature.ruang.data.ViolationSend
import com.gotest.app.feature.ruang.data.listNumbers
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamWorkPage
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
import kotlinx.serialization.json.JsonNull
import javax.inject.Inject

sealed interface ExamWorkEvent {
    data class Answer(
        val itemId: Long,
        val answer: ExamAnswer?,
    ) : ExamWorkEvent

    data object Previous : ExamWorkEvent

    data object Next : ExamWorkEvent

    data class GoToItem(
        val itemId: Long,
    ) : ExamWorkEvent

    data object Retry : ExamWorkEvent

    data object DismissViolation : ExamWorkEvent

    data object TakeOver : ExamWorkEvent

    data object OpenSubmit : ExamWorkEvent

    data object DismissSubmit : ExamWorkEvent

    data object ConfirmSubmit : ExamWorkEvent

    data object OpenExit : ExamWorkEvent

    data object DismissExit : ExamWorkEvent

    data object ConfirmExit : ExamWorkEvent
}

/**
 * The Ujian work screen (web: Ruang/Exams/Work, preflight Ujian 6.4 and 6.6): every item is here from
 * the start, answers are saved in batches every 5 seconds and kept on the phone until the server took
 * them, the clock is the server's, leaving the screen is reported, and one device works at a time.
 * It follows Work.tsx step by step; sending answers lives in [WorkSaver].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ExamWorkViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExamRepository,
        private val local: ExamLocalStore,
        private val elapsed: ElapsedClock,
    ) : ViewModel() {
        // Read by name (the UjianWorkDestination fields), so the view model also runs in unit tests.
        val code: String = checkNotNull(savedStateHandle[ARG_CODE])
        val attemptId: Long = checkNotNull(savedStateHandle[ARG_ATTEMPT])

        /** A Rencana Belajar quiz: the same engine, read from `ruang/{code}/kuis/kerjakan` (T-031). */
        val quiz: Boolean = savedStateHandle[ARG_QUIZ] ?: false

        private val _uiState = MutableStateFlow(ExamWorkUiState(blockIndex = savedStateHandle[KEY_BLOCK] ?: 0))
        val uiState: StateFlow<ExamWorkUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ExamWorkEffect>(Channel.BUFFERED)
        val effects: Flow<ExamWorkEffect> = _effects.receiveAsFlow()

        private var saver: WorkSaver? = null
        private var reporter: ViolationReporter? = null
        private var queue: ExamAnswerQueue? = null
        private var clock: ExamClock? = null
        private var loops: Job? = null
        private var finishing = false
        private var finished = false

        /** "Keluar ke Daftar Ujian" was chosen: letting go of the pinned screen is not leaving. */
        private var exiting = false

        init {
            load()
        }

        fun onEvent(event: ExamWorkEvent) {
            when (event) {
                is ExamWorkEvent.Answer -> answer(event.itemId, event.answer)
                ExamWorkEvent.Previous -> moveBlock(-1)
                ExamWorkEvent.Next -> moveBlock(1)
                is ExamWorkEvent.GoToItem -> goToItem(event.itemId)
                ExamWorkEvent.Retry -> load()
                ExamWorkEvent.DismissViolation -> _uiState.update { it.copy(violationWarning = null) }
                ExamWorkEvent.TakeOver -> takeOver()
                ExamWorkEvent.OpenSubmit -> openSubmit()
                ExamWorkEvent.DismissSubmit -> _uiState.update { it.copy(submit = null) }
                ExamWorkEvent.ConfirmSubmit -> submit()
                ExamWorkEvent.OpenExit -> _uiState.update { it.copy(showExit = true) }
                ExamWorkEvent.DismissExit -> _uiState.update { it.copy(showExit = false) }
                ExamWorkEvent.ConfirmExit -> exit()
            }
        }

        /** Android pinning changed; the paper stays covered and answers are refused until pinned. */
        fun onPinnedChange(pinned: Boolean) {
            _uiState.update { it.copy(pinned = pinned) }
        }

        /**
         * The student left the Ujian: the app went to the background (another app, home, screen off) or
         * the pinned screen was left. The moment is kept on the phone (T-026 A2b review 2).
         */
        fun onLeave() {
            if (!guarded()) return
            clock?.now(elapsed.elapsedMs())?.let { reporter?.leave(it) }
        }

        /** The student is back: two seconds away or more is reported (Aturan 12.13); a quiz counts none. */
        fun onReturn() {
            val current = reporter
            val now = clock?.now(elapsed.elapsedMs())
            when {
                current == null || now == null -> Unit
                !guarded() -> current.cancelLeave()
                current.comeBack(now) -> viewModelScope.launch { sendViolations() }
            }
        }

        private fun guarded(): Boolean {
            val state = _uiState.value
            return state.page?.isQuiz == false && !state.otherDevice && state.stopped == null && !state.timeUp &&
                !finished && !exiting
        }

        /** Reports kept on the phone go to the server one by one (A2b-4). */
        private suspend fun sendViolations() {
            when (val sent = reporter?.send() ?: return) {
                ViolationSend.Nothing, ViolationSend.Later -> {}

                ViolationSend.Offline -> {
                    _uiState.update { it.copy(connectionLost = true) }
                }

                is ViolationSend.Refused -> {
                    handleRefusal(sent.error)
                }

                is ViolationSend.Reported -> {
                    if (sent.result.status == SUBMITTED) {
                        finish()
                    } else {
                        _uiState.update {
                            it.copy(
                                violations = sent.result.violations,
                                violationWarning = sent.result.violations,
                            )
                        }
                    }
                }
            }
        }

        private fun load() {
            loops?.cancel()
            // Pinning is the phone's, not the page's: Android tells only changes, so it is kept (A2b review ulang 1).
            _uiState.update { ExamWorkUiState(blockIndex = it.blockIndex, pinned = it.pinned) }
            viewModelScope.launch {
                val token = local.deviceToken(attemptId)
                (if (quiz) repository.quizWork(code, attemptId, token) else repository.work(code, attemptId, token))
                    .onSuccess(::open)
                    .onFailure { error ->
                        if (error is ApiError && handleRefusal(error)) return@onFailure
                        val problem =
                            if (error is ApiError) WorkLoadProblem.Server(error.message) else WorkLoadProblem.Offline
                        _uiState.update { it.copy(isLoading = false, loadProblem = problem) }
                    }
            }
        }

        private fun open(page: ExamWorkPage) {
            val items =
                page.items.associate { item -> item.id to WorkItem(item, ExamContent.of(item.type, item.content)) }
            val answerQueue = ExamAnswerQueue(attemptId, local, page.attempt.lastClientSeq)
            val numbers = items.mapValues { it.value.item.number }
            queue = answerQueue
            saver = WorkSaver(attemptId, repository, answerQueue, { local.deviceToken(attemptId) }, numbers::get)
            reporter = ViolationReporter(attemptId, repository, local) { local.deviceToken(attemptId) }
            clock = ExamClock.of(page.attempt.deadlineAt, page.attempt.serverNow, elapsed.elapsedMs())
            // Answers kept on this phone replace what the server has (they are newer).
            val saved =
                page.answers.mapNotNull { (id, json) ->
                    val itemId = id.toLongOrNull() ?: return@mapNotNull null
                    ExamAnswer.fromJson(items[itemId]?.item?.type.orEmpty(), json)?.let { itemId to it }
                }
            val pending =
                answerQueue.pending.mapNotNull { (itemId, entry) ->
                    itemId to ExamAnswer.fromJson(items[itemId]?.item?.type.orEmpty(), entry.answer)
                }
            val answers = (saved + pending).toMap().filterValues { it != null }.mapValues { it.value!! }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    page = page,
                    items = items,
                    answers = answers,
                    blockIndex = it.blockIndex.coerceIn(0, maxOf(page.blocks.lastIndex, 0)),
                    violations = page.attempt.violations,
                    violationLimit = page.attempt.violationLimit,
                    otherDevice = page.attempt.deviceToken == null,
                )
            }
            startLoops()
            // Time away while the app was closed counts too, and reports not sent yet go now.
            onReturn()
            if (reporter?.hasPending == true && guarded()) viewModelScope.launch { sendViolations() }
        }

        /** The 1-second clock and the 5-second save, as on the web. */
        private fun startLoops() {
            loops =
                viewModelScope.launch {
                    launch {
                        while (isActive) {
                            tick()
                            delay(TICK_MS)
                        }
                    }
                    while (isActive) {
                        delay(SAVE_EVERY_MS)
                        if (!_uiState.value.timeUp) flush(wait = false)
                        if (reporter?.hasPending == true && guarded()) sendViolations()
                    }
                }
        }

        private fun tick() {
            val current = clock ?: return
            val left = current.millisLeft(elapsed.elapsedMs())
            val seconds = maxOf(0L, (left + MS_PER_SECOND - 1) / MS_PER_SECOND)
            _uiState.update { it.copy(secondsLeft = seconds, timeUp = left <= 0) }
            if (left <= 0 && !finishing) finishAfterTimeUp()
        }

        /** Time is up: answers are locked, the last ones are sent for up to 2 minutes, then the result. */
        private fun finishAfterTimeUp() {
            finishing = true
            viewModelScope.launch {
                val until = elapsed.elapsedMs() + GRACE_MS
                while (saver?.isEmpty == false && elapsed.elapsedMs() < until) {
                    flush(wait = true)
                    if (saver?.isEmpty == false) delay(TIME_UP_RETRY_MS)
                }
                finish()
            }
        }

        private fun answer(
            itemId: Long,
            value: ExamAnswer?,
        ) {
            val state = _uiState.value
            if (state.locked || state.awaitingPin) return
            val clean = ExamAnswer.clean(value)
            val now = clock?.now(elapsed.elapsedMs()) ?: return
            // The screen keeps text as typed (spaces too); the queue gets the clean answer, as on the web.
            val shown = (value as? ExamAnswer.Text)?.takeIf { it.text.isNotEmpty() } ?: clean
            _uiState.update {
                it.copy(answers = if (shown == null) it.answers - itemId else it.answers + (itemId to shown))
            }
            queue?.record(itemId, clean?.toJson() ?: JsonNull, now)
            refreshUnsent()
        }

        private fun moveBlock(step: Int) {
            showBlock(_uiState.value.blockIndex + step)
            viewModelScope.launch { flush(wait = false) }
        }

        private fun goToItem(itemId: Long) {
            if (_uiState.value.otherDevice) return
            val index = _uiState.value.blocks.indexOfFirst { itemId in it.itemIds }
            if (index >= 0) showBlock(index)
            viewModelScope.launch { flush(wait = false) }
        }

        /** The block on screen survives the screen being recreated (T-026 A2b review). */
        private fun showBlock(index: Int) {
            val block = index.coerceIn(0, maxOf(_uiState.value.blocks.lastIndex, 0))
            savedStateHandle[KEY_BLOCK] = block
            _uiState.update { it.copy(blockIndex = block) }
        }

        /** Sends what waits; true when nothing is left. */
        private suspend fun flush(wait: Boolean): Boolean {
            val current = saver
            if (current == null || _uiState.value.otherDevice) return false
            val result = current.flush(wait)
            when (result) {
                FlushResult.Busy -> Unit
                FlushResult.Offline -> _uiState.update { it.copy(connectionLost = true) }
                is FlushResult.Refused -> if (!handleRefusal(result.error)) showSaveError(result.error)
                is FlushResult.Sent -> applySent(result)
            }
            refreshUnsent()
            return result != FlushResult.Busy && current.isEmpty
        }

        private fun applySent(result: FlushResult.Sent) {
            val reply = result.reply ?: return
            clock = ExamClock.of(reply.deadlineAt, reply.serverNow, elapsed.elapsedMs())
            val outcome = result.outcome
            val notice =
                when {
                    outcome == null -> null
                    outcome.rejectedNumbers.isNotEmpty() -> WorkNotice.Rejected(listNumbers(outcome.rejectedNumbers))
                    outcome.stale -> WorkNotice.Stale
                    else -> null
                }
            _uiState.update {
                it.copy(connectionLost = false, errorMessage = null, saveFailed = false, notice = notice ?: it.notice)
            }
            if (outcome?.submitted == true) finish()
        }

        private fun showSaveError(error: ApiError) {
            val serverMessage = error.message.takeIf { error.code != null }
            _uiState.update { it.copy(errorMessage = serverMessage, saveFailed = serverMessage == null) }
        }

        private fun refreshUnsent() {
            _uiState.update { it.copy(unsentItemIds = saver?.pendingIds.orEmpty()) }
        }

        /** A refusal that ends work on this screen or this phone (Work.tsx `handleRefusal`). */
        private fun handleRefusal(error: ApiError): Boolean =
            when (error.code) {
                "other_device" -> {
                    _uiState.update { it.copy(isLoading = false, otherDevice = true) }
                    true
                }

                "submitted", "time_up", "closed" -> {
                    finish()
                    true
                }

                "not_member", "not_open" -> {
                    _uiState.update { it.copy(isLoading = false, stopped = error.message) }
                    true
                }

                else -> {
                    false
                }
            }

        private fun takeOver() {
            _uiState.update { it.copy(isTakingOver = true, takeOverMessage = null, takeOverFailed = false) }
            viewModelScope.launch {
                repository
                    .takeOver(attemptId)
                    .onSuccess { token ->
                        local.saveDeviceToken(attemptId, token)
                        // As the web reloads the page: the latest answers and lastClientSeq come in.
                        load()
                    }.onFailure { error ->
                        val message = (error as? ApiError)?.message
                        _uiState.update {
                            it.copy(isTakingOver = false, takeOverMessage = message, takeOverFailed = message == null)
                        }
                    }
            }
        }

        private fun openSubmit() {
            val state = _uiState.value
            val unanswered = state.orderedItems.filterNot { state.isAnswered(it.item.id) }.map { it.item.number }
            _uiState.update {
                it.copy(
                    submit =
                        SubmitDialogState(
                            unanswered =
                                unanswered
                                    .takeIf { n ->
                                        n.isNotEmpty()
                                    }?.let(::listNumbers),
                        ),
                )
            }
        }

        private fun submit() {
            val dialog = _uiState.value.submit ?: return
            _uiState.update {
                it.copy(
                    submit = dialog.copy(isSubmitting = true, message = null, unsent = false, failed = false),
                )
            }
            viewModelScope.launch {
                if (!flush(wait = true)) {
                    _uiState.update { it.copy(submit = it.submit?.copy(isSubmitting = false, unsent = true)) }
                    return@launch
                }
                repository
                    .submit(attemptId, local.deviceToken(attemptId))
                    .onSuccess { finish() }
                    .onFailure { error ->
                        if (error is ApiError && handleRefusal(error)) return@onFailure
                        val message = (error as? ApiError)?.message
                        _uiState.update {
                            it.copy(
                                submit =
                                    it.submit?.copy(
                                        isSubmitting = false,
                                        message = message,
                                        failed =
                                            message == null,
                                    ),
                            )
                        }
                    }
            }
        }

        /**
         * "Keluar ke Daftar Ujian": the queue is sent once, then the screen closes even if that failed. As on
         * the web, pressing Keluar is not a violation (DEC-148), but time away before it still is: that report
         * is kept and sent first (A2b review ulang 2); what does not go now goes when the paper opens again.
         */
        private fun exit() {
            _uiState.update { it.copy(showExit = false) }
            val now = clock?.now(elapsed.elapsedMs())
            if (guarded() && now != null) reporter?.comeBack(now) else reporter?.cancelLeave()
            exiting = true
            viewModelScope.launch {
                if (reporter?.hasPending == true) sendViolations()
                if (finished) return@launch
                flush(wait = true)
                _effects.send(ExamWorkEffect.Leave)
            }
        }

        private fun finish() {
            if (finished) return
            finished = true
            loops?.cancel()
            local.forget(attemptId)
            _effects.trySend(ExamWorkEffect.OpenResult)
        }

        private companion object {
            const val SAVE_EVERY_MS = 5_000L
            const val TICK_MS = 1_000L
            const val MS_PER_SECOND = 1_000L
            const val GRACE_MS = 120_000L
            const val TIME_UP_RETRY_MS = 3_000L
            const val SUBMITTED = "submitted"
            const val ARG_CODE = "code"
            const val ARG_ATTEMPT = "attemptId"
            const val ARG_QUIZ = "quiz"
            const val KEY_BLOCK = "blockIndex"
        }
    }
