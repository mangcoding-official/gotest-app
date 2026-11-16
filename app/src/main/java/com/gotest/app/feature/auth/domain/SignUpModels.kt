package com.gotest.app.feature.auth.domain

import com.gotest.app.core.model.EducationLevel
import java.time.Instant

/** Where the parent's consent link goes (Aturan 2.15). */
enum class GuardianChannel { WhatsApp, Email }

/** "Usiamu sudah {age} tahun?", asked for [levels] while parental consent is on (Aturan 2.14). */
data class GuardianQuestion(
    val age: Int,
    val levels: Set<EducationLevel>,
    val channels: List<GuardianChannel>,
)

/** "Kamu akan bergabung ke …" on Daftar when the sign-up came through a Mitra code (Aturan 2.18). */
data class JoinNotice(
    val isClass: Boolean,
    val organization: String,
    val label: String,
)

/** What Daftar needs from the server before the form (API `GET /auth/daftar/info`). */
data class RegistrationInfo(
    val guardianQuestion: GuardianQuestion?,
    val emailCode: Boolean,
    val joinNotice: JoinNotice?,
)

/** The answer to the age question; [contact] is the parent's number or email when [adult] is false. */
data class GuardianAnswer(
    val adult: Boolean,
    val channel: GuardianChannel?,
    val contact: String?,
)

data class SignUpForm(
    val fullName: String,
    val email: String,
    val password: String,
    val level: EducationLevel,
    val guardian: GuardianAnswer?,
)

sealed interface SignUpResult {
    data object SignedIn : SignUpResult

    /** An under-age account is made locked and waits for the parent (Aturan 2.15). */
    data object AwaitingConsent : SignUpResult

    /** Email codes are on: the account is made after the code ("Cek email kamu"). */
    data class EmailCodeSent(
        val sentTo: String,
        val resendInSeconds: Int,
    ) : SignUpResult
}

enum class ConsentStatus { Pending, Expired, Declined, Revoked }

enum class ConsentDelivery { Queued, Sent, Limited, Failed }

/** The waiting page's state (web: Auth/GuardianConsentWaiting.tsx). [contact] is masked. */
data class ConsentState(
    val status: ConsentStatus,
    val contact: String?,
    val channel: GuardianChannel,
    val channels: List<GuardianChannel>,
    val delivery: ConsentDelivery,
    val expiresAt: Instant?,
)

/** An email link while email codes are off cannot go out again; the page asks for a number instead. */
val ConsentState.emailClosed: Boolean get() = channel == GuardianChannel.Email && GuardianChannel.Email !in channels

/** Until tomorrow (WIB) a held-back contact takes no more links, so sending again is not offered. */
val ConsentState.canResend: Boolean get() = delivery != ConsentDelivery.Limited && !emailClosed

/** "kontak" when email is a way to reach the parent, otherwise "nomor". */
val ConsentState.offersEmail: Boolean get() = GuardianChannel.Email in channels || channel == GuardianChannel.Email
