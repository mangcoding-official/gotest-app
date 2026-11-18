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

    /** Sends the waiting reports one by one, oldest first. */
    suspend fun send(): ViolationSend =
        mutex.withLock {
            var outcome: ViolationSend = ViolationSend.Nothing
            var waiting = local.pendingViolations(attemptId)
            while (waiting.isNotEmpty() && outcome !is ViolationSend.Offline && outcome !is ViolationSend.Refused) {
                val result = repository.reportViolation(attemptId, deviceToken(), waiting.first())
                val error = result.exceptionOrNull()
                outcome =
                    when {
                        result.isSuccess -> ViolationSend.Reported(result.getOrThrow())
                        error is ApiError -> ViolationSend.Refused(error)
                        else -> ViolationSend.Offline
                    }
                // Taken, or refused for good: either way this report is done.
                if (outcome !is ViolationSend.Offline) waiting = waiting.drop(1)
                local.savePendingViolations(attemptId, waiting)
                if ((outcome as? ViolationSend.Reported)?.result?.status == SUBMITTED) waiting = emptyList()
            }
            outcome
        }

    private companion object {
        const val KIND = "page_left"
        const val MIN_AWAY_MS = 2_000L
        const val SUBMITTED = "submitted"
    }
}
