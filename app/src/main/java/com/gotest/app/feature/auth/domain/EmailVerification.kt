package com.gotest.app.feature.auth.domain

import com.gotest.app.feature.ruang.domain.JoinedMitra
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /akun/verifikasi-email` and the reply of sending the code again (DEC-154, API "Akun menunggu verifikasi
 * email"): an account made while its sign-up code could not be sent waits here until its email is proven.
 */
@Serializable
data class EmailVerificationState(
    @SerialName("waiting") val waiting: Boolean = true,
    /** The account's email in full, so a typo shows. */
    @SerialName("email") val email: String? = null,
    @SerialName("sent_to") val sentTo: String? = null,
    /** Seconds until "Kirim ulang kode" may be pressed. */
    @SerialName("resend_in") val resendIn: Int = 0,
    /** The latest code could not be sent by the server. */
    @SerialName("send_failed") val sendFailed: Boolean = false,
)

/** `POST /akun/verifikasi-email/kode`: the account is open. */
@Serializable
data class EmailVerified(
    /** Why the Mitra link of the sign-up was no longer usable, or null. */
    @SerialName("message") val message: String? = null,
    @SerialName("joined") val joined: JoinedMitra? = null,
    /** `pending`: the parent's approval is asked now (the waiting page for it opens next). */
    @SerialName("consent") val consent: String? = null,
) {
    companion object {
        const val CONSENT_PENDING = "pending"
    }
}

@Serializable
data class EmailVerificationCode(
    @SerialName("code") val code: String,
)

/** The three routes open while the account waits; a wrong or late code is an ApiError on `code`. */
interface EmailVerificationRepository {
    suspend fun state(): Result<EmailVerificationState>

    suspend fun resend(): Result<EmailVerificationState>

    suspend fun verify(code: String): Result<EmailVerified>
}
