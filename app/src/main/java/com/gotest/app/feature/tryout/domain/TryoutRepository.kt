package com.gotest.app.feature.tryout.domain

import com.gotest.app.feature.practice.domain.AnswerData

/*
 * API module D (T-032). A failure is a [com.gotest.app.core.network.ApiError] with the server's own `code` and
 * message (the web's text), or an IOException without a connection. Debug builds may bind a fake (src/debug);
 * release builds always bind the API.
 */
@Suppress("TooManyFunctions") // One function per route of the contract.
interface TryoutRepository {
    suspend fun catalog(): Result<TryoutCatalogPage>

    /** A new attempt, or the running one continued; no section runs until [begin]. */
    suspend fun start(packageId: Long): Result<TryoutAttempt>

    suspend fun attempt(attemptId: Long): Result<TryoutAttempt>

    /** Starts the next section's timer. */
    suspend fun begin(attemptId: Long): Result<TryoutAttempt>

    /** [itemId] is the `id` of the item in `question_items`, not its question. */
    suspend fun answer(
        attemptId: Long,
        itemId: Long,
        answer: AnswerData,
    ): Result<Unit>

    suspend fun flag(
        attemptId: Long,
        itemId: Long,
        flagged: Boolean,
    ): Result<Unit>

    /** Idempotent: a section already closed changes nothing. */
    suspend fun finishSection(
        attemptId: Long,
        sectionId: Long?,
    ): Result<TryoutAttempt>

    /** Akhiri seluruh Try Out; before the first section of a standalone Try Out it is cancelled (DEC-090). */
    suspend fun submit(attemptId: Long): Result<TryoutAttempt>

    suspend fun result(attemptId: Long): Result<TryoutResult>

    suspend fun session(sessionId: Long): Result<TryoutSessionPage>

    /** The server's sentence ("Anda terdaftar pada sesi ini. …"). */
    suspend fun register(sessionId: Long): Result<String?>

    suspend fun unregister(sessionId: Long): Result<String?>

    /** The session attempt, also when it is already running (continued). */
    suspend fun startSession(sessionId: Long): Result<Long>

    suspend fun sessionLeaderboard(sessionId: Long): Result<SessionLeaderboard>
}
