package com.gotest.app.feature.ruang.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Rencana Belajar for the child (T-031; API-CONTRACTS "API Android Rencana Belajar sisi anak dan siswa").
 * Shapes are the web's props (StudyRoomData::today and ::planPage, lib/studyPlan.ts), camelCase; only the
 * mulai answer is snake_case like the Ujian one.
 */

/** Tugas hari ini in `GET /ruang/{code}`: the main plan's tasks for today; [plan] is null without a plan. */
@Serializable
data class TodayTasks(
    @SerialName("plan") val plan: PlanHead? = null,
    @SerialName("tasks") val tasks: List<RoomTask> = emptyList(),
)

@Serializable
data class PlanHead(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
)

/** One task for the child, with the one thing the child can do with it now. */
@Serializable
data class RoomTask(
    @SerialName("id") val id: Long,
    @SerialName("planId") val planId: Long,
    @SerialName("title") val title: String,
    /** `family_quiz`, `external`, `gotest_practice`. */
    @SerialName("type") val type: String,
    @SerialName("subjectText") val subjectText: String? = null,
    @SerialName("topicName") val topicName: String? = null,
    @SerialName("targetQuestions") val targetQuestions: Int? = null,
    @SerialName("minScore") val minScore: Double = 0.0,
    /** Plain calendar dates (Y-m-d). */
    @SerialName("workOn") val workOn: String,
    @SerialName("dueOn") val dueOn: String,
    /** `not_started`, `in_progress`, `done`, `late`. */
    @SerialName("status") val status: String,
    @SerialName("belowTarget") val belowTarget: Boolean = false,
    @SerialName("score") val score: Double? = null,
    /** "8 dari 10" for an external task recorded that way. */
    @SerialName("scoreLabel") val scoreLabel: String? = null,
    /** `child` or `staff`. */
    @SerialName("recordedBy") val recordedBy: String? = null,
    @SerialName("note") val note: String? = null,
    /** Null when the task cannot be worked on yet; [blockedReason] says why. */
    @SerialName("action") val action: RoomTaskAction? = null,
    @SerialName("blockedReason") val blockedReason: String? = null,
) {
    val isDone: Boolean get() = status == STATUS_DONE

    companion object {
        const val TYPE_QUIZ = "family_quiz"
        const val TYPE_EXTERNAL = "external"
        const val TYPE_PRACTICE = "gotest_practice"
        const val STATUS_DONE = "done"
    }
}

/** `kind` and `url` are the web's; [target] is what the app does (T-031 "`target` di aksi tugas"). */
@Serializable
data class RoomTaskAction(
    /** `start`, `continue`, `result`, `record`. */
    @SerialName("kind") val kind: String,
    @SerialName("url") val url: String? = null,
    @SerialName("target") val target: RoomTaskTarget,
) {
    companion object {
        const val START = "start"
        const val CONTINUE = "continue"
        const val RESULT = "result"
        const val RECORD = "record"
    }
}

@Serializable
data class RoomTaskTarget(
    /** `study_task_start`, `study_task_record`, `quiz_result`, `practice_result`. */
    @SerialName("type") val type: String,
    @SerialName("id") val id: Long,
) {
    companion object {
        const val QUIZ_RESULT = "quiz_result"
        const val PRACTICE_RESULT = "practice_result"
    }
}

/** `GET /ruang/{code}/rencana` (web Ruang/Plan). */
@Serializable
data class StudyPlanPage(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("former") val former: Boolean = false,
    @SerialName("primaryPlanId") val primaryPlanId: Long? = null,
    /** The main plan first. */
    @SerialName("plans") val plans: List<RoomPlan> = emptyList(),
    /** The child's progress in the main plan (or the plan of the latest result); null without a plan. */
    @SerialName("planProgress") val planProgress: PlanProgress? = null,
)

@Serializable
data class RoomPlan(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("examTarget") val examTarget: String? = null,
    @SerialName("startsOn") val startsOn: String,
    @SerialName("examDate") val examDate: String,
    /** `upcoming`, `running`, `finished`, `archived`. */
    @SerialName("state") val state: String,
    @SerialName("isPrimary") val isPrimary: Boolean = false,
    @SerialName("weekCount") val weekCount: Int,
    @SerialName("currentWeek") val currentWeek: Int? = null,
    @SerialName("weeks") val weeks: List<RoomWeek> = emptyList(),
) {
    companion object {
        const val STATE_RUNNING = "running"
    }
}

