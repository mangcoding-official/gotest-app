package com.gotest.app.feature.account.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Profil of API module F (T-033, API-CONTRACTS "API Android modul F"): the same props as the web's
 * Profile/Edit, built by ProfilePageData. Name, email, photo, child account and Mitra come from `/akun`.
 */

/** Ringkasan pengerjaan; each change is the percent against the 30 days before, or null. */
@Serializable
data class ProfileStats(
    @SerialName("questionsDone") val questionsDone: Int = 0,
    @SerialName("accuracy") val accuracy: Int = 0,
    @SerialName("packagesDone") val packagesDone: Int = 0,
    @SerialName("questionsDoneChange") val questionsDoneChange: Int? = null,
    @SerialName("accuracyChange") val accuracyChange: Int? = null,
    @SerialName("packagesDoneChange") val packagesDoneChange: Int? = null,
)

/** A browser or a phone signed in to the account (DEC-102). */
@Serializable
data class ProfileSession(
    /** A browser's session id, or `app-{token id}`. */
    @SerialName("id") val id: String,
    /** `browser` or `app`. */
    @SerialName("kind") val kind: String,
    @SerialName("device") val device: String,
    @SerialName("ip") val ip: String? = null,
    @SerialName("lastActiveAt") val lastActiveAt: String? = null,
    @SerialName("current") val current: Boolean = false,
) {
    val isApp: Boolean get() = kind == "app"
}

/** `GET /profil`. */
@Serializable
data class ProfilePage(
    @SerialName("googleLinked") val googleLinked: Boolean = false,
    /** The WhatsApp number in display form ("0812-3456-7890"), or null. */
    @SerialName("phone") val phone: String? = null,
    /** Masuk dengan Nomor HP is on: a number may be added or changed. */
    @SerialName("phoneSignIn") val phoneSignIn: Boolean = false,
    /** A new email must be proven with a code sent to it. */
    @SerialName("emailCode") val emailCode: Boolean = false,
    @SerialName("emailVerified") val emailVerified: Boolean = false,
    /** `email`, or `whatsapp` when the password link goes by WhatsApp. */
    @SerialName("passwordLinkChannel") val passwordLinkChannel: String = "email",
    /** Active Mitra where this account is the only manager. */
    @SerialName("soleManagerOf") val soleManagerOf: List<String> = emptyList(),
    @SerialName("stats") val stats: ProfileStats? = null,
    @SerialName("sessions") val sessions: List<ProfileSession> = emptyList(),
) {
    val linkByWhatsApp: Boolean get() = passwordLinkChannel == "whatsapp"
}

@Serializable
data class ProfileUpdate(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    /** `sd`, `smp`, `sma`, `kuliah`, `umum`, or empty while the account has none. */
    @SerialName("education_level") val educationLevel: String,
)

@Serializable
data class EmailCodeRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class EmailSave(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
)

@Serializable
data class PasswordChange(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("password") val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class PhoneCodeRequest(
    @SerialName("phone") val phone: String,
)

@Serializable
data class PhoneSave(
    @SerialName("phone") val phone: String,
    @SerialName("code") val code: String,
)

@Serializable
data class CurrentPassword(
    @SerialName("current_password") val currentPassword: String,
)

@Serializable
data class DeleteConfirmation(
    @SerialName("confirmation") val confirmation: String,
)

/** The reply of every Profil write; only some carry the extra fields. */
@Serializable
data class ProfileWriteReply(
    @SerialName("message") val message: String? = null,
    /** A new email waits for its code: where it went, masked. */
    @SerialName("emailCodeSentTo") val emailCodeSentTo: String? = null,
    /** Where the WhatsApp code went, in display form. */
    @SerialName("phoneCodeSentTo") val phoneCodeSentTo: String? = null,
    /** How many browsers and phones "Keluar dari perangkat lain" signed out. */
    @SerialName("signed_out") val signedOut: Int? = null,
)

/**
 * Profil writes of API module F. Field errors come back as ApiError with the web's field names
 * (name, email, education_level, code, photo, current_password, password, password_link, phone, confirmation).
 */
interface ProfilePagesRepository {
    suspend fun page(): Result<ProfilePage>

    suspend fun update(update: ProfileUpdate): Result<ProfileWriteReply>

    suspend fun resendEmailCode(email: String): Result<ProfileWriteReply>

    suspend fun saveEmail(
        email: String,
        code: String,
    ): Result<ProfileWriteReply>

    /** A JPEG of the cropped photo, sent as `photo` (`avatar.jpg`). */
    suspend fun uploadPhoto(jpeg: ByteArray): Result<ProfileWriteReply>

    suspend fun deletePhoto(): Result<ProfileWriteReply>

    suspend fun changePassword(change: PasswordChange): Result<ProfileWriteReply>

    suspend fun sendPasswordLink(): Result<ProfileWriteReply>

    suspend fun sendPhoneCode(phone: String): Result<ProfileWriteReply>

    suspend fun savePhone(
        phone: String,
        code: String,
    ): Result<ProfileWriteReply>

    suspend fun removePhone(): Result<ProfileWriteReply>

    suspend fun signOutOtherDevices(currentPassword: String): Result<ProfileWriteReply>

    suspend fun deleteAccount(confirmation: String): Result<ProfileWriteReply>
}
