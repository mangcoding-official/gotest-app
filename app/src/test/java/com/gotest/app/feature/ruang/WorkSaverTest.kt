package com.gotest.app.feature.ruang

import com.gotest.app.feature.ruang.data.ExamAnswerQueue
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.ui.work.FlushResult
import com.gotest.app.feature.ruang.ui.work.WorkSaver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WorkSaverTest {
    private val repository = ScriptedExamRepository()
    private val local = testLocalStore()
    private val queue = ExamAnswerQueue(9, local, lastClientSeq = 0)
    private val saver = WorkSaver(9, repository, queue, { "token" }, { (it - 54).toInt() })
    private val at = Instant.parse("2026-12-01T01:30:05Z")

    private fun saved(vararg ids: Long) =
        ExamAnswersSaved(
            saved = ids.toList(),
            lastClientSeq = 2,
            serverNow = "2026-12-01T01:31:00+00:00",
            deadlineAt = "2026-12-01T02:30:00+00:00",
            status = "in_progress",
        )

    @Test
    fun nothingToSendIsSent() =
        runTest {
            assertEquals(FlushResult.Sent(null, null), saver.flush(wait = true))
            assertTrue(repository.savedBatches.isEmpty())
        }

    @Test
    fun savedAnswersLeaveTheQueue() =
        runTest {
            queue.record(55, buildJsonObject { put("selected", "b") }, at)
            repository.saveReplies += Result.success(saved(55))
            val result = saver.flush(wait = true) as FlushResult.Sent
            assertEquals(listOf(55L), result.reply?.saved)
            assertTrue(saver.isEmpty)
        }

    @Test
    fun withoutConnectionTheAnswersStay() =
        runTest {
            queue.record(55, buildJsonObject { put("selected", "b") }, at)
            assertEquals(FlushResult.Offline, saver.flush(wait = true))
            assertEquals(setOf(55L), saver.pendingIds)
        }

    @Test
    fun aValidationRefusalDropsTheBatchButOtherRefusalsKeepIt() =
        runTest {
            queue.record(55, buildJsonObject { put("selected", "b") }, at)
            repository.saveReplies += refused(409, "other_device")
            assertTrue(saver.flush(wait = true) is FlushResult.Refused)
            assertEquals(setOf(55L), saver.pendingIds)

            repository.saveReplies += refused(422, "validation", "Waktu perangkat tidak wajar.")
            assertTrue(saver.flush(wait = true) is FlushResult.Refused)
            assertTrue(saver.isEmpty)
        }
}
