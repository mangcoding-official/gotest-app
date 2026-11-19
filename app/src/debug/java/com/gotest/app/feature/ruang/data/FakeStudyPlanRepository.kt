package com.gotest.app.feature.ruang.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.PlanCompletion
import com.gotest.app.feature.ruang.domain.PlanHead
import com.gotest.app.feature.ruang.domain.PlanProgress
import com.gotest.app.feature.ruang.domain.PlanProgressHistory
import com.gotest.app.feature.ruang.domain.PlanProgressPlan
import com.gotest.app.feature.ruang.domain.PlanProgressTopic
import com.gotest.app.feature.ruang.domain.PlanWeekProgress
import com.gotest.app.feature.ruang.domain.RoomPlan
import com.gotest.app.feature.ruang.domain.RoomTask
import com.gotest.app.feature.ruang.domain.RoomTaskAction
import com.gotest.app.feature.ruang.domain.RoomTaskTarget
import com.gotest.app.feature.ruang.domain.RoomWeek
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.domain.TaskRecord
import com.gotest.app.feature.ruang.domain.TaskStart
import com.gotest.app.feature.ruang.domain.TodayTasks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** One task of the debug plans; what the child did is kept here, quizzes in [FakeExamRepository]. */
internal data class FakeTask(
    val id: Long,
    val planId: Long,
    val week: Int,
    val title: String,
    val type: String,
    val topic: String?,
    val target: Int?,
    val minScore: Double,
    val workOn: LocalDate,
    val dueOn: LocalDate,
    var score: Double? = null,
    var scoreLabel: String? = null,
    var recordedBy: String? = null,
    var note: String? = null,
    var done: Boolean = false,
    var started: Boolean = false,
    var resultId: Long? = null,
)

/**
 * Debug data for the child's Rencana Belajar (T-031; debug builds only, T-026 criterion 8). In SMP 1 Bantargedung
 * Gilang has "Persiapan PTS Semester 1" (main plan, week 2 of 4) and "Persiapan Ujian Akhir" (not started yet).
 * Today: a quiz (six items of the Ujian paper), an external task with a 10-question target, and a video without
 * one; a Gotest Latihan is due tomorrow. Bimbel Cerdas has no plan. Texts are the server's (StudyRoomData,
 * StudyChildAccess, StudyTaskRecords, StudyTaskStarts).
 */
