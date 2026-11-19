package com.gotest.app.feature.ruang.ui.tasks

import androidx.compose.runtime.Immutable
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.RoomTask
import com.gotest.app.feature.ruang.domain.RoomTaskAction
import com.gotest.app.feature.ruang.domain.RoomTaskTarget
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.domain.TaskRecord
import com.gotest.app.feature.ruang.ui.components.formatGrade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a task leads; a Gotest Latihan has no screen in the app until A2c, so those are null for now. */
data class TaskNavigation(
    val onOpenQuiz: (code: String, attemptId: Long) -> Unit,
    val onOpenQuizResult: (code: String, attemptId: Long) -> Unit,
    val onOpenPractice: ((attemptId: Long) -> Unit)? = null,
    val onOpenPracticeResult: ((attemptId: Long) -> Unit)? = null,
)

enum class RecordMode { Score, Count, None }

/** The "Catat tugas selesai" form of one external task (RoomTasks.tsx RecordModal). */
@Immutable
data class RecordForm(
    val task: RoomTask,
    val mode: RecordMode,
    val score: String = "",
    val correct: String = "",
    val total: String = "",
    val note: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    /** `errors.task`, a refusal of Hapus catatan, or blank for no connection (`task_failed`). */
    val error: String? = null,
    val isSaving: Boolean = false,
    val isRemoving: Boolean = false,
) {
    val recorded: Boolean get() = task.isDone
    val busy: Boolean get() = isSaving || isRemoving

    /** "Tanpa nilai" only for a task without a question target (K-RB10). */
    val modes: List<RecordMode>
        get() =
            listOf(RecordMode.Score, RecordMode.Count) +
                if (task.targetQuestions.isNullOrZero()) {
                    listOf(RecordMode.None)
                } else {
                    emptyList()
                }

    companion object {
        private val COUNTED = Regex("""^(\d+) dari (\d+)$""")

        /** The form as the web opens it: the recorded values, or the target's count. */
        fun of(task: RoomTask): RecordForm {
            val counted = task.scoreLabel?.let { COUNTED.matchEntire(it) }
            val withoutScore = task.targetQuestions.isNullOrZero()
            val mode =
                when {
                    counted != null -> RecordMode.Count
                    task.isDone && task.score == null && withoutScore -> RecordMode.None
                    !withoutScore -> RecordMode.Count
                    else -> RecordMode.Score
                }
            return RecordForm(
                task = task,
                mode = mode,
                score = if (counted == null) task.score?.let(::formatGrade).orEmpty() else "",
                correct = counted?.groupValues?.get(1).orEmpty(),
                total =
                    counted?.groupValues?.get(2) ?: task.targetQuestions
                        ?.takeIf { it > 0 }
                        ?.toString()
                        .orEmpty(),
                note = task.note.orEmpty(),
            )
        }
    }
}

private fun Int?.isNullOrZero(): Boolean = this == null || this == 0

@Immutable
data class TaskActionState(
    /** The task whose Mulai or Lanjutkan is on its way ("Membuka..."). */
    val startingId: Long? = null,
    /** A refusal under a task card (RoomTasks.tsx `error`); blank means `task_failed`. */
    val errors: Map<Long, String> = emptyMap(),
    val record: RecordForm? = null,
)

sealed interface TaskEffect {
    data class OpenQuiz(
        val attemptId: Long,
    ) : TaskEffect

    data class OpenQuizResult(
        val attemptId: Long,
    ) : TaskEffect

    data class OpenPractice(
        val attemptId: Long,
    ) : TaskEffect

    data class OpenPracticeResult(
        val attemptId: Long,
    ) : TaskEffect
}

sealed interface TaskEvent {
    data class Act(
        val task: RoomTask,
    ) : TaskEvent

    data class ModeChange(
        val mode: RecordMode,
    ) : TaskEvent

    data class FormChange(
        val score: String? = null,
        val correct: String? = null,
        val total: String? = null,
        val note: String? = null,
    ) : TaskEvent

    data object Save : TaskEvent

    data object Remove : TaskEvent

    data object CloseRecord : TaskEvent
}

/**
 * The actions of a Rencana Belajar task, shared by Tugas hari ini (Ruang) and the plan page (T-031 "`target` di
 * aksi tugas"): start or continue a quiz or a Gotest Latihan, see a result, record or un-record an external task.
 * After a record changes, [onChanged] reloads the page, as Inertia reloads props.
 */
