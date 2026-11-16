package com.gotest.app.feature.auth.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.ConsentDelivery
import com.gotest.app.feature.auth.domain.ConsentState
import com.gotest.app.feature.auth.domain.ConsentStatus
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.domain.RegistrationInfo
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy data until the API exists (README): Daftar sends email code `123456`; an account under 18 at
 * SD/SMP/SMA waits for the parent (WhatsApp or email); gilangpermana@mail.com is taken; the consent link
 * goes out at most 3 times an hour. Texts and limits are the web's.
 */
@Singleton
class FakeSignUpRepository
    @Inject
    constructor(
        private val session: SessionStore,
    ) : SignUpRepository {
        private var pendingSignUp: SignUpForm? = null
        private var emailCodeWrong = 0
        private val consent = MutableStateFlow<ConsentState?>(null)
        private val consentSends = mutableListOf<Long>()
        private var info =
            RegistrationInfo(
                guardianQuestion =
                    GuardianQuestion(
                        age = 18,
                        levels = setOf(EducationLevel.Sd, EducationLevel.Smp, EducationLevel.SmaSmk),
                        channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email),
                    ),
                emailCode = true,
                joinNotice = null,
            )

        override suspend fun registrationInfo(): RegistrationInfo = info

        override suspend fun dropJoinCode() {
            info = info.copy(joinNotice = null)
        }

        override suspend fun register(form: SignUpForm): Result<SignUpResult> {
            simulateLatency()
            if (form.email.trim().equals(TAKEN_EMAIL, ignoreCase = true)) {
                return Result.failure(AuthFlowError("email", "Email ini sudah terdaftar. Masuk dengan email kamu."))
            }
            pendingSignUp = form
            emailCodeWrong = 0
            return Result.success(
                SignUpResult.EmailCodeSent(sentTo = maskEmail(form.email.trim()), resendInSeconds = RESEND_SECONDS),
            )
        }

        override suspend fun confirmEmailCode(code: String): Result<SignUpResult> {
            simulateLatency()
            val form = pendingSignUp
            return when {
                form == null -> {
                    Result.failure(
                        AuthFlowError(
                            "email",
                            "Waktu untuk memasukkan kode sudah habis. Tekan Daftar lagi untuk meminta kode baru.",
                        ),
                    )
                }

                emailCodeWrong >= MAX_CODE_TRIES -> {
                    Result.failure(AuthFlowError("code", TOO_MANY_TRIES))
                }

                code != EMAIL_CODE -> {
                    emailCodeWrong++
                    val left = MAX_CODE_TRIES - emailCodeWrong
                    val message = if (left <= 0) TOO_MANY_TRIES else "Kode salah. Sisa $left kali percobaan."
                    Result.failure(AuthFlowError("code", message))
                }

                else -> {
                    pendingSignUp = null
                    Result.success(finishSignUp(FAKE_EMAIL_TOKEN, form.guardian))
                }
            }
        }

        override suspend fun resendEmailCode(): Result<Int> {
            simulateLatency()
            emailCodeWrong = 0
            return Result.success(RESEND_SECONDS)
        }

        override fun observeConsent(): Flow<ConsentState?> = consent.asStateFlow()

        override suspend fun resendConsent(): Result<String> {
            simulateLatency()
            val state = consent.value ?: return Result.failure(AuthFlowError("resend", "Tautan belum bisa dikirim."))
            return sendConsent(state.channel, state.contact.orEmpty()).map {
                "Tautan baru dikirim ke ${channelWord(state.channel)} ${state.contact}."
            }
        }

        override suspend fun changeConsentContact(
            channel: GuardianChannel,
            contact: String,
        ): Result<String> {
            simulateLatency()
            val masked =
                when (channel) {
                    GuardianChannel.WhatsApp -> PhoneNumber.normalize(contact)?.let(::maskPhone)
                    GuardianChannel.Email -> contact.trim().takeIf(EMAIL::matches)?.let(::maskEmail)
                }
            return when {
                masked != null -> {
                    sendConsent(channel, masked).map { "Tautan dikirim ke ${channelWord(channel)} $masked." }
                }

                channel == GuardianChannel.WhatsApp && contact.isBlank() -> {
                    fail("guardian_phone", "Tulis nomor WhatsApp orang tua atau wali.")
                }

                channel == GuardianChannel.WhatsApp -> {
                    fail("guardian_phone", "Tulis nomor HP Indonesia yang aktif di WhatsApp, misalnya 0812 3456 7890.")
                }

                contact.isBlank() -> {
                    fail("guardian_email", "Tulis email orang tua atau wali.")
                }

                else -> {
                    fail("guardian_email", "Tulis email yang benar, misalnya nama@contoh.id.")
                }
            }
        }

        private fun sendConsent(
            channel: GuardianChannel,
            masked: String,
        ): Result<Unit> {
            val now = System.currentTimeMillis()
            consentSends.removeAll { now - it > HOUR_MILLIS }
            if (consentSends.size >= MAX_CONSENT_SENDS) {
                val minutes = ((HOUR_MILLIS - (now - consentSends.first())) / MINUTE_MILLIS + 1).toInt()
                return Result.failure(
                    AuthFlowError("resend", "Tautan sudah dikirim beberapa kali. Coba lagi dalam $minutes menit."),
                )
            }
            consentSends += now
            consent.value =
                consent.value?.copy(
                    status = ConsentStatus.Pending,
                    contact = masked,
                    channel = channel,
                    delivery = ConsentDelivery.Sent,
                    expiresAt = Instant.ofEpochMilli(now).plus(CONSENT_DAYS, ChronoUnit.DAYS),
                )
            return Result.success(Unit)
        }

        /** Saves the session; an under-age answer locks the account until the parent approves. */
        fun finishSignUp(
            token: String,
            guardian: GuardianAnswer?,
        ): SignUpResult {
            session.save(token)
            if (guardian == null || guardian.adult) return SignUpResult.SignedIn
            val channel = guardian.channel ?: GuardianChannel.WhatsApp
            val contact = guardian.contact.orEmpty()
            val masked =
                if (channel ==
                    GuardianChannel.WhatsApp
                ) {
                    PhoneNumber.normalize(contact)?.let(::maskPhone)
                } else {
                    maskEmail(contact)
                }
            consent.value =
                ConsentState(
                    status = ConsentStatus.Pending,
                    contact = masked,
                    channel = channel,
                    channels = info.guardianQuestion?.channels.orEmpty(),
                    delivery = ConsentDelivery.Sent,
                    expiresAt = Instant.now().plus(CONSENT_DAYS, ChronoUnit.DAYS),
                )
            return SignUpResult.AwaitingConsent
        }

        private fun fail(
            field: String,
            message: String,
        ): Result<String> = Result.failure(AuthFlowError(field, message))

        private companion object {
            const val EMAIL_CODE = "123456"
            const val RESEND_SECONDS = 60
            const val MAX_CODE_TRIES = 5
            const val TOO_MANY_TRIES = "Terlalu banyak percobaan. Minta kode baru."
            const val FAKE_EMAIL_TOKEN = "fake-email-token"
            const val TAKEN_EMAIL = "gilangpermana@mail.com"
            const val MAX_CONSENT_SENDS = 3
            const val CONSENT_DAYS = 7L
            const val MINUTE_MILLIS = 60_000L
            const val HOUR_MILLIS = 60 * MINUTE_MILLIS
            val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }

private const val MASK_HEAD = 4
private const val MASK_TAIL = 3

private fun channelWord(channel: GuardianChannel) =
    if (channel ==
        GuardianChannel.WhatsApp
    ) {
        "WhatsApp"
    } else {
        "email"
    }

/** `r***@contoh.id`, as the web masks an email. */
private fun maskEmail(email: String): String = email.take(1) + "***@" + email.substringAfter('@')

/** `0812-****-890`, as the web masks a number. */
private fun maskPhone(stored: String): String {
    val local = "0" + stored.removePrefix("62")
    return local.take(MASK_HEAD) + "-****-" + local.takeLast(MASK_TAIL)
}
