package com.gotest.app.feature.ruang.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `POST /gabung`: the typed code, cleaned (no spaces, upper case). */
@Serializable
data class JoinCodeLookup(
    @SerialName("code") val code: String,
)

/** `GET /gabung/{code}`: a class code or an event code, and whether this account can join. */
@Serializable
data class JoinCodePage(
    @SerialName("code") val code: String,
    /**
     * Event code: `invalid`, `expired`, `full`, `guest`, `ready`. Class code: `invalid`, `inactive`,
     * `blocked`, `full`, `joined`, `guest`, `ready`.
     */
    @SerialName("state") val state: String,
    @SerialName("organizationName") val organizationName: String? = null,
    @SerialName("label") val label: String? = null,
    /** `event` or `class`. */
    @SerialName("kind") val kind: String,
    @SerialName("className") val className: String? = null,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class JoinedMitra(
    @SerialName("code") val code: String,
    /** `siswa` for a class code; an event code made by the account's own staff may give `pengelola`/`pengajar`. */
    @SerialName("role") val role: String,
    @SerialName("class") val className: String? = null,
)

/** `POST /gabung/{code}` and `POST /undangan/{token}`. */
@Serializable
data class JoinResult(
    @SerialName("message") val message: String? = null,
    @SerialName("joined") val joined: JoinedMitra,
)

/** `GET /undangan/{token}`. */
@Serializable
data class InvitationPage(
    @SerialName("token") val token: String,
    /** `invalid`, `used`, `expired`, `guest`, `other_account`, `ready`. */
    @SerialName("state") val state: String,
    @SerialName("organizationName") val organizationName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("byPhone") val byPhone: Boolean = false,
    @SerialName("role") val role: String? = null,
    @SerialName("organizationType") val organizationType: String? = null,
    /** Set when this account must prove its email with a code first (F-c FC3). */
    @SerialName("emailProof") val emailProof: EmailProof? = null,
)

@Serializable
data class EmailProof(
    @SerialName("sentTo") val sentTo: String? = null,
    @SerialName("codeSent") val codeSent: Boolean? = null,
    @SerialName("resendIn") val resendIn: Int? = null,
)

/** `POST /undangan/{token}/kode`: the masked address, or null when this account needs no code. */
@Serializable
data class InvitationCodeSent(
    @SerialName("message") val message: String? = null,
    @SerialName("emailCodeSentTo") val emailCodeSentTo: String? = null,
)
