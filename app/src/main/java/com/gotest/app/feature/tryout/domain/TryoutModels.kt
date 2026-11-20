package com.gotest.app.feature.tryout.domain

import com.gotest.app.feature.practice.domain.CatalogCategory
import com.gotest.app.feature.practice.domain.QuestionContent
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.practice.domain.StudentStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Try Out and scheduled sessions (API module D, T-032; API-CONTRACTS "API Android modul D"). Pages have the web's
 * props shape (camelCase, TryoutPageData, TryoutSessionData); the attempt is TryoutAttemptController's JSON as it
 * is (snake_case).
 */

/** `GET /tryout/packages` (web Tryout/Index without `access`). */
@Serializable
data class TryoutCatalogPage(
    @SerialName("sessions") val sessions: List<TryoutSession> = emptyList(),
    @SerialName("serverNow") val serverNow: String? = null,
    @SerialName("categories") val categories: List<CatalogCategory> = emptyList(),
    @SerialName("packages") val packages: List<TryoutPackage> = emptyList(),
    @SerialName("recentAttempts") val recentAttempts: List<RecentAttempt> = emptyList(),
    @SerialName("stats") val stats: StudentStats = StudentStats(),
)

/** When a package can be started (DEC-065): `none`, `upcoming`, `open`, `closed`. */
@Serializable
data class TryoutWindow(
    @SerialName("state") val state: String = STATE_NONE,
    @SerialName("opensAt") val opensAt: String? = null,
    @SerialName("closesAt") val closesAt: String? = null,
    @SerialName("reviewOpensAt") val reviewOpensAt: String? = null,
) {
    companion object {
        const val STATE_NONE = "none"
        const val STATE_UPCOMING = "upcoming"
        const val STATE_OPEN = "open"
        const val STATE_CLOSED = "closed"
    }
}

@Serializable
data class TryoutSectionInfo(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
)

@Serializable
data class LeaderboardRow(
    @SerialName("rank") val rank: Int,
    @SerialName("name") val name: String,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("durationUsed") val durationUsed: Int = 0,
)

/** One package of the Try Out catalog; the start sheet reads the same row (TryoutStartPackage). */
@Serializable
data class TryoutPackage(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("categoryId") val categoryId: Long? = null,
    @SerialName("categoryName") val categoryName: String? = null,
    @SerialName("progress") val progress: Int = 0,
    @SerialName("completed") val completed: Boolean = false,
    @SerialName("continueAttemptId") val continueAttemptId: Long? = null,
    @SerialName("premium") val premium: Boolean = false,
    @SerialName("premiumLocked") val premiumLocked: Boolean = false,
    @SerialName("allowRetry") val allowRetry: Boolean = true,
    @SerialName("finishedCount") val finishedCount: Int = 0,
    @SerialName("lastFinishedAt") val lastFinishedAt: String? = null,
    @SerialName("lastScore") val lastScore: Double? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("totalParticipants") val totalParticipants: Int? = null,
    @SerialName("window") val window: TryoutWindow? = null,
    @SerialName("lastAttemptId") val lastAttemptId: Long? = null,
    @SerialName("ruleLabel") val ruleLabel: String = "",
    @SerialName("sections") val sections: List<TryoutSectionInfo> = emptyList(),
    @SerialName("leaderboard") val leaderboard: List<LeaderboardRow> = emptyList(),
) {
    /** A Try Out that cannot be done again opens its last result instead (web `resultOnly`). */
    val resultOnlyId: Long?
        get() = lastAttemptId.takeIf { finishedCount > 0 && !allowRetry && continueAttemptId == null }

    val windowState: String get() = window?.state ?: TryoutWindow.STATE_NONE
}

/** A scheduled session (DEC-067), on the catalog and its own page. */
@Serializable
data class TryoutSession(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("packageId") val packageId: Long,
    @SerialName("packageName") val packageName: String = "",
    @SerialName("premium") val premium: Boolean = false,
    @SerialName("premiumLocked") val premiumLocked: Boolean = false,
    /** `scheduled`, `running`, `closed`. */
    @SerialName("state") val state: String,
    @SerialName("startsAt") val startsAt: String,
    @SerialName("endsAt") val endsAt: String,
    @SerialName("capacity") val capacity: Int? = null,
    @SerialName("registeredCount") val registeredCount: Int = 0,
    @SerialName("isFull") val isFull: Boolean = false,
    /** A public session not yet registered: whether the Gratis quota of its month allows it (review R-08). */
    @SerialName("canRegister") val canRegister: Boolean? = null,
    @SerialName("registered") val registered: Boolean = false,
    @SerialName("attemptId") val attemptId: Long? = null,
    @SerialName("attemptStatus") val attemptStatus: String? = null,
    @SerialName("attemptDone") val attemptDone: Boolean = false,
    @SerialName("score") val score: Double? = null,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("totalParticipants") val totalParticipants: Int? = null,
    /** A school's own session; its participants are chosen by the school (DEC-088). */
    @SerialName("partner") val partner: SessionPartner? = null,
    @SerialName("sections") val sections: List<SessionSection> = emptyList(),
) {
    companion object {
        const val SCHEDULED = "scheduled"
        const val RUNNING = "running"
        const val CLOSED = "closed"
    }
}

