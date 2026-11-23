package com.gotest.app.feature.multiplayer.ui

import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import com.gotest.app.feature.multiplayer.domain.savedValue
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.answerPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Answering a game, as the web's PlayScreen: one save per question at a time that keeps sending until the server
 * holds the newest answer, unsaved answers sent again every 4 seconds, and finishing only after every answer on
 * its way has landed. The time running out finishes on its own.
 */
@Suppress("TooManyFunctions")
internal class GamePlay(
    private val scope: CoroutineScope,
    private val repository: MainBarengRepository,
    private val state: MutableStateFlow<MainBarengUiState>,
    /** The game after "Selesai": the result when it ended, otherwise the waiting screen. */
    private val onFinished: suspend (GameState) -> Unit,
    /** "Ya, Keluar" went through. */
    private val onLeft: () -> Unit,
) {
    private var code: String? = null
    private val latest = mutableMapOf<Long, AnswerValue?>()
    private val inFlight = mutableMapOf<Long, Deferred<Unit>>()
    private val failed = mutableSetOf<Long>()
    private var finished = false
    private var retry: Job? = null

    fun begin(
        gameCode: String,
        questions: List<GameQuestion>,
    ) {
        stop()
        code = gameCode
        finished = false
        val answers = questions.mapNotNull { question -> question.savedValue()?.let { question.id to it } }.toMap()
        state.update { it.copy(play = PlayState(questions = questions, answers = answers)) }
    }

    fun stop() {
        retry?.cancel()
        retry = null
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        latest.clear()
        failed.clear()
        code = null
    }

    private fun current(): PlayState? = state.value.play

    private fun updatePlay(change: (PlayState) -> PlayState) {
        state.update { ui -> ui.play?.let { ui.copy(play = change(it)) } ?: ui }
    }

    fun goTo(index: Int) {
        val play = current() ?: return
        if (play.finishing) return
        updatePlay { it.copy(index = index.coerceIn(0, (it.questions.size - 1).coerceAtLeast(0))) }
    }

    fun showModal(modal: PlayModal) = updatePlay { it.copy(modal = modal) }

    fun select(option: Int) = record(AnswerValue.Single(option))

    fun toggle(option: Int) {
        val play = current() ?: return
        val question = play.question ?: return
        val chosen = (play.answers[question.id] as? AnswerValue.Multiple)?.options.orEmpty()
        val next = if (option in chosen) chosen - option else (chosen + option).sorted()
        record(next.takeIf { it.isNotEmpty() }?.let(AnswerValue::Multiple))
    }

    fun statement(
        row: Int,
        label: Int,
    ) {
        val play = current() ?: return
        val question = play.question ?: return
        val rows = question.content.statements.size
        val now = (play.answers[question.id] as? AnswerValue.Statement)?.labels.orEmpty()
        record(AnswerValue.Statement(List(rows) { index -> if (index == row) label else now.getOrNull(index) }))
    }

    private fun record(value: AnswerValue?) {
        val play = current()
        val question = play?.question?.takeIf { !state.value.timeUp && !play.finishing } ?: return
        updatePlay {
            it.copy(answers = if (value == null) it.answers - question.id else it.answers + (question.id to value))
        }
        latest[question.id] = value
        send(question)
    }

    /** One save per question at a time, always the newest answer (web `send`). */
    private fun send(question: GameQuestion): Deferred<Unit> =
        inFlight[question.id] ?: scope
            .async(start = CoroutineStart.LAZY) { deliver(question) }
            .also { task ->
                // Registered before it runs, so a save that finishes at once still clears its own entry.
                inFlight[question.id] = task
                task.start()
            }

    private suspend fun deliver(question: GameQuestion) {
        val ok = code?.let { pushNewest(it, question) } ?: false
        if (ok) failed.remove(question.id) else failed += question.id
        updatePlay { it.copy(unsent = if (ok) it.unsent - question.id else it.unsent + question.id) }
        inFlight.remove(question.id)
        if (!ok) keepRetrying()
    }

    /** Keeps sending until the server holds the newest answer; false once a save fails. */
    private suspend fun pushNewest(
        gameCode: String,
        question: GameQuestion,
    ): Boolean {
        var value: AnswerValue?
        do {
            value = latest[question.id]
            // An answer taken back goes as an empty one, as the web sends `value ?? []`.
            val payload = answerPayload(question.questionType, value ?: AnswerValue.Multiple(emptyList()))
            if (repository.answer(gameCode, question.id, payload).isFailure) return false
        } while (latest[question.id] != value)
        return true
    }

    /** Unsaved answers go again on their own once the connection returns. */
    private fun keepRetrying() {
        if (retry?.isActive == true) return
        retry =
            scope.launch {
                while (isActive && failed.isNotEmpty() && current()?.finishing == false) {
                    delay(RETRY_MS)
                    failed.toList().forEach { id -> current()?.questions?.firstOrNull { it.id == id }?.let(::send) }
                }
            }
    }

    /** "Ya, Selesai", or the time ran out: every answer on its way lands first. */
    fun finish() {
        if (finished || current() == null) return
        val gameCode = code ?: return
        finished = true
        updatePlay { it.copy(finishing = true, modal = PlayModal.None, error = null) }
        scope.launch {
            inFlight.values.toList().forEach { runCatching { it.await() } }
            val questions = current()?.questions.orEmpty()
            failed.toList().mapNotNull { id -> questions.firstOrNull { it.id == id } }.map(::send).forEach {
                runCatching { it.await() }
            }
            repository
                .finish(gameCode)
                .onSuccess { next -> onFinished(next) }
                .onFailure {
                    finished = false
                    updatePlay { it.copy(finishing = false, error = PlayError.FinishFailed) }
                }
        }
    }

    fun leave() {
        val gameCode = code ?: return
        updatePlay { it.copy(modal = PlayModal.None, error = null) }
        scope.launch {
            repository
                .leave(gameCode)
                .onSuccess {
                    stop()
                    onLeft()
                }.onFailure { updatePlay { it.copy(error = PlayError.LeaveFailed) } }
        }
    }

    private companion object {
        const val RETRY_MS = 4_000L
    }
}
