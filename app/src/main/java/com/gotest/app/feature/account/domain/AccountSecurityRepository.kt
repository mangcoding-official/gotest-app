package com.gotest.app.feature.account.domain

import java.time.Instant

/** A device signed in to the account (DEC-102, API-K5): a web browser or a phone with the app. */
data class ActiveSession(
    val id: String,
    val device: String,
    val ip: String?,
    val lastActiveAt: Instant?,
    val current: Boolean,
)

/** A refused request with the server's own text; [field] is the web's field name (phone, code, current_password). */
class AccountFieldError(
    val field: String,
    override val message: String,
) : Exception(message)

/**
 * The WhatsApp number on the account (DEC-123) and the devices signed in to it (DEC-102), on Profil.
 * Bound to a fake until the API contract (Jalur 5 A1g) is approved.
 */
interface AccountSecurityRepository {
    /** Whether the server offers WhatsApp codes (the web's `phoneSignIn`); adding a number needs it. */
    suspend fun isPhoneSignInAvailable(): Boolean

    /** Sends a code to a new number; returns where it went, in display form. */
    suspend fun sendPhoneCode(phone: String): Result<String>

    /** Saves the number after its code; returns the confirmation shown to the user. */
    suspend fun savePhone(
        phone: String,
        code: String,
    ): Result<String>

    /** Removes the number; refused when it is the only way in. Returns the confirmation. */
    suspend fun removePhone(): Result<String>

    suspend fun sessions(): Result<List<ActiveSession>>

    /** "Keluar dari perangkat lain" after the current password; this device stays signed in. */
    suspend fun revokeOtherSessions(currentPassword: String): Result<Unit>
}
