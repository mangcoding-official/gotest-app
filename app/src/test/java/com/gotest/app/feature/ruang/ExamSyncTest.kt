package com.gotest.app.feature.ruang

import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.ruang.data.ExamAnswerQueue
import com.gotest.app.feature.ruang.data.ExamClock
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.data.PendingAnswer
import com.gotest.app.feature.ruang.data.listNumbers
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExamSyncTest {
    private val prefs = InMemoryStore()
    private val store = ExamLocalStore(prefs, ReversingCipher(), Json { ignoreUnknownKeys = true })
    private val at = Instant.parse("2026-12-01T01:30:05Z")

    private fun selected(id: String) = buildJsonObject { put("selected", id) }

    private fun reply(
        saved: List<Long> = emptyList(),
        stale: List<Long> = emptyList(),
        rejected: List<Long> = emptyList(),
        last: Long = 0,
        status: String = "in_progress",
    ) = ExamAnswersSaved(saved, stale, rejected, last, "2026-12-01T01:31:00+00:00", "2026-12-01T02:30:00+00:00", status)

    @Test
    fun numberingGoesOnAfterWhatTheServerKept() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 4)
        queue.record(55, selected("c"), at)
        queue.record(56, selected("a"), at)
        assertEquals(listOf(5L, 6L), queue.pending.values.map { it.seq })
    }

    @Test
    fun onlyTheLatestAnswerPerItemIsKept() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, selected("c"), at)
        queue.record(55, selected("d"), at)
        assertEquals(1, queue.pending.size)
        assertEquals(PendingAnswer(selected("d"), 2, at.toString()), queue.pending[55])
    }

    @Test
    fun pendingAnswersSurviveReopeningAndDropWhatTheServerAlreadyHas() {
        ExamAnswerQueue(9, store, lastClientSeq = 0).apply {
            record(55, selected("c"), at)
            record(56, selected("a"), at)
            record(57, JsonNull, at)
        }
        // After a take-over the server already kept seq 1 and 2 from this phone.
        val reopened = ExamAnswerQueue(9, store, lastClientSeq = 2)
        assertEquals(setOf(57L), reopened.pending.keys)
        reopened.record(58, selected("b"), at)
        assertEquals(4L, reopened.pending.getValue(58).seq)
    }

    @Test
    fun anAnswerChangedWhileSendingStaysForTheNextBatch() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, selected("c"), at)
        queue.record(56, selected("a"), at)
        val sent = queue.batch()
        queue.record(56, selected("b"), at)
        val outcome = queue.applyReply(sent, reply(saved = listOf(55, 56), last = 2)) { it.toInt() - 54 }
        assertEquals(setOf(56L), queue.pending.keys)
        assertEquals(selected("b"), queue.pending.getValue(56).answer)
        assertFalse(outcome.stale)
        assertTrue(outcome.rejectedNumbers.isEmpty())
    }

    @Test
    fun rejectedAndStaleAnswersLeaveTheQueueAndAreReported() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, selected("c"), at)
        queue.record(57, selected("a"), at)
        queue.record(56, selected("d"), at)
        val outcome =
            queue.applyReply(
                queue.batch(),
                reply(saved = listOf(55), stale = listOf(56), rejected = listOf(57, 55), last = 3),
            ) {
                (it - 54).toInt()
            }
        assertTrue(queue.isEmpty)
        assertEquals(listOf(1, 3), outcome.rejectedNumbers)
        assertTrue(outcome.stale)
        assertNull(prefs.getString("pending.9"))
    }

    @Test
    fun theServerSequenceMovesTheNextNumberForward() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, selected("c"), at)
        queue.applyReply(queue.batch(), reply(saved = listOf(55), last = 40)) { 1 }
        queue.record(56, selected("a"), at)
        assertEquals(41L, queue.pending.getValue(56).seq)
    }

    @Test
    fun aRefusedBatchIsDroppedButNewerAnswersStay() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, selected("c"), at)
        val sent = queue.batch()
        queue.record(55, selected("d"), at)
        queue.record(56, selected("a"), at)
        queue.dropBatch(sent)
        assertEquals(setOf(55L, 56L), queue.pending.keys)
        assertEquals(selected("d"), queue.pending.getValue(55).answer)
    }

    @Test
    fun aBatchHoldsAtMostTwoHundredAnswers() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        (1L..250L).forEach { queue.record(it, selected("a"), at) }
        assertEquals(ExamAnswerQueue.MAX_BATCH, queue.batch().size)
    }

    @Test
    fun aClearedAnswerIsSentAsJsonNull() {
        val queue = ExamAnswerQueue(9, store, lastClientSeq = 0)
        queue.record(55, JsonNull, at)
        val upload = queue.uploads(queue.batch()).single()
        assertEquals(JsonNull, upload.answer)
        assertTrue(Json.encodeToString(upload).contains("\"answer\":null"))
    }

    @Test
    fun deviceTokenIsEncryptedAndForgottenAfterSubmitting() {
        store.saveDeviceToken(9, "Hk3token")
        assertEquals("nekot3kH", prefs.getString("device.9"))
        assertEquals("Hk3token", store.deviceToken(9))
        ExamAnswerQueue(9, store, 0).record(55, selected("c"), at)
        store.forget(9)
        assertNull(store.deviceToken(9))
        assertTrue(store.pending(9).isEmpty())
    }

    @Test
    fun clockFollowsTheServerNotThePhone() {
        val clock = ExamClock.of("2026-12-01T02:30:00+00:00", "2026-12-01T01:30:00+00:00", anchorElapsedMs = 10_000)
        assertEquals(3_600_000L, clock.millisLeft(10_000))
        assertEquals(3_595_000L, clock.millisLeft(15_000))
        assertEquals(Instant.parse("2026-12-01T01:30:05Z"), clock.now(15_000))
    }

    @Test
    fun numbersAreListedLikeTheWeb() {
        assertEquals("", listNumbers(emptyList()))
        assertEquals("1", listNumbers(listOf(1)))
        assertEquals("1 dan 2", listNumbers(listOf(1, 2)))
        assertEquals("1, 2 dan 3", listNumbers(listOf(1, 2, 3)))
    }

    private class InMemoryStore : KeyValueStore {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private class ReversingCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain.reversed()

        override fun decrypt(encrypted: String): String? = encrypted.reversed()
    }
}
