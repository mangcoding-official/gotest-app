package com.gotest.app.feature.ruang

import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.RoomTask
import com.gotest.app.feature.ruang.domain.RoomTaskAction
import com.gotest.app.feature.ruang.domain.RoomTaskTarget
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.domain.TaskRecord
import com.gotest.app.feature.ruang.domain.TaskStart
import com.gotest.app.feature.ruang.ui.tasks.RecordForm
import com.gotest.app.feature.ruang.ui.tasks.RecordMode
import com.gotest.app.feature.ruang.ui.tasks.TaskActions
import com.gotest.app.feature.ruang.ui.tasks.TaskEffect
import com.gotest.app.feature.ruang.ui.tasks.TaskEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Rencana Belajar task actions (T-031 "`target` di aksi tugas", RoomTasks.tsx). */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskActionsTest {
    private class ScriptedPlans : StudyPlanRepository {
        var startReply: Result<TaskStart> = Result.failure(IOException())
        var recordReply: Result<String?> = Result.success("Tugas dicatat selesai.")
        val records = mutableListOf<TaskRecord>()

        override suspend fun plan(code: String): Result<StudyPlanPage> = Result.failure(IOException())

        override suspend fun start(
            code: String,
            taskId: Long,
        ): Result<TaskStart> = startReply

        override suspend fun record(
            code: String,
            taskId: Long,
            record: TaskRecord,
        ): Result<String?> {
            records += record
            return recordReply
        }

        override suspend fun unrecord(
            code: String,
            taskId: Long,
        ): Result<String?> = Result.success("Catatan tugas dihapus.")
    }

    private val plans = ScriptedPlans()
    private val local = testLocalStore()

    private fun task(
        type: String,
        kind: String,
        target: Int? = null,
        scoreLabel: String? = null,
        score: Double? = null,
        status: String = "not_started",
    ) = RoomTask(
        id = 7,
        planId = 1,
        title = "Tugas",
        type = type,
        targetQuestions = target,
        workOn = "2026-11-19",
        dueOn = "2026-11-20",
        status = status,
        score = score,
        scoreLabel = scoreLabel,
        action = RoomTaskAction(kind, target = RoomTaskTarget("study_task_start", 7)),
    )

    @Test
    fun aNewQuizKeepsItsDeviceTokenAndOpensTheQuiz() =
        runTest {
            var reloads = 0
            val actions = TaskActions(backgroundScope, "SMP1BTG", plans, local) { reloads++ }
            val effects = mutableListOf<TaskEffect>()
            backgroundScope.launch { actions.effects.collect { effects += it } }
            plans.startReply = Result.success(TaskStart(RoomTask.TYPE_QUIZ, 101, "quiz-token", "in_progress"))

            actions.onEvent(TaskEvent.Act(task(RoomTask.TYPE_QUIZ, RoomTaskAction.START)))
            runCurrent()

            assertEquals("quiz-token", local.deviceToken(101))
            assertEquals(listOf<TaskEffect>(TaskEffect.OpenQuiz(101)), effects)
            assertNull(actions.state.value.startingId)
            assertEquals(0, reloads)
        }

    @Test
    fun aRefusedStartIsShownUnderItsTask() =
        runTest {
            val actions = TaskActions(backgroundScope, "SMP1BTG", plans, local) {}
            val refusal = "Tugas ini bisa dikerjakan mulai 30 November 2026."
            plans.startReply = Result.failure(ApiError(422, refusal, "validation", mapOf("task" to listOf(refusal))))

            actions.onEvent(TaskEvent.Act(task(RoomTask.TYPE_QUIZ, RoomTaskAction.START)))
            runCurrent()

            assertEquals(refusal, actions.state.value.errors[7])
        }

    @Test
    fun theRecordFormOpensAsTheWebDoes() {
        // A task with a target opens on "Benar … dari …" with the target as total; without one, a score.
        val counted = RecordForm.of(task(RoomTask.TYPE_EXTERNAL, RoomTaskAction.RECORD, target = 10))
        assertEquals(RecordMode.Count, counted.mode)
        assertEquals("10", counted.total)
        assertEquals(listOf(RecordMode.Score, RecordMode.Count), counted.modes)

        val free = RecordForm.of(task(RoomTask.TYPE_EXTERNAL, RoomTaskAction.RECORD))
        assertEquals(RecordMode.Score, free.mode)
        assertEquals(listOf(RecordMode.Score, RecordMode.Count, RecordMode.None), free.modes)

        val recorded =
            RecordForm.of(
                task(
                    RoomTask.TYPE_EXTERNAL,
                    RoomTaskAction.RECORD,
                    10,
                    scoreLabel = "8 dari 10",
                    score = 80.0,
                    status = "done",
                ),
            )
        assertEquals(RecordMode.Count, recorded.mode)
        assertEquals("8", recorded.correct)
        assertTrue(recorded.recorded)
    }

    @Test
    fun savingSendsOnlyTheChosenWayAndReloadsTheList() =
        runTest {
            var reloads = 0
            val actions = TaskActions(backgroundScope, "SMP1BTG", plans, local) { reloads++ }
            actions.onEvent(TaskEvent.Act(task(RoomTask.TYPE_EXTERNAL, RoomTaskAction.RECORD, target = 10)))
            actions.onEvent(TaskEvent.FormChange(correct = "8", score = "90"))
            actions.onEvent(TaskEvent.Save)
            runCurrent()

            assertEquals(TaskRecord(score = null, correct = "8", total = "10", note = ""), plans.records.single())
            assertNull(actions.state.value.record)
            assertEquals(1, reloads)
        }

    @Test
    fun aFieldRefusalStaysInTheForm() =
        runTest {
            val actions = TaskActions(backgroundScope, "SMP1BTG", plans, local) {}
            val message = "Jumlah benar tidak boleh lebih dari jumlah soal."
            plans.recordReply =
                Result.failure(ApiError(422, message, "validation", mapOf("correct" to listOf(message))))
            actions.onEvent(TaskEvent.Act(task(RoomTask.TYPE_EXTERNAL, RoomTaskAction.RECORD, target = 10)))
            actions.onEvent(TaskEvent.FormChange(correct = "12"))
            actions.onEvent(TaskEvent.Save)
            runCurrent()

            val form = actions.state.value.record
            assertEquals(message, form?.fieldErrors?.get("correct"))
            assertNull(form?.error)
        }
}
