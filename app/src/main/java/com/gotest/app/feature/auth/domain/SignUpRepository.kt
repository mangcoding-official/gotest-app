package com.gotest.app.feature.auth.domain

import kotlinx.coroutines.flow.Flow

/**
 * Daftar with email and the parent's approval the web has and the app gains in T-018 (Aturan 2.14, 2.15,
 * 2.18), against API module A since T-026 A2a.
 */
interface SignUpRepository {
    /** The sign-up page's questions and notices; a failure is shown with "Coba Lagi" (Ardy's review of A2a, 3). */
    suspend fun registrationInfo(): Result<RegistrationInfo>

    /** "Bukan, daftar biasa": the Mitra code is dropped and Daftar becomes a plain sign-up. */
    suspend fun dropJoinCode()

    /** A sign-up still waiting for its email code after Android closed the app (T-026 required 10). */
    suspend fun pendingEmailCode(): SignUpResult.EmailCodeSent?

    suspend fun register(form: SignUpForm): Result<SignUpResult>

    suspend fun confirmEmailCode(code: String): Result<SignUpResult>

    /** Sends the email code again; returns the seconds until the next resend. */
    suspend fun resendEmailCode(): Result<Int>

    fun observeConsent(): Flow<ConsentState?>

    /**
     * Reads the waiting page again (T-026 required 13), as the web does when the page loads; false once the
     * parent approved, so Beranda can open.
     */
    suspend fun refreshConsent(): Result<Boolean>

    /** "Kirim ulang tautan"; returns the confirmation shown to the student. */
    suspend fun resendConsent(): Result<String>

    suspend fun changeConsentContact(
        channel: GuardianChannel,
        contact: String,
    ): Result<String>
}
