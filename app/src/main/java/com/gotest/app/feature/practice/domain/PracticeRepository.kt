package com.gotest.app.feature.practice.domain

/*
 * API module C (T-030). A failure is a [com.gotest.app.core.network.ApiError] with the server's own `code` and
 * message (the web's text), or an IOException without a connection. Debug builds may bind a fake (src/debug);
 * release builds always bind the API.
 */
interface PracticeRepository {
    suspend fun catalog(): Result<PracticeCatalogPage>

    /** The plan and the Gratis quota from `GET /akun`, for the start sheet (web `access`, `freePlan`). */
    suspend fun startAccess(): Result<StartAccess>

    /** A new attempt, or Latihan Lagi; [timerEnabled] only for a package whose timer is optional. */
    suspend fun start(
        packageId: Long,
        timerEnabled: Boolean?,
    ): Result<PracticeAttempt>

    suspend fun attempt(attemptId: Long): Result<PracticeAttempt>

    suspend fun activate(
        attemptId: Long,
        itemId: Long,
    ): Result<ActiveQuestion>

    /** [questionId] is the item's `question_id`, as the web sends it. */
    suspend fun answer(
        attemptId: Long,
        questionId: Long,
        answer: AnswerData,
    ): Result<Unit>

    suspend fun submit(attemptId: Long): Result<PracticeAttempt>

    suspend fun result(attemptId: Long): Result<PracticeResult>
}
