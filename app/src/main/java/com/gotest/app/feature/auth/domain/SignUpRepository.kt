package com.gotest.app.feature.auth.domain

import kotlinx.coroutines.flow.Flow

/**
 * Daftar with email and the parent's approval the web has and the app gains in T-018 (Aturan 2.14, 2.15,
 * 2.18). Bound to a fake until the API contract (Jalur 5 A1) is approved.
 */
interface SignUpRepository {
    suspend fun registrationInfo(): RegistrationInfo

    /** "Bukan, daftar biasa": the Mitra code is dropped and Daftar becomes a plain sign-up. */
    suspend fun dropJoinCode()

    suspend fun register(form: SignUpForm): Result<SignUpResult>

    suspend fun confirmEmailCode(code: String): Result<SignUpResult>

    /** Sends the email code again; returns the seconds until the next resend. */
    suspend fun resendEmailCode(): Result<Int>

    fun observeConsent(): Flow<ConsentState?>

    /** "Kirim ulang tautan"; returns the confirmation shown to the student. */
    suspend fun resendConsent(): Result<String>

    suspend fun changeConsentContact(
        channel: GuardianChannel,
        contact: String,
    ): Result<String>
}
