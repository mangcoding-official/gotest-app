package com.gotest.app.core.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Daftar with email and the parent's approval in API module A (API-CONTRACTS "API Android modul A"):
 * `POST /auth/daftar/info`, `/auth/daftar`, `/auth/daftar/kode`, and `/persetujuan-orang-tua`.
 */

@Serializable
data class MitraLinkRequest(
    @SerialName("mitra_link") val mitraLink: String? = null,
)

@Serializable
data class GuardianQuestionPayload(
    @SerialName("age") val age: Int,
    /** Education level codes (`sd`, `smp`, `sma`, …). */
    @SerialName("levels") val levels: List<String> = emptyList(),
    /** `whatsapp` and, while email codes are on, `email`. */
    @SerialName("channels") val channels: List<String> = emptyList(),
)

@Serializable
data class JoinNoticePayload(
    /** `class` or `event`. */
    @SerialName("kind") val kind: String,
    @SerialName("organization") val organization: String,
    @SerialName("label") val label: String,
)

/** `POST /auth/daftar/info`: what Daftar asks, for the Mitra link it came through (if any). */
@Serializable
data class SignUpInfoReply(
    @SerialName("guardian_question") val guardianQuestion: GuardianQuestionPayload? = null,
    @SerialName("invited_email") val invitedEmail: String? = null,
    @SerialName("join_notice") val joinNotice: JoinNoticePayload? = null,
)

/** `POST /auth/daftar`; the parent's fields only when the age question was answered "Belum". */
@Serializable
data class SignUpRequest(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("education_level") val educationLevel: String,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("guardian_channel") val guardianChannel: String? = null,
    @SerialName("guardian_phone") val guardianPhone: String? = null,
    @SerialName("guardian_email") val guardianEmail: String? = null,
    @SerialName("mitra_link") val mitraLink: String? = null,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class SignUpCodeRequest(
    @SerialName("registration_token") val registrationToken: String,
    @SerialName("code") val code: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class SignUpResendRequest(
    @SerialName("registration_token") val registrationToken: String,
    @SerialName("device_name") val deviceName: String,
)

/** The waiting page's state (`GuardianConsents::waitingPayload`, the web's camelCase `expiresAt`). */
@Serializable
data class ConsentReply(
    @SerialName("message") val message: String? = null,
    @SerialName("waiting") val waiting: Boolean = true,
    /** `pending`, `expired`, `declined`, `revoked`, or `approved`/`not_needed` once it no longer waits. */
    @SerialName("status") val status: String? = null,
    @SerialName("contact") val contact: String? = null,
    @SerialName("channel") val channel: String = CHANNEL_WHATSAPP,
    @SerialName("channels") val channels: List<String> = emptyList(),
    /** `queued`, `sent`, `limited`, or `failed`. */
    @SerialName("delivery") val delivery: String = "failed",
    @SerialName("expiresAt") val expiresAt: String? = null,
) {
    companion object {
        const val CHANNEL_WHATSAPP = "whatsapp"
        const val CHANNEL_EMAIL = "email"
    }
}

@Serializable
data class ConsentContactRequest(
    @SerialName("guardian_channel") val guardianChannel: String,
    @SerialName("guardian_phone") val guardianPhone: String? = null,
    @SerialName("guardian_email") val guardianEmail: String? = null,
)
