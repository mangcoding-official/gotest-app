package com.gotest.app.feature.ruang.ui.work

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamAnswerQueue
import com.gotest.app.feature.ruang.data.ExamClock
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.data.listNumbers
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.navigation.UjianWorkDestination
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
        savedStateHandle: SavedStateHandle,
        private val repository: ExamRepository,
        private val local: ExamLocalStore,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<UjianWorkDestination>()
        val code: String = route.code
        val attemptId: Long = route.attemptId

        private val _uiState = MutableStateFlow(ExamWorkUiState())
        val uiState: StateFlow<ExamWorkUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ExamWorkEffect>(Channel.BUFFERED)
        val effects: Flow<ExamWorkEffect> = _effects.receiveAsFlow()

        private var saver: WorkSaver? = null
        private var queue: ExamAnswerQueue? = null
        private var clock: ExamClock? = null
        private var loops: Job? = null
        private var finishing = false

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

        /**
         * The student came back after [awayMs] (screen left, phone locked, or the pinned screen left).
         * Two seconds or more is a violation (Aturan 12.13); a quiz counts none.
         */
        fun onReturned(
            awayMs: Long,
            leftAtElapsed: Long,
        ) {
            if (!countsAsViolation(awayMs)) return
            val occurredAt = clock?.now(leftAtElapsed)?.toString() ?: return
            viewModelScope.launch {
                repository
                    .reportViolation(
                        attemptId,
                        local.deviceToken(attemptId),
                        ExamViolationRequest(VIOLATION_KIND, occurredAt, awayMs),
                    ).onSuccess { result ->
                        if (result.status == SUBMITTED) {
                            finish()
                        } else {
                            _uiState.update {
                                it.copy(
                                    violations = result.violations,
                                    violationWarning = result.violations,
                                )
                            }
                        }
                    }.onFailure { error ->
                        // As on the web, a report that could not be sent is not repeated (T-026 A2b-4).
                        if (error is ApiError) {
                            handleRefusal(
                                error,
                            )
                        } else {
                            _uiState.update { it.copy(connectionLost = true) }
                        }
                    }
            }
        }

        private fun load() {
            loops?.cancel()
            _uiState.update { ExamWorkUiState(blockIndex = it.blockIndex) }
            viewModelScope.launch {
                repository
                    .work(code, attemptId, local.deviceToken(attemptId))
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
            clock = ExamClock.of(page.attempt.deadlineAt, page.attempt.serverNow, SystemClock.elapsedRealtime())
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
                    }
                }
        }

        private fun tick() {
            val current = clock ?: return
            val left = current.millisLeft(SystemClock.elapsedRealtime())
            val seconds = maxOf(0L, (left + MS_PER_SECOND - 1) / MS_PER_SECOND)
            _uiState.update { it.copy(secondsLeft = seconds, timeUp = left <= 0) }
            if (left <= 0 && !finishing) finishAfterTimeUp()
        }

        /** Time is up: answers are locked, the last ones are sent for up to 2 minutes, then the result. */
        private fun finishAfterTimeUp() {
            finishing = true
            viewModelScope.launch {
                val until = SystemClock.elapsedRealtime() + GRACE_MS
                while (saver?.isEmpty == false && SystemClock.elapsedRealtime() < until) {
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
            if (state.locked) return
            val clean = ExamAnswer.clean(value)
            val now = clock?.now(SystemClock.elapsedRealtime()) ?: return
            // The screen keeps text as typed (spaces too); the queue gets the clean answer, as on the web.
            val shown = (value as? ExamAnswer.Text)?.takeIf { it.text.isNotEmpty() } ?: clean
            _uiState.update {
                it.copy(answers = if (shown == null) it.answers - itemId else it.answers + (itemId to shown))
            }
            queue?.record(itemId, clean?.toJson() ?: JsonNull, now)
            refreshUnsent()
        }

        private fun moveBlock(step: Int) {
            _uiState.update { it.copy(blockIndex = (it.blockIndex + step).coerceIn(0, maxOf(it.blocks.lastIndex, 0))) }
            viewModelScope.launch { flush(wait = false) }
        }

        private fun goToItem(itemId: Long) {
            if (_uiState.value.otherDevice) return
            val index = _uiState.value.blocks.indexOfFirst { itemId in it.itemIds }
            if (index >= 0) _uiState.update { it.copy(blockIndex = index) }
            viewModelScope.launch { flush(wait = false) }
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

        /** Two seconds away or more, in an Ujian (a quiz counts none) that can still be worked on. */
        private fun countsAsViolation(awayMs: Long): Boolean {
            val state = _uiState.value
            val exam = state.page?.isQuiz == false
            return exam && !state.locked && awayMs >= MIN_AWAY_MS
        }

        private fun applySent(result: FlushResult.Sent) {
            val reply = result.reply ?: return
            clock = ExamClock.of(reply.deadlineAt, reply.serverNow, SystemClock.elapsedRealtime())
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

        /** "Keluar ke Daftar Ujian": the queue is sent once, then the screen closes even if that failed. */
        private fun exit() {
            _uiState.update { it.copy(showExit = false) }
            viewModelScope.launch {
                flush(wait = true)
                _effects.send(ExamWorkEffect.Leave)
            }
        }

        private fun finish() {
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
            const val MIN_AWAY_MS = 2_000L
            const val SUBMITTED = "submitted"
            const val VIOLATION_KIND = "page_left"
        }
    }
