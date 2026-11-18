package com.gotest.app.feature.ruang

import com.gotest.app.feature.ruang.data.ViolationReporter
import com.gotest.app.feature.ruang.data.ViolationSend
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** T-026 A2b review 2 and A2b-4: leaving survives closing the app, and reports are sent again. */
class ViolationReporterTest {
    private val repository = ScriptedExamRepository()
    private val local = testLocalStore()
    private val left = Instant.parse("2026-12-01T01:40:00Z")

    private fun reporter() = ViolationReporter(9, repository, local) { "token" }

    @Test
    fun shortTimeAwayIsNotAViolation() {
        val reporter = reporter()
        reporter.leave(left)
        assertFalse(reporter.comeBack(left.plusMillis(1_999)))
        assertFalse(reporter.hasPending)
    }

    @Test
    fun timeAwayIsKeptWhenTheAppIsClosedMeanwhile() {
        reporter().leave(left)
        // A new screen after the app was closed: the same phone storage, a new reporter.
        val reopened = reporter()
        assertTrue(reopened.comeBack(left.plusSeconds(30)))
        val report = local.pendingViolations(9).single()
        assertEquals("page_left", report.kind)
        assertEquals(left.toString(), report.occurredAt)
        assertEquals(30_000L, report.awayMs)
    }

    @Test
    fun aSecondLeaveKeepsTheFirstMoment() {
        val reporter = reporter()
        reporter.leave(left)
        reporter.leave(left.plusSeconds(10))
        reporter.comeBack(left.plusSeconds(20))
        assertEquals(20_000L, local.pendingViolations(9).single().awayMs)
    }

    @Test
    fun reportsWithoutConnectionStayAndGoLaterWithTheirFirstTime() =
        runTest {
            val reporter = reporter()
            reporter.leave(left)
            reporter.comeBack(left.plusSeconds(5))
            assertEquals(ViolationSend.Offline, reporter.send())
            assertTrue(reporter.hasPending)

            repository.violationReplies += Result.success(ExamViolationResult(1, 3, "in_progress"))
            val sent = reporter.send()
            assertEquals(ViolationSend.Reported(ExamViolationResult(1, 3, "in_progress")), sent)
            assertFalse(reporter.hasPending)
            assertEquals(listOf(left.toString(), left.toString()), repository.reportedViolations.map { it.occurredAt })
        }

    @Test
    fun theThirdViolationStopsSendingTheRest() =
        runTest {
            val reporter = reporter()
            repeat(2) { round ->
                reporter.leave(left.plusSeconds(round * 60L))
                reporter.comeBack(left.plusSeconds(round * 60L + 5))
            }
            repository.violationReplies += Result.success(ExamViolationResult(3, 3, "submitted"))
            val sent = reporter.send() as ViolationSend.Reported
            assertEquals("submitted", sent.result.status)
            assertEquals(1, repository.reportedViolations.size)
        }

    @Test
    fun aRefusedReportIsDropped() =
        runTest {
            val reporter = reporter()
            reporter.leave(left)
            reporter.comeBack(left.plusSeconds(5))
            repository.violationReplies += refused(409, "other_device")
            assertTrue(reporter.send() is ViolationSend.Refused)
            assertFalse(reporter.hasPending)
        }

    @Test
    fun cancelledLeaveIsForgotten() {
        val reporter = reporter()
        reporter.leave(left)
        reporter.cancelLeave()
        assertFalse(reporter.comeBack(left.plusSeconds(60)))
    }

    @Test
    fun keystoreFailureKeepsTheTokenInMemory() {
        val cipher = ReversingCipher(failing = true)
        val store = testLocalStore(cipher)
        store.saveDeviceToken(9, "Hk3")
        assertEquals("Hk3", store.deviceToken(9))
    }
}
