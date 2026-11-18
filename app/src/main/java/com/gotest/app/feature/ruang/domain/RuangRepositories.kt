package com.gotest.app.feature.ruang.domain

/*
 * API module G (T-029). A failure is a [com.gotest.app.core.network.ApiError] with the server's own
 * `code` and message (the same text as the web), or an IOException when the phone is offline.
 * Debug builds may bind fakes (src/debug); release builds always bind the API.
 */

/** The student's Mitra spaces, "Gabung ke kelas", and invitations. */
interface RuangRepository {
    /** The account's Mitra, in the server's order (`organizations` in `GET /akun`). */
    suspend fun memberships(): Result<List<MitraMembership>>

    /** When this phone's sign-in ends (`token.expires_at` in `GET /akun`), or null without a limit. */
    suspend fun signInEndsAt(): Result<String?>

    suspend fun ruang(code: String): Result<RuangPage>

    /** The typed code, cleaned by the server; 422 with the web's message when there is no such code. */
    suspend fun lookupCode(typed: String): Result<String>

    suspend fun joinCode(code: String): Result<JoinCodePage>

    suspend fun join(code: String): Result<JoinedMitra>

    suspend fun invitation(token: String): Result<InvitationPage>

    /** Sends the email code an unproven email needs first; the masked address, or null when not needed. */
    suspend fun sendInvitationCode(token: String): Result<String?>

    /** [code]: the 6-digit email code, only when the invitation page asked for an email proof. */
    suspend fun acceptInvitation(
        token: String,
        code: String?,
    ): Result<JoinedMitra>
}

/** Ujian in a Mitra space (DEC-148). [deviceToken] goes in `X-Exam-Device`. */
interface ExamRepository {
    suspend fun schedules(code: String): Result<ExamListPage>

    suspend fun detail(
        code: String,
        scheduleId: Long,
    ): Result<ExamDetail>

    suspend fun start(
        code: String,
        scheduleId: Long,
    ): Result<ExamStart>

    suspend fun work(
        code: String,
        attemptId: Long,
        deviceToken: String?,
    ): Result<ExamWorkPage>

    suspend fun saveAnswers(
        attemptId: Long,
        deviceToken: String?,
        items: List<ExamAnswerUpload>,
    ): Result<ExamAnswersSaved>

    suspend fun reportViolation(
        attemptId: Long,
        deviceToken: String?,
        violation: ExamViolationRequest,
    ): Result<ExamViolationResult>

    /** Moves the attempt to this phone; the other device gets 409 `other_device` from then on. */
    suspend fun takeOver(attemptId: Long): Result<String>

    suspend fun submit(
        attemptId: Long,
        deviceToken: String?,
    ): Result<Unit>

    suspend fun result(
        code: String,
        attemptId: Long,
    ): Result<ExamResultPage>
}