@Serializable
data class SessionPartner(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
)

@Serializable
data class SessionSection(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("orderNumber") val orderNumber: Int,
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
)

/** `GET /tryout/sesi/{session}` (web Tryout/Session). */
@Serializable
data class TryoutSessionPage(
    @SerialName("session") val session: TryoutSession,
    @SerialName("canStart") val canStart: Boolean = false,
)

@Serializable
data class SessionMessage(
    @SerialName("message") val message: String? = null,
)

/** `GET /tryout/sesi/{session}/leaderboard`: empty until the session ends ([availableAt]). */
@Serializable
data class SessionLeaderboard(
    @SerialName("entries") val entries: List<SessionLeaderboardEntry> = emptyList(),
    @SerialName("user_entry") val userEntry: SessionLeaderboardEntry? = null,
    @SerialName("total_participants") val totalParticipants: Int = 0,
    @SerialName("available_at") val availableAt: String? = null,
)

@Serializable
data class SessionLeaderboardEntry(
    @SerialName("rank") val rank: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("duration_used") val durationUsed: Int = 0,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("is_current_user") val isCurrentUser: Boolean = false,
)

@Serializable
data class TryoutAttemptEnvelope(
    @SerialName("attempt") val attempt: TryoutAttempt,
)

/** `GET /tryout/attempts/{attempt}`: the sections, the running one with its questions, without any key. */
@Serializable
data class TryoutAttempt(
    @SerialName("id") val id: Long,
    /** `started`, `submitted`, `auto_submitted`, `cancelled`. */
    @SerialName("status") val status: String,
    @SerialName("in_session") val inSession: Boolean = false,
    @SerialName("server_now") val serverNow: String? = null,
    @SerialName("active_section") val activeSection: TryoutActiveSection? = null,
    @SerialName("next_section") val nextSection: TryoutSectionSummary? = null,
    @SerialName("sections") val sections: List<TryoutSectionSummary> = emptyList(),
) {
    val running: Boolean get() = status == STATUS_STARTED

    companion object {
        const val STATUS_STARTED = "started"
        const val STATUS_CANCELLED = "cancelled"
    }
}

@Serializable
data class TryoutSectionSummary(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("order_number") val orderNumber: Int,
    /** `pending`, `started`, and a finished state. */
    @SerialName("status") val status: String,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("question_count") val questionCount: Int = 0,
) {
    companion object {
        const val PENDING = "pending"
        const val STARTED = "started"
    }
}

@Serializable
data class TryoutActiveSection(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("order_number") val orderNumber: Int,
    @SerialName("status") val status: String = TryoutSectionSummary.STARTED,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("deadline_at") val deadlineAt: String? = null,
    @SerialName("question_items") val items: List<TryoutItem> = emptyList(),
)

@Serializable
data class TryoutItem(
    /** The item id; answers and flags are sent with it, not with the question id. */
    @SerialName("id") val id: Long,
    @SerialName("question_id") val questionId: Long? = null,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("answered_at") val answeredAt: String? = null,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
    @SerialName("selected_option") val selectedOption: Int? = null,
    @SerialName("selected_options") val selectedOptions: List<Int> = emptyList(),
    @SerialName("statement_answers") val statementAnswers: List<Int?> = emptyList(),
    @SerialName("question") val question: TryoutQuestion = TryoutQuestion(),
)

@Serializable
data class TryoutQuestion(
    @SerialName("question_type") val questionType: String? = null,
    @SerialName("content") val content: QuestionContent? = null,
)

@Serializable
data class FlagRequest(
    @SerialName("flagged") val flagged: Boolean,
)

@Serializable
data class FinishSectionRequest(
    @SerialName("section_id") val sectionId: Long? = null,
)

@Serializable
data class TryoutResultEnvelope(
    @SerialName("result") val result: TryoutResult,
)

/** `GET /tryout/attempts/{attempt}/result`: score, per subtest, and ranking; no key or pembahasan (DEC-074). */
@Serializable
data class TryoutResult(
    @SerialName("id") val id: Long,
    @SerialName("package_id") val packageId: Long? = null,
    @SerialName("package_name") val packageName: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("score_label") val scoreLabel: String? = null,
    @SerialName("ranked") val ranked: Boolean? = null,
    @SerialName("disqualified") val disqualified: Boolean = false,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("total_participants") val totalParticipants: Int? = null,
    @SerialName("session_id") val sessionId: Long? = null,
    @SerialName("ranking_available") val rankingAvailable: Boolean? = null,
    @SerialName("ranking_available_at") val rankingAvailableAt: String? = null,
    @SerialName("scoring_profile") val scoringProfile: com.gotest.app.feature.practice.domain.ScoringProfile? = null,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("empty_count") val emptyCount: Int = 0,
    @SerialName("sections") val sections: List<TryoutResultSection> = emptyList(),
)

@Serializable
data class TryoutResultSection(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("empty_count") val emptyCount: Int = 0,
    @SerialName("section_score") val sectionScore: Double = 0.0,
    @SerialName("max_score") val maxScore: Double = 0.0,
)
