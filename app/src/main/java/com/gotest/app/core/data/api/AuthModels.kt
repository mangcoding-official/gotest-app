package com.gotest.app.core.data.api

import com.gotest.app.feature.practice.domain.FreePlan
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.domain.MitraMembership
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * API module A (API-CONTRACTS "API Android modul A"): the sign-in reply every way of signing in shares, the
 * account (`GET /akun`), and the reference data (`GET /referensi`). Every key named by @SerialName (T-026
 * required 9): the contract mixes snake_case at the top with the web's camelCase inside `access`.
 */

/** `user` of the sign-in reply and of `/akun`. */
@Serializable
data class ApiUser(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("email") val email: String? = null,
    /** In display form ("0812-3456-7890"), or null. */
    @SerialName("phone") val phone: String? = null,
    @SerialName("education_level") val educationLevel: String? = null,
)

/**
 * The reply of every sign-in and sign-up: the token and who signed in. A sign-up that waits for its email code
 * has no token yet (202 `email_code_sent`); one whose code could not be sent has a token and
 * `code: "email_verification_pending"` (DEC-154).
 */
@Serializable
data class SignInReply(
    @SerialName("token") val token: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("user") val user: ApiUser? = null,
    @SerialName("is_child") val isChild: Boolean = false,
    /** `not_needed`, `approved`, `pending`, `declined`, or `revoked` (the last three lock the account). */
    @SerialName("consent") val consent: String = CONSENT_NOT_NEEDED,
    @SerialName("code") val code: String? = null,
    @SerialName("registration_token") val registrationToken: String? = null,
    @SerialName("sent_to") val sentTo: String? = null,
    @SerialName("resend_in") val resendIn: Int? = null,
    /** Sign-up: the Mitra its link joined. */
    @SerialName("joined") val joined: JoinedMitra? = null,
    /** Google: the account was linked to Google now. */
    @SerialName("linked") val linked: Boolean? = null,
) {
    /** The parent has not approved, declined, or took the approval back: only the waiting page opens. */
    val consentLocked: Boolean get() = consent in LOCKED

    companion object {
        const val CONSENT_NOT_NEEDED = "not_needed"
        const val EMAIL_CODE_SENT = "email_code_sent"
        const val EMAIL_VERIFICATION_PENDING = "email_verification_pending"
        private val LOCKED = setOf("pending", "declined", "revoked")
    }
}

@Serializable
data class SignInRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class EmailRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class MessageReply(
    @SerialName("message") val message: String? = null,
)

/** `access` of `/akun`, the web's shape (`PremiumAccess::payload`). */
@Serializable
data class AccountAccessPayload(
    /** `active` or `expired`. */
    @SerialName("status") val status: String = "expired",
    @SerialName("label") val label: String? = null,
    @SerialName("plan") val plan: String? = null,
    @SerialName("startsAt") val startsAt: String? = null,
    @SerialName("endsAt") val endsAt: String? = null,
    @SerialName("remainingDays") val remainingDays: Int = 0,
    /** `personal` or `mitra`. */
    @SerialName("source") val source: String? = null,
    @SerialName("partner") val partner: String? = null,
)

@Serializable
data class TokenInfo(
    @SerialName("name") val name: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

/** `GET /akun`: the account as the web shares it with every page. */
@Serializable
data class AccountPage(
    @SerialName("user") val user: ApiUser,
    @SerialName("is_child") val isChild: Boolean = false,
    @SerialName("consent") val consent: String = SignInReply.CONSENT_NOT_NEEDED,
    @SerialName("access") val access: AccountAccessPayload = AccountAccessPayload(),
    @SerialName("free_plan") val freePlan: FreePlan? = null,
    @SerialName("organizations") val organizations: List<MitraMembership> = emptyList(),
    @SerialName("token") val token: TokenInfo? = null,
)

@Serializable
data class LevelOption(
    @SerialName("value") val value: String,
    @SerialName("label") val label: String,
)

/** `GET /referensi`: what the sign-in and sign-up screens offer, as the server has it on (T-026 required 12). */
@Serializable
data class Reference(
    @SerialName("education_levels") val educationLevels: List<LevelOption> = emptyList(),
    @SerialName("phone_sign_in") val phoneSignIn: Boolean = false,
    @SerialName("google_sign_in") val googleSignIn: Boolean = false,
    @SerialName("email_code") val emailCode: Boolean = false,
    @SerialName("min_app_version") val minAppVersion: Int? = null,
)
