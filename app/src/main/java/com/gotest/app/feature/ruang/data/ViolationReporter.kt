package com.gotest.app.feature.ruang.data

import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** The phone's monotonic clock (`SystemClock.elapsedRealtime()`), replaceable in tests. */
fun interface ElapsedClock {
    fun elapsedMs(): Long
}

/** What sending the queued violation reports ended with. */
sealed interface ViolationSend {
    /** Nothing was waiting. */
    data object Nothing : ViolationSend

    /** All sent; [result] is the server's count after the last one. */
    data class Reported(
        val result: ExamViolationResult,
    ) : ViolationSend

    /** No connection: the reports stay on the phone and go with the next try. */
    data object Offline : ViolationSend

    /** The server could not take it now (5xx, 429, 401): the reports stay on the phone for the next try. */
    data object Later : ViolationSend

    /** Refused for good (422 or a refusal with a code): that report is dropped. */
    data class Refused(
        val error: ApiError,
    ) : ViolationSend
}

/**
 * Leaving the Ujian (Aturan 12.13, API-K8 A). The moment the student left is kept on the phone, so
 * time away still counts when the app is closed meanwhile (T-026 A2b review 2); two seconds or more
 * becomes a report, kept and sent again until the server took it, with its first time (A2b-4).
 */
class ViolationReporter(
    private val attemptId: Long,
    private val repository: ExamRepository,
    private val local: ExamLocalStore,
    private val deviceToken: () -> String?,
) {
    private val mutex = Mutex()

    val hasPending: Boolean get() = local.pendingViolations(attemptId).isNotEmpty()

    /** The student left at [at] (server time); a later leave before coming back changes nothing. */
    fun leave(at: Instant) {
        if (local.awaySince(attemptId) == null) local.saveAwaySince(attemptId, at.toString())
    }

    /** The student is back at [now]: true when the time away became a report to send. */
    fun comeBack(now: Instant): Boolean {
        val since = local.awaySince(attemptId) ?: return false
        local.saveAwaySince(attemptId, null)
        val awayMs = now.toEpochMilli() - parseServerTime(since).toEpochMilli()
        val counts = awayMs >= MIN_AWAY_MS
        if (counts) {
            local.savePendingViolations(
                attemptId,
                local.pendingViolations(attemptId) + ExamViolationRequest(KIND, since, awayMs),
            )
        }
        return counts
    }

    /** Leaving no longer counts (the Ujian was submitted or locked): forget the open leave. */
    fun cancelLeave() = local.saveAwaySince(attemptId, null)

    /**
     * Sends the waiting reports one by one, oldest first. The list is read again from the phone each time
     * and only the report that was taken or refused for good is removed, so a report added meanwhile stays
     * (A2b review ulang 3).
     */
    suspend fun send(): ViolationSend =
        mutex.withLock {
            var outcome: ViolationSend = ViolationSend.Nothing
            var report = local.pendingViolations(attemptId).firstOrNull()
            while (report != null) {
                outcome = outcomeOf(repository.reportViolation(attemptId, deviceToken(), report))
                if (outcome is ViolationSend.Reported || outcome is ViolationSend.Refused) remove(report)
                val goOn = outcome is ViolationSend.Reported && outcome.result.status != SUBMITTED
                report = if (goOn) local.pendingViolations(attemptId).firstOrNull() else null
            }
            outcome
        }

    private fun outcomeOf(result: Result<ExamViolationResult>): ViolationSend {
        val error = result.exceptionOrNull()
        return when {
            error == null -> ViolationSend.Reported(result.getOrThrow())
            error is ApiError && (error.status == UNPROCESSABLE || error.code != null) -> ViolationSend.Refused(error)
            error is ApiError -> ViolationSend.Later
            else -> ViolationSend.Offline
        }
    }

    private fun remove(report: ExamViolationRequest) {
        val waiting = local.pendingViolations(attemptId)
        val index = waiting.indexOf(report)
        if (index >= 0) local.savePendingViolations(attemptId, waiting.filterIndexed { i, _ -> i != index })
    }

    private companion object {
        const val KIND = "page_left"
        const val MIN_AWAY_MS = 2_000L
        const val SUBMITTED = "submitted"
        const val UNPROCESSABLE = 422
    }
}
