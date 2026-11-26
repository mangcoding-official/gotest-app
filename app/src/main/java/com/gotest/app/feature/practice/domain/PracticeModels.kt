package com.gotest.app.feature.practice.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Latihan (API module C, T-030; API-CONTRACTS "API Android modul B dan C"). Catalog and start pages have the web's
 * props shape (camelCase, PracticePageData); the attempt is PracticeAttemptController's JSON as it is (snake_case).
 */

/** `GET /practice/packages` (web Practice/Index without `access`). */
@Serializable
data class PracticeCatalogPage(
    @SerialName("categories") val categories: List<CatalogCategory> = emptyList(),
    @SerialName("packages") val packages: List<PracticePackage> = emptyList(),
    @SerialName("recentAttempts") val recentAttempts: List<RecentAttempt> = emptyList(),
    @SerialName("stats") val stats: StudentStats = StudentStats(),
)

@Serializable
data class CatalogCategory(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class CatalogSubject(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class PackageComposition(
    @SerialName("subject") val subject: String,
    @SerialName("questionCount") val questionCount: Int,
    @SerialName("difficulty") val difficulty: String,
)

/** One package of the Latihan catalog; the start sheet reads the same row (PracticeStartPackage). */
@Serializable
data class PracticePackage(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("categoryId") val categoryId: Long? = null,
    @SerialName("categoryName") val categoryName: String? = null,
    @SerialName("progress") val progress: Int = 0,
    @SerialName("answeredCount") val answeredCount: Int = 0,
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("completed") val completed: Boolean = false,
    @SerialName("continueAttemptId") val continueAttemptId: Long? = null,
    /** Paket Khusus Premium: the mark for everyone (DEC-127 butir 8). */
    @SerialName("premium") val premium: Boolean = false,
    /** The lock for a Gratis account. */
    @SerialName("premiumLocked") val premiumLocked: Boolean = false,
    @SerialName("timerLabel") val timerLabel: String = "",
    @SerialName("timerOptional") val timerOptional: Boolean = false,
    @SerialName("composition") val composition: List<PackageComposition> = emptyList(),
    @SerialName("subjects") val subjects: List<CatalogSubject> = emptyList(),
    @SerialName("topicIds") val topicIds: List<Long> = emptyList(),
    @SerialName("allowRetry") val allowRetry: Boolean = true,
    @SerialName("lastAttemptId") val lastAttemptId: Long? = null,
    @SerialName("finishedCount") val finishedCount: Int = 0,
    @SerialName("lastFinishedAt") val lastFinishedAt: String? = null,
    @SerialName("lastScore") val lastScore: Double? = null,
    @SerialName("lastScoreLabel") val lastScoreLabel: String? = null,
) {
    /** A finished package that cannot be retried opens its last pembahasan instead of a new start. */
    val reviewAttemptId: Long?
        get() = lastAttemptId.takeIf { continueAttemptId == null && !allowRetry && finishedCount > 0 }
}

/** A finished attempt in "Aktivitas"; the result per subtest loads when the row is opened. */
@Serializable
data class RecentAttempt(
    @SerialName("id") val id: Long,
    @SerialName("packageName") val packageName: String,
    @SerialName("description") val description: String? = null,
    @SerialName("categoryName") val categoryName: String? = null,
    @SerialName("score") val score: Double? = null,
    @SerialName("scoreLabel") val scoreLabel: String? = null,
    @SerialName("submittedAt") val submittedAt: String? = null,
)

@Serializable
data class StudentStats(
    @SerialName("questionsDone") val questionsDone: Int = 0,
    @SerialName("accuracy") val accuracy: Int = 0,
    @SerialName("packagesDone") val packagesDone: Int = 0,
)

/** The parts of `GET /akun` a start reads: the plan and the Gratis quota (web `access`, `freePlan`). */
@Serializable
data class StartAccess(
    @SerialName("access") val access: AccountAccess? = null,
    @SerialName("free_plan") val freePlan: FreePlan? = null,
)

@Serializable
data class AccountAccess(
    /** `active` or `expired`. */
    @SerialName("status") val status: String = "expired",
) {
    val active: Boolean get() = status == "active"
}

@Serializable
data class FreePlan(
    @SerialName("practice") val practice: FreeQuota = FreeQuota(),
    @SerialName("tryout") val tryout: FreeQuota = FreeQuota(),
)

@Serializable
data class FreeQuota(
    @SerialName("used") val used: Int = 0,
    @SerialName("limit") val limit: Int = 0,
) {
    val left: Int get() = (limit - used).coerceAtLeast(0)
}

@Serializable
data class StartRequest(
    @SerialName("timer_enabled") val timerEnabled: Boolean? = null,
)

@Serializable
data class AttemptEnvelope(
    @SerialName("attempt") val attempt: PracticeAttempt,
)

/** `GET /attempts/{attempt}` and the answer of a start: the attempt without any key. */
@Serializable
data class PracticeAttempt(
    @SerialName("id") val id: Long,
    /** `started`, `submitted`, `auto_submitted`. */
    @SerialName("status") val status: String,
    @SerialName("server_now") val serverNow: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    /** `none`, `package`, `per_question`. */
    @SerialName("effective_timer_mode") val timerMode: String = TIMER_NONE,
    @SerialName("timer_enabled") val timerEnabled: Boolean = false,
    @SerialName("deadline_at") val deadlineAt: String? = null,
    @SerialName("active_question_item_id") val activeItemId: Long? = null,
    /** `linear` for a Latihan timed per question (answered in order), otherwise `free`. */
    @SerialName("navigation") val navigation: String = "free",
    @SerialName("question_items") val items: List<PracticeItem> = emptyList(),
) {
    val running: Boolean get() = status == STATUS_STARTED
    val linear: Boolean get() = navigation == "linear"

    companion object {
        const val STATUS_STARTED = "started"
        const val TIMER_NONE = "none"
        const val TIMER_PACKAGE = "package"
        const val TIMER_PER_QUESTION = "per_question"
    }
}

@Serializable
data class PracticeItem(
    @SerialName("id") val id: Long,
    @SerialName("question_id") val questionId: Long,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("answered_at") val answeredAt: String? = null,
    @SerialName("question_type") val questionType: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("selected_option") val selectedOption: Int? = null,
    @SerialName("selected_options") val selectedOptions: List<Int> = emptyList(),
    @SerialName("statement_answers") val statementAnswers: List<Int?> = emptyList(),
    @SerialName("question_deadline_at") val questionDeadlineAt: String? = null,
    @SerialName("timed_out") val timedOut: Boolean = false,
    @SerialName("locked") val locked: Boolean = false,
) {
    val closed: Boolean get() = locked || timedOut
}

/** `POST /attempts/{attempt}/questions/{item}/activate`: the question and its reading, without a key. */
@Serializable
data class ActivatedQuestion(
    @SerialName("question") val question: ActiveQuestion,
    @SerialName("server_now") val serverNow: String? = null,
)

@Serializable
data class ActiveQuestion(
    @SerialName("attemptQuestionItemId") val itemId: Long,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("displayOrder") val displayOrder: Int? = null,
    @SerialName("questionType") val questionType: String? = null,
    @SerialName("content") val content: QuestionContent = QuestionContent(),
)

@Serializable
data class QuestionContent(
    /** Clean HTML. */
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("media_path") val mediaPath: String? = null,
    @SerialName("stimulus") val stimulus: Stimulus? = null,
    @SerialName("options") val options: List<OptionText> = emptyList(),
    @SerialName("answer_labels") val answerLabels: List<OptionText> = emptyList(),
    @SerialName("statements") val statements: List<OptionText> = emptyList(),
)

@Serializable
data class Stimulus(
    @SerialName("name") val name: String? = null,
    @SerialName("text") val text: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("media_path") val mediaPath: String? = null,
)

@Serializable
data class OptionText(
    @SerialName("text") val text: String = "",
    @SerialName("media_path") val mediaPath: String? = null,
)

/** `PATCH /attempts/{attempt}/answers/{question}`: one of the three shapes, the others left out. */
@Serializable
data class AnswerRequest(
    @SerialName("answer_data") val answerData: AnswerData,
)

@Serializable
data class AnswerData(
    @SerialName("selected_option") val selectedOption: Int? = null,
    @SerialName("selected_options") val selectedOptions: List<Int>? = null,
    @SerialName("statement_answers") val statementAnswers: List<Int?>? = null,
)

@Serializable
data class ResultEnvelope(
    @SerialName("result") val result: PracticeResult,
)

/** `GET /attempts/{attempt}/result`: key, answers, and pembahasan from the snapshot (DEC-071). */
@Serializable
data class PracticeResult(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String? = null,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("score_label") val scoreLabel: String? = null,
    @SerialName("display_score") val displayScore: DisplayScore? = null,
    @SerialName("disqualified") val disqualified: Boolean = false,
    @SerialName("scoring_profile") val scoringProfile: ScoringProfile? = null,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("empty_count") val emptyCount: Int = 0,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("question_items") val items: List<ReviewItem> = emptyList(),
)

@Serializable
data class DisplayScore(
    @SerialName("value") val value: Double,
    @SerialName("scale") val scale: Double? = null,
    @SerialName("label") val label: String,
    @SerialName("correct") val correct: Int = 0,
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class ScoringProfile(
    @SerialName("scale") val scale: Double? = null,
    @SerialName("weighted") val weighted: Boolean = false,
)

/** One question of a pembahasan (web PembahasanItem). */
@Serializable
data class ReviewItem(
    @SerialName("id") val id: Long,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("question_type") val questionType: String? = null,
    @SerialName("selected_option") val selectedOption: Int? = null,
    @SerialName("correct_option") val correctOption: Int? = null,
    @SerialName("selected_options") val selectedOptions: List<Int> = emptyList(),
    @SerialName("correct_options") val correctOptions: List<Int> = emptyList(),
    @SerialName("statement_answers") val statementAnswers: List<Int?> = emptyList(),
    @SerialName("statement_correct_answers") val statementCorrectAnswers: List<Int?> = emptyList(),
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("is_empty") val isEmpty: Boolean = false,
    @SerialName("score_awarded") val scoreAwarded: Double = 0.0,
    @SerialName("max_score") val maxScore: Double = 0.0,
    @SerialName("topic_name") val topicName: String? = null,
    /** Clean HTML. */
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("explanation_media_path") val explanationMediaPath: String? = null,
    @SerialName("question") val question: QuestionContent = QuestionContent(),
)
