package com.gotest.app.feature.ruang.domain

import com.gotest.app.feature.tryout.domain.TryoutSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Ruang Mitra and Ujian pages for a student (API module G, T-029; API-CONTRACTS "API Android modul G").
 * Page bodies have the web's props shape (camelCase); keys only the API has are snake_case.
 */

@Serializable
data class MitraRef(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
)

/** One Mitra of the account, from `organizations` in `GET /akun`. */
@Serializable
data class MitraMembership(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String? = null,
    @SerialName("role") val role: String,
    @SerialName("former") val former: Boolean = false,
    /** The Ujian card shows when the plan includes Ujian or the student has a schedule or result (API-K7 B). */
    @SerialName("has_exams") val hasExams: Boolean = false,
) {
    /** Pengelola and pengajar open the Mitra portal, which the app does not have yet (A4). */
    val opensPortal: Boolean get() = role == ROLE_MANAGER || role == ROLE_TEACHER

    private companion object {
        const val ROLE_MANAGER = "pengelola"
        const val ROLE_TEACHER = "pengajar"
    }
}

/** `GET /ruang/{code}` (web Ruang/Show). */
@Serializable
data class RuangPage(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("former") val former: Boolean = false,
    /** Tugas hari ini from the main plan (Rencana Belajar). */
    @SerialName("today") val today: TodayTasks = TodayTasks(),
    /** The child's own progress in the main plan; null without a plan. */
    @SerialName("planProgress") val planProgress: PlanProgress? = null,
    /** The school's Try Out sessions this student takes part in, newest first (same shape as the catalog's). */
    @SerialName("sessions") val sessions: List<TryoutSession> = emptyList(),
    @SerialName("practice") val practice: List<RuangPractice> = emptyList(),
    @SerialName("progress") val progress: MitraProgress = MitraProgress(),
)

/** One Latihan sekolah given to the student, or worked on before (web Ruang/Show PracticeCard). */
@Serializable
data class RuangPractice(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("questionCount") val questionCount: Int = 0,
    /** `none`, `package`, or `per_question`. */
    @SerialName("timerMode") val timerMode: String = TIMER_NONE,
    @SerialName("durationSeconds") val durationSeconds: Int? = null,
    /** Closed by the Mitra: results and pembahasan stay, no new start. */
    @SerialName("closed") val closed: Boolean = false,
    @SerialName("continueAttemptId") val continueAttemptId: Long? = null,
    @SerialName("canStart") val canStart: Boolean = false,
    @SerialName("finishedCount") val finishedCount: Int = 0,
    @SerialName("lastAttemptId") val lastAttemptId: Long? = null,
    @SerialName("lastScore") val lastScore: Double? = null,
) {
    companion object {
        const val TIMER_NONE = "none"
        const val TIMER_PACKAGE = "package"
        const val TIMER_PER_QUESTION = "per_question"
    }
}

/** Perkembanganmu in a Mitra space (web Components/Mitra/ProgressView), oldest attempt first. */
@Serializable
data class MitraProgress(
    @SerialName("attempts") val attempts: List<ProgressAttempt> = emptyList(),
    @SerialName("subjects") val subjects: List<ProgressSubject> = emptyList(),
)

@Serializable
data class ProgressAttempt(
    @SerialName("id") val id: Long,
    /** `latihan` or `simulasi`. */
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("correct") val correct: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("percent") val percent: Int = 0,
    @SerialName("score") val score: Double = 0.0,
) {
    companion object {
        const val KIND_PRACTICE = "latihan"
    }
}

@Serializable
data class ProgressSubject(
    @SerialName("name") val name: String,
    @SerialName("correct") val correct: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("percent") val percent: Int = 0,
)

@Serializable
enum class ScheduleState {
    @SerialName("scheduled")
    Scheduled,

    @SerialName("running")
    Running,

    @SerialName("closed")
    Closed,
}

@Serializable
enum class MyExamStatus {
    @SerialName("not_started")
    NotStarted,

    @SerialName("in_progress")
    InProgress,

    @SerialName("submitted")
    Submitted,
}

/** `GET /ruang/{code}/ujian`. */
@Serializable
data class ExamListPage(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("schedules") val schedules: List<ExamSchedule> = emptyList(),
)

@Serializable
data class ExamSchedule(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("opensAt") val opensAt: String,
    @SerialName("closesAt") val closesAt: String,
    @SerialName("durationMinutes") val durationMinutes: Int,
    @SerialName("state") val state: ScheduleState,
    @SerialName("myStatus") val myStatus: MyExamStatus,
    @SerialName("attemptId") val attemptId: Long? = null,
    /** Only after the teacher shows the grades. */
    @SerialName("grade") val grade: Double? = null,
)

/** `GET /ruang/{code}/ujian/{schedule}`: the start page. */
@Serializable
data class ExamDetail(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("scheduleId") val scheduleId: Long,
    @SerialName("title") val title: String,
    /** Clean HTML. */
    @SerialName("instructions") val instructions: String? = null,
    @SerialName("questionCount") val questionCount: Int,
    @SerialName("durationMinutes") val durationMinutes: Int,
    @SerialName("opensAt") val opensAt: String,
    @SerialName("closesAt") val closesAt: String,
    @SerialName("availableMinutes") val availableMinutes: Int? = null,
    @SerialName("state") val state: ScheduleState,
    @SerialName("timeNotice") val timeNotice: String? = null,
    @SerialName("rules") val rules: List<String> = emptyList(),
    @SerialName("myStatus") val myStatus: MyExamStatus,
    @SerialName("attemptId") val attemptId: Long? = null,
    @SerialName("canStart") val canStart: Boolean,
    @SerialName("blockedReason") val blockedReason: String? = null,
    /** `other_schedule`, `not_open`, `closed`, `not_ready`, or null. */
    @SerialName("blockedCode") val blockedCode: String? = null,
)

/** `POST …/mulai`: [deviceToken] only for a new attempt; a running one answers null (take over instead). */
@Serializable
data class ExamStart(
    @SerialName("attempt_id") val attemptId: Long,
    @SerialName("device_token") val deviceToken: String? = null,
    @SerialName("status") val status: String,
)
