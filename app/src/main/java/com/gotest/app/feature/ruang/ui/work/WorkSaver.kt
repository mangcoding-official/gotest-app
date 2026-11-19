package com.gotest.app.feature.ruang.ui.work

import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamAnswerQueue
import com.gotest.app.feature.ruang.data.SaveOutcome
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.domain.ExamRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What one attempt to send the queued answers ended with. */
sealed interface FlushResult {
    /** Sent (or nothing to send); [reply] is null when the queue was already empty. */
    data class Sent(
        val reply: ExamAnswersSaved?,
        val outcome: SaveOutcome?,
    ) : FlushResult

    /** Another send is running; the periodic save skips instead of waiting. */
    data object Busy : FlushResult

    data object Offline : FlushResult

    data class Refused(
        val error: ApiError,
    ) : FlushResult
}

/**
 * Sends queued answers one request at a time (Work.tsx `flush`). Unlike the web, a caller that needs
 * everything sent (Kumpulkan, time up) waits for a running send instead of failing with "belum
 * terkirim" (T-026 web finding).
 */
class WorkSaver(
    private val attemptId: Long,
    private val repository: ExamRepository,
    private val queue: ExamAnswerQueue,
    private val deviceToken: () -> String?,
    private val numberOf: (Long) -> Int?,
) {
    private val mutex = Mutex()

    val isEmpty: Boolean get() = queue.isEmpty

    val pendingIds: Set<Long> get() = queue.pending.keys

    suspend fun flush(wait: Boolean): FlushResult {
        if (!wait && mutex.isLocked) return FlushResult.Busy
        return mutex.withLock { send() }
    }

    private suspend fun send(): FlushResult {
        val batch = queue.batch()
        if (batch.isEmpty()) return FlushResult.Sent(null, null)
        val result = repository.saveAnswers(attemptId, deviceToken(), queue.uploads(batch))
        val reply = result.getOrNull()
        val error = result.exceptionOrNull()
        return when {
            reply != null -> {
                FlushResult.Sent(reply, queue.applyReply(batch, reply, numberOf))
            }

            error is ApiError -> {
                // Refused for good (for example a bad shape): sending it again would fail the same way.
                if (error.status == UNPROCESSABLE && error.code == VALIDATION) queue.dropBatch(batch)
                FlushResult.Refused(error)
            }

            else -> {
                FlushResult.Offline
            }
        }
    }

    private companion object {
        const val UNPROCESSABLE = 422
        const val VALIDATION = "validation"
    }
}