class TaskActions(
    private val scope: CoroutineScope,
    private val code: String,
    private val repository: StudyPlanRepository,
    private val local: ExamLocalStore,
    private val onChanged: () -> Unit,
) {
    private val _state = MutableStateFlow(TaskActionState())
    val state: StateFlow<TaskActionState> = _state.asStateFlow()

    private val _effects = Channel<TaskEffect>(Channel.BUFFERED)
    val effects: Flow<TaskEffect> = _effects.receiveAsFlow()

    fun onEvent(event: TaskEvent) {
        when (event) {
            is TaskEvent.Act -> act(event.task)
            is TaskEvent.ModeChange -> updateForm { it.copy(mode = event.mode) }
            is TaskEvent.FormChange -> updateForm { it.changed(event) }
            TaskEvent.Save -> save()
            TaskEvent.Remove -> remove()
            TaskEvent.CloseRecord -> if (_state.value.record?.busy != true) _state.update { it.copy(record = null) }
        }
    }

    private fun act(task: RoomTask) {
        val action = task.action ?: return
        when (action.kind) {
            RoomTaskAction.START, RoomTaskAction.CONTINUE -> start(task)
            RoomTaskAction.RECORD -> _state.update { it.copy(record = RecordForm.of(task)) }
            RoomTaskAction.RESULT -> openResult(action.target)
        }
    }

    private fun openResult(target: RoomTaskTarget) {
        val effect =
            when (target.type) {
                RoomTaskTarget.QUIZ_RESULT -> TaskEffect.OpenQuizResult(target.id)
                RoomTaskTarget.PRACTICE_RESULT -> TaskEffect.OpenPracticeResult(target.id)
                else -> return
            }
        _effects.trySend(effect)
    }

    private fun start(task: RoomTask) {
        if (_state.value.startingId != null) return
        _state.update { it.copy(startingId = task.id, errors = it.errors - task.id) }
        scope.launch {
            repository
                .start(code, task.id)
                .onSuccess { started ->
                    _state.update { it.copy(startingId = null) }
                    if (started.isQuiz) {
                        // A running quiz answers without a token: the work screen offers to take it over.
                        started.deviceToken?.let { local.saveDeviceToken(started.attemptId, it) }
                        _effects.send(TaskEffect.OpenQuiz(started.attemptId))
                    } else {
                        _effects.send(TaskEffect.OpenPractice(started.attemptId))
                    }
                }.onFailure { error ->
                    _state.update { it.copy(startingId = null, errors = it.errors + (task.id to firstError(error))) }
                }
        }
    }

    private fun save() {
        val form = _state.value.record ?: return
        if (form.busy) return
        val record =
            TaskRecord(
                score = form.score.takeIf { form.mode == RecordMode.Score },
                correct = form.correct.takeIf { form.mode == RecordMode.Count },
                total = form.total.takeIf { form.mode == RecordMode.Count },
                note = form.note,
            )
        updateForm { it.copy(isSaving = true, fieldErrors = emptyMap(), error = null) }
        scope.launch {
            repository
                .record(code, form.task.id, record)
                .onSuccess {
                    // The task shows itself done after the reload; no message (ALUR-KERJA 11).
                    _state.update { it.copy(record = null) }
                    onChanged()
                }.onFailure { error ->
                    updateForm { it.copy(isSaving = false, fieldErrors = fieldErrors(error), error = taskError(error)) }
                }
        }
    }

    private fun remove() {
        val form = _state.value.record ?: return
        if (form.busy) return
        updateForm { it.copy(isRemoving = true, error = null) }
        scope.launch {
            repository
                .unrecord(code, form.task.id)
                .onSuccess {
                    _state.update { it.copy(record = null) }
                    onChanged()
                }.onFailure { error -> updateForm { it.copy(isRemoving = false, error = firstError(error)) } }
        }
    }

    private fun updateForm(change: (RecordForm) -> RecordForm) =
        _state.update { state -> state.copy(record = state.record?.let(change)) }

    private companion object {
        val FIELDS = setOf("score", "correct", "total", "note")

        /** The web's `firstError`: the first message the server sent; blank shows `task_failed`. */
        fun firstError(error: Throwable): String =
            (error as? ApiError)?.let {
                it.fieldErrors.values
                    .firstOrNull()
                    ?.firstOrNull() ?: it.message
            } ?: FAILED

        fun fieldErrors(error: Throwable): Map<String, String> =
            (error as? ApiError)
                ?.fieldErrors
                ?.filterKeys { it in FIELDS }
                ?.mapValues { it.value.first() }
                .orEmpty()

        /** `errors.task`, or a refusal without a form field, or no connection. */
        fun taskError(error: Throwable): String? {
            val api = error as? ApiError ?: return FAILED
            return api.fieldError("task") ?: api.message.takeIf { api.fieldErrors.keys.none { it in FIELDS } }
        }

        /** No server message (no connection): the screen writes `task_failed`. */
        const val FAILED = ""
    }
}

private fun RecordForm.changed(event: TaskEvent.FormChange) =
    copy(
        score = event.score ?: score,
        correct = event.correct ?: correct,
        total = event.total ?: total,
        note = event.note ?: note,
    )

/** Opens what a task effect asks for; a Gotest Latihan does nothing until its screen exists (A2c). */
fun TaskNavigation.handle(
    code: String,
    effect: TaskEffect,
) {
    when (effect) {
        is TaskEffect.OpenQuiz -> onOpenQuiz(code, effect.attemptId)
        is TaskEffect.OpenQuizResult -> onOpenQuizResult(code, effect.attemptId)
        is TaskEffect.OpenPractice -> onOpenPractice?.invoke(effect.attemptId)
        is TaskEffect.OpenPracticeResult -> onOpenPracticeResult?.invoke(effect.attemptId)
    }
}
