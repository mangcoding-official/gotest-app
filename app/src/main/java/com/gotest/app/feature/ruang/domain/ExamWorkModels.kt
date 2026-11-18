package com.gotest.app.feature.ruang.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** `GET /ruang/{code}/ujian/kerjakan/{attempt}` with `X-Exam-Device`: the paper, without any key. */
@Serializable
data class ExamWorkPage(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("title") val title: String,
    @SerialName("attempt") val attempt: ExamAttemptInfo,
    /** `exam`; a Rencana Belajar quiz (`quiz`) comes with A1k. */
    @SerialName("mode") val mode: String = MODE_EXAM,
    @SerialName("blocks") val blocks: List<ExamBlock> = emptyList(),
    @SerialName("items") val items: List<ExamItem> = emptyList(),
    /** Saved answers by item id; items without an answer are absent. */
    @SerialName("answers") val answers: Map<String, JsonElement> = emptyMap(),
) {
    val isQuiz: Boolean get() = mode == MODE_QUIZ

    companion object {
        const val MODE_EXAM = "exam"
        const val MODE_QUIZ = "quiz"
    }
}

@Serializable
data class ExamAttemptInfo(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("deadlineAt") val deadlineAt: String,
    @SerialName("serverNow") val serverNow: String,
    @SerialName("violations") val violations: Int = 0,
    @SerialName("violationLimit") val violationLimit: Int = 0,
    /** The token sent, while it is still this device's; null after another device took over. */
    @SerialName("deviceToken") val deviceToken: String? = null,
    /** The highest `client_seq` the server kept; numbering goes on from here + 1. */
    @SerialName("lastClientSeq") val lastClientSeq: Long = 0,
)

/** One screen of the paper: a single item, or a reading passage with all its items (kept together). */
@Serializable
data class ExamBlock(
    @SerialName("kind") val kind: String,
    @SerialName("itemIds") val itemIds: List<Long>,
    @SerialName("groupId") val groupId: Long? = null,
    @SerialName("title") val title: String? = null,
    /** Clean HTML. */
    @SerialName("passage") val passage: String? = null,
    @SerialName("mediaUrl") val mediaUrl: String? = null,
)

@Serializable
data class ExamItem(
    @SerialName("id") val id: Long,
    /** 1..n in the order this student sees. */
    @SerialName("number") val number: Int,
    @SerialName("type") val type: String,
    /** Clean HTML. */
    @SerialName("prompt") val prompt: String,
    @SerialName("mediaUrl") val mediaUrl: String? = null,
    /** Already in this student's order; read with [ExamContent.of]. */
    @SerialName("content") val content: JsonObject = JsonObject(emptyMap()),
)

/** One answer sent in `PUT /ujian/pengerjaan/{attempt}/jawaban`. */
@Serializable
data class ExamAnswerUpload(
    @SerialName("item_id") val itemId: Long,
    /**
     * [kotlinx.serialization.json.JsonNull] clears the answer. Not a Kotlin null: the app's Json drops
     * null fields (`explicitNulls = false`), and the server needs `"answer": null` to clear it.
     */
    @SerialName("answer") val answer: JsonElement,
    @SerialName("client_seq") val clientSeq: Long,
    /** ISO 8601: the phone's clock corrected by the server's (`serverNow`). */
    @SerialName("answered_at") val answeredAt: String,
)

@Serializable
data class ExamAnswersRequest(
    @SerialName("items") val items: List<ExamAnswerUpload>,
)

/** The reply to saving answers; snake_case, as on the web. */
@Serializable
data class ExamAnswersSaved(
    @SerialName("saved") val saved: List<Long> = emptyList(),
    /** Older than what the server has (another device answered later). */
    @SerialName("stale") val stale: List<Long> = emptyList(),
    /** Arrived after the deadline grace period, or in a wrong shape. */
    @SerialName("rejected") val rejected: List<Long> = emptyList(),
    @SerialName("last_client_seq") val lastClientSeq: Long = 0,
    @SerialName("server_now") val serverNow: String,
    @SerialName("deadline_at") val deadlineAt: String,
    @SerialName("status") val status: String,
)

@Serializable
data class ExamViolationRequest(
    @SerialName("kind") val kind: String,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("away_ms") val awayMs: Long,
)

/** The third violation submits the attempt (`status: submitted`). */
@Serializable
data class ExamViolationResult(
    @SerialName("violations") val violations: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("status") val status: String,
)

@Serializable
data class ExamTakeOver(
    @SerialName("device_token") val deviceToken: String,
)

@Serializable
data class ExamSubmitted(
    @SerialName("status") val status: String,
)

/** `GET /ruang/{code}/ujian/hasil/{attempt}`; 409 `in_progress` while the attempt still runs. */
@Serializable
data class ExamResultPage(
    @SerialName("organization") val organization: MitraRef,
    @SerialName("title") val title: String,
    @SerialName("mode") val mode: String = ExamWorkPage.MODE_EXAM,
    @SerialName("submittedAt") val submittedAt: String? = null,
    /** `manual`, `time_up`, `violation`, `teacher`. */
    @SerialName("submitReason") val submitReason: String? = null,
    @SerialName("released") val released: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("grade") val grade: Double? = null,
    @SerialName("showAnswerKey") val showAnswerKey: Boolean = false,
    @SerialName("items") val items: List<ExamResultItem> = emptyList(),
)

@Serializable
data class ExamResultItem(
    @SerialName("number") val number: Int,
    /** `correct`, `partial`, `wrong`, `empty`, or null. */
    @SerialName("result") val result: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("content") val content: JsonObject? = null,
    @SerialName("answerKey") val answerKey: JsonObject? = null,
    @SerialName("answer") val answer: JsonElement? = null,
    @SerialName("explanation") val explanation: String? = null,
)
