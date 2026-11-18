package com.gotest.app.feature.ruang.data

import com.gotest.app.feature.ruang.domain.ExamAnswerUpload
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.OffsetDateTime

/**
 * The Ujian clock is the server's (DEC-148): the deadline minus the server time, counted down with
 * the phone's monotonic clock from when the server time arrived, so changing the phone's time changes
 * nothing. [anchorElapsedMs] is `SystemClock.elapsedRealtime()` at that moment.
 */
data class ExamClock(
    val deadline: Instant,
    val serverNow: Instant,
    val anchorElapsedMs: Long,
) {
    fun millisLeft(elapsedMs: Long): Long = deadline.toEpochMilli() - now(elapsedMs).toEpochMilli()

    /** The server's time now: what `answered_at` and `occurred_at` carry (preflight API 4.10). */
    fun now(elapsedMs: Long): Instant = serverNow.plusMillis(elapsedMs - anchorElapsedMs)

    companion object {
        fun of(
            deadlineAt: String,
            serverNow: String,
            anchorElapsedMs: Long,
        ) = ExamClock(parseServerTime(deadlineAt), parseServerTime(serverNow), anchorElapsedMs)
    }
}

/** What a save reply means for the student (texts are the web's, `Work.tsx`). */
data class SaveOutcome(
    /** Item numbers that arrived after the deadline grace period. */
    val rejectedNumbers: List<Int>,
    /** Some answers were replaced from another device in the meantime. */
    val stale: Boolean,
    val submitted: Boolean,
)

/**
 * Answers waiting for the server, numbered with `client_seq` after what the server kept (R-01), and
 * kept on the phone until the server took them (the web keeps them in localStorage).
 */
class ExamAnswerQueue(
    private val attemptId: Long,
    private val store: ExamLocalStore,
    lastClientSeq: Long,
) {
    var pending: Map<Long, PendingAnswer> = emptyMap()
        private set

    private var nextSeq: Long

    init {
        // Answers the server already has (sent before a reload or from before a take-over) are dropped.
        replace(store.pending(attemptId).filterValues { it.seq > lastClientSeq })
        nextSeq = maxOf(lastClientSeq, pending.values.maxOfOrNull { it.seq } ?: 0) + 1
    }

    val isEmpty: Boolean get() = pending.isEmpty()

    /** Keeps only the latest answer per item; [answer] is already clean (JsonNull clears it). */
    fun record(
        itemId: Long,
        answer: JsonElement,
        answeredAt: Instant,
    ) {
        replace(pending + (itemId to PendingAnswer(answer, nextSeq++, answeredAt.toString())))
    }

    /** At most [MAX_BATCH] answers per request, as the server accepts. */
    fun batch(): Map<Long, PendingAnswer> = pending.entries.take(MAX_BATCH).associate { it.toPair() }

    fun uploads(batch: Map<Long, PendingAnswer>): List<ExamAnswerUpload> =
        batch.map { (itemId, entry) -> ExamAnswerUpload(itemId, entry.answer, entry.seq, entry.answeredAt) }

    /**
     * Removes what the server dealt with (saved, stale, or rejected), unless the answer changed again
     * while this batch was on its way: that one stays for the next batch.
     */
    fun applyReply(
        sent: Map<Long, PendingAnswer>,
        reply: ExamAnswersSaved,
        numberOf: (Long) -> Int?,
    ): SaveOutcome {
        val done = (reply.saved + reply.stale + reply.rejected).toSet()
        replace(pending.filterNot { (id, entry) -> id in done && entry.seq == sent[id]?.seq })
        nextSeq = maxOf(nextSeq, reply.lastClientSeq + 1)
        return SaveOutcome(
            rejectedNumbers = reply.rejected.mapNotNull(numberOf).sorted(),
            stale = reply.stale.isNotEmpty(),
            submitted = reply.status == STATUS_SUBMITTED,
        )
    }

    /** The server refused this batch for good (422): sending it again would fail the same way. */
    fun dropBatch(sent: Map<Long, PendingAnswer>) {
        replace(pending.filterNot { (id, entry) -> entry.seq == sent[id]?.seq })
    }

    fun clear() {
        replace(emptyMap())
    }

    private fun replace(next: Map<Long, PendingAnswer>) {
        pending = next
        store.savePending(attemptId, next)
    }

    companion object {
        const val MAX_BATCH = 200
        const val STATUS_SUBMITTED = "submitted"
    }
}

/**
 * A server time (`2026-12-01T02:30:00+00:00`, ISO 8601 with an offset). Not `Instant.parse`: on Android
 * (java.time through desugaring, Java 8 rules) it only accepts a `Z` ending.
 */
fun parseServerTime(value: String): Instant = OffsetDateTime.parse(value).toInstant()

/** "1", "1 dan 2", "1, 2 dan 3" (the web's `listNumbers`). */
fun listNumbers(numbers: List<Int>): String =
    if (numbers.size <= 1) {
        numbers.joinToString("")
    } else {
        "${numbers.dropLast(1).joinToString(", ")} dan ${numbers.last()}"
    }