@Serializable
data class RoomWeek(
    @SerialName("number") val number: Int,
    @SerialName("startsOn") val startsOn: String,
    @SerialName("endsOn") val endsOn: String,
    @SerialName("isCurrent") val isCurrent: Boolean = false,
    @SerialName("tasks") val tasks: List<RoomTask> = emptyList(),
)

/** The child's own progress in one plan (preflight Rencana Belajar 7.2–7.5, 7.15), without the suggestion. */
@Serializable
data class PlanProgress(
    @SerialName("plan") val plan: PlanProgressPlan,
    @SerialName("completion") val completion: PlanCompletion,
    @SerialName("week") val week: PlanWeekProgress,
    @SerialName("averageScore") val averageScore: Double? = null,
    @SerialName("streakDays") val streakDays: Int = 0,
    @SerialName("topics") val topics: List<PlanProgressTopic> = emptyList(),
    @SerialName("history") val history: List<PlanProgressHistory> = emptyList(),
)

@Serializable
data class PlanProgressPlan(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("startsOn") val startsOn: String,
    @SerialName("examDate") val examDate: String,
    @SerialName("state") val state: String,
)

@Serializable
data class PlanCompletion(
    @SerialName("done") val done: Int = 0,
    @SerialName("counted") val counted: Int = 0,
    @SerialName("percent") val percent: Int? = null,
)

@Serializable
data class PlanWeekProgress(
    @SerialName("number") val number: Int? = null,
    @SerialName("startsOn") val startsOn: String? = null,
    @SerialName("endsOn") val endsOn: String? = null,
    @SerialName("done") val done: Int = 0,
    @SerialName("counted") val counted: Int = 0,
    /** `on_track`, `slipping`, `behind`, `nothing_due`, `upcoming`. */
    @SerialName("status") val status: String,
)

@Serializable
data class PlanProgressTopic(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String,
    /** `green`, `yellow`, `red`, `gray`. */
    @SerialName("color") val color: String,
    @SerialName("average") val average: Double = 0.0,
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("taskCount") val taskCount: Int = 0,
)

@Serializable
data class PlanProgressHistory(
    @SerialName("taskId") val taskId: Long,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String,
    /** "Gotest", "Buatan orang tua" or "Buatan guru", "Dicatat manual". */
    @SerialName("sourceLabel") val sourceLabel: String,
    @SerialName("topicName") val topicName: String? = null,
    @SerialName("workOn") val workOn: String,
    @SerialName("dueOn") val dueOn: String,
    @SerialName("score") val score: Double? = null,
    @SerialName("scoreLabel") val scoreLabel: String? = null,
    @SerialName("status") val status: String,
    @SerialName("belowTarget") val belowTarget: Boolean = false,
    /** An instant (ISO 8601). */
    @SerialName("completedAt") val completedAt: String? = null,
    @SerialName("recordedBy") val recordedBy: String? = null,
    @SerialName("deleted") val deleted: Boolean = false,
)

/** `POST /ruang/{code}/tugas/{task}/mulai`. A quiz brings a device token (null when it was already running). */
@Serializable
data class TaskStart(
    /** `family_quiz` or `gotest_practice`. */
    @SerialName("kind") val kind: String,
    @SerialName("attempt_id") val attemptId: Long,
    @SerialName("device_token") val deviceToken: String? = null,
    @SerialName("status") val status: String? = null,
) {
    val isQuiz: Boolean get() = kind == RoomTask.TYPE_QUIZ
}

/** `PUT /ruang/{code}/tugas/{task}/catatan`: a score, or correct of total, or neither; texts as typed. */
@Serializable
data class TaskRecord(
    @SerialName("score") val score: String? = null,
    @SerialName("correct") val correct: String? = null,
    @SerialName("total") val total: String? = null,
    @SerialName("note") val note: String? = null,
)

@Serializable
data class TaskMessage(
    @SerialName("message") val message: String? = null,
)