@Singleton
class FakeStudyPlanRepository
    @Inject
    constructor(
        private val exams: FakeExamRepository,
    ) : StudyPlanRepository {
        private val today: LocalDate = LocalDate.now()
        private val start: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
        private val tasks = fakeTasks(today, start)

        override suspend fun plan(code: String): Result<StudyPlanPage> {
            simulateLatency()
            val mitra = mitraOf(code) ?: return notFound()
            if (code != FakeRuangRepository.SMP) return Result.success(StudyPlanPage(mitra, former = true))
            return Result.success(
                StudyPlanPage(
                    organization = mitra,
                    primaryPlanId = MAIN_PLAN,
                    plans = listOf(mainPlan(), laterPlan()),
                    planProgress = progress(),
                ),
            )
        }

        override suspend fun start(
            code: String,
            taskId: Long,
        ): Result<TaskStart> {
            simulateLatency()
            val task = tasks.firstOrNull { it.id == taskId } ?: return notFound()
            blocker(task)?.let { return refuse(it) }
            return when (task.type) {
                RoomTask.TYPE_QUIZ -> {
                    exams.startQuiz(task.id)
                }

                RoomTask.TYPE_PRACTICE -> {
                    task.started = true
                    Result.success(TaskStart(RoomTask.TYPE_PRACTICE, PRACTICE_ATTEMPTS + task.id))
                }

                else -> {
                    refuse("Tugas luar dicatat, bukan dikerjakan di sini. Isi nilainya setelah selesai.")
                }
            }
        }

        override suspend fun record(
            code: String,
            taskId: Long,
            record: TaskRecord,
        ): Result<String?> {
            simulateLatency()
            val task =
                tasks.firstOrNull { it.id == taskId && it.type == RoomTask.TYPE_EXTERNAL } ?: return notFound()
            blocker(task)?.let { return refuse(it) }
            val checked = checkRecord(task, record).getOrElse { return Result.failure(it) }
            task.score = checked.first
            task.scoreLabel = checked.second
            task.note = record.note?.trim()?.takeIf { it.isNotEmpty() }
            task.recordedBy = RECORDED_BY_CHILD
            task.done = true
            return Result.success("Tugas dicatat selesai.")
        }

        override suspend fun unrecord(
            code: String,
            taskId: Long,
        ): Result<String?> {
            simulateLatency()
            val task =
                tasks.firstOrNull { it.id == taskId && it.type == RoomTask.TYPE_EXTERNAL } ?: return notFound()
            task.score = null
            task.scoreLabel = null
            task.note = null
            task.recordedBy = null
            task.done = false
            return Result.success("Catatan tugas dihapus.")
        }

        /** Tugas hari ini of `GET /ruang/{code}`: today's tasks, earlier ones not done, then the late ones. */
        internal fun today(code: String): TodayTasks {
            if (code != FakeRuangRepository.SMP) return TodayTasks()
            val shown =
                tasks
                    .filter { it.planId == MAIN_PLAN && !it.workOn.isAfter(today) }
                    .filter { it.workOn == today || status(it) != STATUS_DONE }
                    .sortedWith(compareBy({ group(it) }, { it.workOn }, { it.id }))
            return TodayTasks(PlanHead(MAIN_PLAN, MAIN_TITLE), shown.map(::row))
        }

        internal fun progressOf(code: String): PlanProgress? = progress().takeIf { code == FakeRuangRepository.SMP }

        private fun group(task: FakeTask): Int =
            when {
                task.workOn == today -> 0
                status(task) != STATUS_LATE -> 1
                else -> 2
            }

        private fun mainPlan(): RoomPlan =
            RoomPlan(
                id = MAIN_PLAN,
                title = MAIN_TITLE,
                examTarget = "PTS Semester 1",
                startsOn = start.toString(),
                examDate = start.plusWeeks(MAIN_WEEKS.toLong()).minusDays(1).toString(),
                state = RoomPlan.STATE_RUNNING,
                isPrimary = true,
                weekCount = MAIN_WEEKS,
                currentWeek = CURRENT_WEEK,
                weeks =
                    (1..MAIN_WEEKS).map { number ->
                        val weekStart = start.plusWeeks(number - 1L)
                        RoomWeek(
                            number = number,
                            startsOn = weekStart.toString(),
                            endsOn = weekStart.plusDays(DAYS_IN_WEEK).toString(),
                            isCurrent = number == CURRENT_WEEK,
                            tasks = tasks.filter { it.planId == MAIN_PLAN && it.week == number }.map(::row),
                        )
                    },
            )

        private fun laterPlan(): RoomPlan {
            val laterStart = start.plusWeeks(LATER_OFFSET_WEEKS)
            return RoomPlan(
                id = LATER_PLAN,
                title = "Persiapan Ujian Akhir",
                startsOn = laterStart.toString(),
                examDate = laterStart.plusWeeks(2).minusDays(1).toString(),
                state = "upcoming",
                weekCount = 2,
                weeks =
                    (1..2).map { number ->
                        val weekStart = laterStart.plusWeeks(number - 1L)
                        RoomWeek(
                            number = number,
                            startsOn = weekStart.toString(),
                            endsOn = weekStart.plusDays(DAYS_IN_WEEK).toString(),
                            tasks = tasks.filter { it.planId == LATER_PLAN && it.week == number }.map(::row),
                        )
                    },
            )
        }

        private fun status(task: FakeTask): String {
            val quiz = if (task.type == RoomTask.TYPE_QUIZ) exams.quizOf(task.id) else null
            return when {
                task.done || quiz?.submitted == true -> STATUS_DONE
                quiz != null || task.started -> "in_progress"
                task.dueOn.isBefore(today) -> STATUS_LATE
                else -> "not_started"
            }
        }

        private fun scoreOf(task: FakeTask): Double? =
            if (exams.quizOf(task.id)?.submitted == true) QUIZ_SCORE else task.score

        private fun blocker(task: FakeTask): String? =
            if (task.workOn.isAfter(today)) "Tugas ini bisa dikerjakan mulai ${longDate(task.workOn)}." else null

        private fun row(task: FakeTask): RoomTask {
            val status = status(task)
            val quiz = exams.quizOf(task.id)
            val done = status == STATUS_DONE
            val blocker = blocker(task)
            val action =
                when {
                    done && task.type == RoomTask.TYPE_QUIZ -> {
                        val id = quiz?.id ?: task.resultId ?: 0
                        RoomTaskAction(RoomTaskAction.RESULT, target = RoomTaskTarget(RoomTaskTarget.QUIZ_RESULT, id))
                    }

                    done && task.type == RoomTask.TYPE_PRACTICE -> {
                        val target = RoomTaskTarget(RoomTaskTarget.PRACTICE_RESULT, task.resultId ?: 0)
                        RoomTaskAction(RoomTaskAction.RESULT, target = target)
                    }

                    blocker != null -> {
                        null
                    }

                    task.type == RoomTask.TYPE_EXTERNAL -> {
                        RoomTaskAction(RoomTaskAction.RECORD, target = RoomTaskTarget("study_task_record", task.id))
                    }

                    else -> {
                        val kind = if (status == "in_progress") RoomTaskAction.CONTINUE else RoomTaskAction.START
                        RoomTaskAction(kind, target = RoomTaskTarget("study_task_start", task.id))
                    }
                }
            val score = scoreOf(task)
            return RoomTask(
                id = task.id,
                planId = task.planId,
                title = task.title,
                type = task.type,
                topicName = task.topic,
                targetQuestions = if (task.type == RoomTask.TYPE_QUIZ) QUIZ_QUESTIONS else task.target,
                minScore = task.minScore,
                workOn = task.workOn.toString(),
                dueOn = task.dueOn.toString(),
                status = status,
                belowTarget = score != null && score < task.minScore,
                score = score,
                scoreLabel = task.scoreLabel,
                recordedBy = task.recordedBy,
                note = task.note,
                action = action,
                blockedReason = blocker.takeIf { action == null && !done },
            )
        }

        private fun progress(): PlanProgress {
            val counted = tasks.filter { it.planId == MAIN_PLAN && !it.dueOn.isAfter(today) }
            val done = counted.count { status(it) == STATUS_DONE }
            val scores = tasks.filter { it.planId == MAIN_PLAN }.mapNotNull(::scoreOf)
            val weekStart = start.plusWeeks(CURRENT_WEEK - 1L)
            val week = tasks.filter { it.planId == MAIN_PLAN && it.week == CURRENT_WEEK }
            val weekCounted = week.filter { !it.dueOn.isAfter(today) }
            val weekDone = weekCounted.count { status(it) == STATUS_DONE }
            return PlanProgress(
                plan =
                    PlanProgressPlan(
                        id = MAIN_PLAN,
                        title = MAIN_TITLE,
                        startsOn = start.toString(),
                        examDate = start.plusWeeks(MAIN_WEEKS.toLong()).minusDays(1).toString(),
                        state = RoomPlan.STATE_RUNNING,
                    ),
                completion =
                    PlanCompletion(
                        done = done,
                        counted = counted.size,
                        percent = if (counted.isEmpty()) null else done * PERCENT / counted.size,
                    ),
                week =
                    PlanWeekProgress(
                        number = CURRENT_WEEK,
                        startsOn = weekStart.toString(),
                        endsOn = weekStart.plusDays(DAYS_IN_WEEK).toString(),
                        done = weekDone,
                        counted = weekCounted.size,
                        status =
                            when {
                                weekCounted.isEmpty() -> "nothing_due"
                                weekDone == weekCounted.size -> "on_track"
                                weekDone * 2 >= weekCounted.size -> "slipping"
                                else -> "behind"
                            },
                    ),
                averageScore = if (scores.isEmpty()) null else (scores.average() * TENTHS).roundToInt() / TENTHS,
                streakDays = STREAK_DAYS,
                topics =
                    listOf(
                        PlanProgressTopic(
                            "t-pecahan",
                            "Pecahan",
                            "red",
                            average = 55.0,
                            questionCount = 10,
                            taskCount = 1,
                        ),
                        PlanProgressTopic(
                            "t-bilangan",
                            "Bilangan Bulat",
                            "yellow",
                            65.0,
                            questionCount = 6,
                            taskCount = 1,
                        ),
                        PlanProgressTopic("t-aljabar", "Aljabar", "gray", 0.0, questionCount = 0, taskCount = 1),
                    ),
                history = week.map(::history),
            )
        }

        private fun history(task: FakeTask): PlanProgressHistory {
            val status = status(task)
            return PlanProgressHistory(
                taskId = task.id,
                title = task.title,
                type = task.type,
                sourceLabel =
                    when (task.type) {
                        RoomTask.TYPE_PRACTICE -> "Gotest"
                        RoomTask.TYPE_QUIZ -> "Buatan guru"
                        else -> "Dicatat manual"
                    },
                topicName = task.topic,
                workOn = task.workOn.toString(),
                dueOn = task.dueOn.toString(),
                score = scoreOf(task),
                scoreLabel = task.scoreLabel,
                status = status,
                belowTarget = scoreOf(task)?.let { it < task.minScore } ?: false,
                completedAt =
                    if (status == STATUS_DONE) {
                        java.time.Instant
                            .now()
                            .toString()
                    } else {
                        null
                    },
                recordedBy = task.recordedBy,
            )
        }

        private companion object {
            const val MAIN_PLAN = 31L
            const val LATER_PLAN = 32L
            const val MAIN_TITLE = "Persiapan PTS Semester 1"
            const val MAIN_WEEKS = 4
            const val CURRENT_WEEK = 2
            const val LATER_OFFSET_WEEKS = 6L
            const val DAYS_IN_WEEK = 6L
            const val STATUS_DONE = "done"
            const val STATUS_LATE = "late"
            const val RECORDED_BY_CHILD = "child"
            const val QUIZ_QUESTIONS = 6
            const val QUIZ_SCORE = 80.0
            const val PRACTICE_ATTEMPTS = 500L
            const val PERCENT = 100
            const val TENTHS = 10.0
            const val STREAK_DAYS = 3
            const val UNPROCESSABLE = 422
            const val NOT_FOUND = 404
            const val MAX_SCORE = 100.0
            const val MAX_COUNT = 1000
            private val WITH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

            fun longDate(date: LocalDate): String = WITH_YEAR.format(date)

            fun mitraOf(code: String): MitraRef? =
                when (code) {
                    FakeRuangRepository.SMP -> MitraRef(FakeExamRepository.SMP_NAME, code)
                    FakeRuangRepository.BIMBEL -> MitraRef("Bimbel Cerdas", code)
                    else -> null
                }

            fun <T> refuse(message: String): Result<T> =
                Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf("task" to listOf(message))))

            fun <T> fieldError(
                field: String,
                message: String,
            ): Result<T> =
                Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf(field to listOf(message))))

            fun <T> notFound(): Result<T> = Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))

            /** StudyTaskRecords: a score, or correct of total, or nothing when the task has no target. */
            @Suppress("ReturnCount", "CyclomaticComplexMethod")
            fun checkRecord(
                task: FakeTask,
                record: TaskRecord,
            ): Result<Pair<Double?, String?>> {
                val scoreText =
                    record.score
                        ?.trim()
                        ?.replace(',', '.')
                        ?.takeIf { it.isNotEmpty() }
                val correctText = record.correct?.trim()?.takeIf { it.isNotEmpty() }
                val totalText = record.total?.trim()?.takeIf { it.isNotEmpty() }
                if ((record.note?.trim()?.length ?: 0) > NOTE_MAX) {
                    return fieldError("note", "Catatan paling panjang 200 huruf.")
                }
                if (scoreText != null) {
                    val score = scoreText.toDoubleOrNull()
                    if (score == null || score < 0 || score > MAX_SCORE || !SCORE.matches(scoreText)) {
                        return fieldError("score", "Isi nilai 0 sampai 100, paling banyak satu angka di belakang koma.")
                    }
                    if (correctText != null || totalText != null) {
                        return fieldError("score", "Pilih salah satu: nilai 0–100, atau benar dari jumlah soal.")
                    }
                    return Result.success(score to null)
                }
                if (correctText != null || totalText != null) {
                    if (correctText == null || totalText == null) {
                        val field = if (correctText != null) "total" else "correct"
                        return fieldError(field, "Isi jumlah benar dan jumlah soalnya.")
                    }
                    val correct =
                        correctText.toIntOrNull()?.takeIf { it in 0..MAX_COUNT }
                            ?: return fieldError("correct", "Isi jumlah benar 0 sampai 1.000.")
                    val total =
                        totalText.toIntOrNull()?.takeIf { it in 1..MAX_COUNT }
                            ?: return fieldError("total", "Isi jumlah soal 1 sampai 1.000.")
                    if (correct >
                        total
                    ) {
                        return fieldError("correct", "Jumlah benar tidak boleh lebih dari jumlah soal.")
                    }
                    return Result.success(
                        (correct * MAX_SCORE / total * TENTHS).roundToInt() / TENTHS to "$correct dari $total",
                    )
                }
                if (task.target != null) {
                    return fieldError(
                        "score",
                        "Tugas ini punya target ${task.target} soal. Isi nilainya atau jumlah benarnya.",
                    )
                }
                return Result.success(null to null)
            }

            private const val NOTE_MAX = 200
            private val SCORE = Regex("""^\d{1,3}(\.\d)?$""")
        }
    }

