package com.gotest.app.core.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The other ways of signing in of API module A (API-CONTRACTS "API Android modul A"): a code to the email,
 * a WhatsApp code to the phone number, Masuk Anak with the family code, and resetting the password.
 */

@Serializable
data class EmailCodeSentReply(
    @SerialName("email_code_sent_to") val sentTo: String? = null,
    @SerialName("resend_in") val resendIn: Int? = null,
)

@Serializable
data class EmailCodeSignInRequest(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class PhoneRequest(
    @SerialName("phone") val phone: String,
)

@Serializable
data class PhoneCodeSentReply(
    /** In display form ("0812-3456-7890"). */
    @SerialName("phone_code_sent_to") val sentTo: String? = null,
)

@Serializable
data class PhoneSignInRequest(
    @SerialName("phone") val phone: String,
    @SerialName("code") val code: String,
    @SerialName("device_name") val deviceName: String,
)

/** `POST /auth/nomor-hp/daftar`: the name step of a proven new number, with the same answers as Daftar. */
@Serializable
data class PhoneSignUpRequest(
    @SerialName("registration_token") val registrationToken: String,
    @SerialName("name") val name: String,
    @SerialName("education_level") val educationLevel: String,
    @SerialName("adult") val adult: Boolean? = null,
    @SerialName("guardian_channel") val guardianChannel: String? = null,
    @SerialName("guardian_phone") val guardianPhone: String? = null,
    @SerialName("guardian_email") val guardianEmail: String? = null,
    @SerialName("mitra_link") val mitraLink: String? = null,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class FamilyCodeRequest(
    @SerialName("code") val code: String,
)

@Serializable
data class FamilyChildPayload(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
)

@Serializable
data class FamilyReply(
    @SerialName("children") val children: List<FamilyChildPayload> = emptyList(),
)

@Serializable
data class ChildSignInRequest(
    @SerialName("code") val code: String,
    @SerialName("child_id") val childId: Long,
    @SerialName("pin") val pin: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class ResetPasswordRequest(
    @SerialName("token") val token: String,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)