private fun fakeTasks(
    today: LocalDate,
    start: LocalDate,
): List<FakeTask> {
    val week2 = start.plusWeeks(1)
    val week3 = start.plusWeeks(2)
    return listOf(
        FakeTask(
            id = 201,
            planId = 31,
            week = 1,
            title = "Kuis Bilangan Bulat",
            type = RoomTask.TYPE_QUIZ,
            topic = "Bilangan Bulat",
            target = null,
            minScore = 70.0,
            workOn = start,
            dueOn = start.plusDays(2),
            done = true,
            resultId = FakeStudyPlan.DONE_QUIZ_ATTEMPT,
            score = 65.0,
        ),
        FakeTask(
            id = 202,
            planId = 31,
            week = 1,
            title = "Rangkum bab Pecahan",
            type = RoomTask.TYPE_EXTERNAL,
            topic = "Pecahan",
            target = null,
            minScore = 70.0,
            workOn = start.plusDays(3),
            dueOn = start.plusDays(4),
        ),
        FakeTask(
            id = 211,
            planId = 31,
            week = 2,
            title = FakeStudyPlan.QUIZ_TITLE,
            type = RoomTask.TYPE_QUIZ,
            topic = "Aljabar",
            target = null,
            minScore = 70.0,
            workOn = today,
            dueOn = today.plusDays(1),
        ),
        FakeTask(
            id = 212,
            planId = 31,
            week = 2,
            title = "Kerjakan LKS halaman 12–15",
            type = RoomTask.TYPE_EXTERNAL,
            topic = "Pecahan",
            target = 10,
            minScore = 70.0,
            workOn = today,
            dueOn = today.plusDays(1),
        ),
        FakeTask(
            id = 213,
            planId = 31,
            week = 2,
            title = "Tonton video Pecahan Senilai",
            type = RoomTask.TYPE_EXTERNAL,
            topic = "Pecahan",
            target = null,
            minScore = 70.0,
            workOn = today,
            dueOn = today,
        ),
        FakeTask(
            id = 214,
            planId = 31,
            week = 2,
            title = "Latihan Pecahan",
            type = RoomTask.TYPE_PRACTICE,
            topic = "Pecahan",
            target = 10,
            minScore = 70.0,
            workOn = today.plusDays(1),
            dueOn = today.plusDays(2),
        ),
        FakeTask(
            id = 221,
            planId = 31,
            week = 3,
            title = "Kuis Pecahan",
            type = RoomTask.TYPE_QUIZ,
            topic = "Pecahan",
            target = null,
            minScore = 70.0,
            workOn = week3,
            dueOn = week3.plusDays(1),
        ),
        FakeTask(
            id = 222,
            planId = 31,
            week = 3,
            title = "Latihan Aljabar",
            type = RoomTask.TYPE_PRACTICE,
            topic = "Aljabar",
            target = 15,
            minScore = 75.0,
            workOn = week3.plusDays(2),
            dueOn = week3.plusDays(3),
        ),
        FakeTask(
            id = 231,
            planId = 32,
            week = 1,
            title = "Latihan Bilangan Bulat",
            type = RoomTask.TYPE_PRACTICE,
            topic = "Bilangan Bulat",
            target = 10,
            minScore = 70.0,
            workOn = week2.plusWeeks(5),
            dueOn = week2.plusWeeks(5).plusDays(1),
        ),
    )
}

/** Shared with [FakeExamRepository]: the quiz of today and the one already done in week 1. */
internal object FakeStudyPlan {
    const val QUIZ_TITLE = "Kuis Aljabar Dasar"
    const val DONE_QUIZ_TITLE = "Kuis Bilangan Bulat"
    const val DONE_QUIZ_ATTEMPT = 90L
}
