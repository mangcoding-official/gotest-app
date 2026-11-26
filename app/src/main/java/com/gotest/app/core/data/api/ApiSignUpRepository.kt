package com.gotest.app.core.data.api

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.network.apiCall
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.core.session.SignUpJoin
import com.gotest.app.feature.auth.domain.ConsentDelivery
import com.gotest.app.feature.auth.domain.ConsentState
import com.gotest.app.feature.auth.domain.ConsentStatus
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.domain.JoinNotice
import com.gotest.app.feature.auth.domain.RegistrationInfo
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daftar with email and the parent's approval against API module A. The Mitra link opened before signing in
 * goes along as `mitra_link` (T-026 required 4), and a sign-up waiting for its email code keeps its
 * `registration_token` outside memory (required 10). A refusal is the server's ApiError, with the web's texts.
 */
@Singleton
class ApiSignUpRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
        private val acceptor: SignInAcceptor,
        private val drafts: SignUpDraftStore,
        private val options: SignInOptionsRepository,
        private val signals: SessionSignals,
        private val join: SignUpJoin = SignUpJoin(),
    ) : SignUpRepository {
        private val api = retrofit.create(SignUpApi::class.java)
        private val consent = MutableStateFlow<ConsentState?>(null)

        override suspend fun registrationInfo(): RegistrationInfo {
            val emailCode = options.options().emailCode
            val info = apiCall(json) { api.signUpInfo(MitraLinkRequest(drafts.mitraLink)) }.getOrNull()
            // The staff dialog after joining names the Mitra (A2a-1); the sign-up reply only has its code.
            info?.joinNotice?.organization?.let { join.mitraName = it }
            return RegistrationInfo(
                guardianQuestion = info?.guardianQuestion?.toQuestion(),
                emailCode = emailCode,
                joinNotice = info?.joinNotice?.toNotice(),
            )
        }

        override suspend fun dropJoinCode() {
            drafts.mitraLink = null
        }

        override suspend fun pendingEmailCode(): SignUpResult.EmailCodeSent? =
            drafts.emailCode?.let { SignUpResult.EmailCodeSent(sentTo = it.sentTo, resendInSeconds = 0) }

        override suspend fun register(form: SignUpForm): Result<SignUpResult> {
            drafts.clearEmailCode()
            val guardian = form.guardian
            val underAge = guardian?.adult == false
            val request =
                SignUpRequest(
                    name = form.fullName,
                    email = form.email,
                    password = form.password,
                    passwordConfirmation = form.password,
                    educationLevel = form.level.code,
                    adult = guardian?.adult,
                    guardianChannel = guardian?.channel?.takeIf { underAge }?.code,
                    guardianPhone =
                        guardian?.contact?.takeIf {
                            underAge && guardian.channel == GuardianChannel.WhatsApp
                        },
                    guardianEmail =
                        guardian?.contact?.takeIf {
                            underAge && guardian.channel == GuardianChannel.Email
                        },
                    mitraLink = drafts.mitraLink,
                    deviceName = acceptor.deviceName,
                )
            return apiCall(json) { api.signUp(request) }.map { reply ->
                val token = reply.registrationToken
                if (reply.code == SignInReply.EMAIL_CODE_SENT && token != null) {
                    val sentTo = reply.sentTo.orEmpty()
                    drafts.saveEmailCode(token, sentTo)
                    SignUpResult.EmailCodeSent(sentTo = sentTo, resendInSeconds = reply.resendIn ?: 0)
                } else {
                    finish(reply)
                }
            }
        }

        override suspend fun confirmEmailCode(code: String): Result<SignUpResult> {
            val pending = drafts.emailCode ?: return Result.failure(expired())
            return apiCall(json) { api.confirmSignUpCode(SignUpCodeRequest(pending.token, code, acceptor.deviceName)) }
                .onFailure(::forgetSpentToken)
                .map(::finish)
        }

        override suspend fun resendEmailCode(): Result<Int> {
            val pending = drafts.emailCode ?: return Result.failure(expired())
            return apiCall(json) { api.resendSignUpCode(SignUpResendRequest(pending.token, acceptor.deviceName)) }
                .onFailure(::forgetSpentToken)
                .map { reply ->
                    // DEC-154: the code could not go out, so the account was made and waits for its email.
                    if (reply.token != null) {
                        finish(reply)
                        0
                    } else {
                        drafts.saveEmailCode(pending.token, reply.sentTo ?: pending.sentTo)
                        reply.resendIn ?: 0
                    }
                }
        }

        override fun observeConsent(): Flow<ConsentState?> = consent.asStateFlow()

        override suspend fun refreshConsent(): Result<Boolean> =
            apiCall(json) { api.consent() }
                .onSuccess(::keep)
                .map { it.waiting }

        override suspend fun resendConsent(): Result<String> =
            apiCall(json) { api.resendConsent() }
                .onSuccess(::keep)
                .map { it.message.orEmpty() }

        override suspend fun changeConsentContact(
            channel: GuardianChannel,
            contact: String,
        ): Result<String> {
            val request =
                ConsentContactRequest(
                    guardianChannel = channel.code,
                    guardianPhone = contact.takeIf { channel == GuardianChannel.WhatsApp },
                    guardianEmail = contact.takeIf { channel == GuardianChannel.Email },
                )
            return apiCall(json) { api.changeConsentContact(request) }
                .onSuccess(::keep)
                .map { it.message.orEmpty() }
        }

        /** The latest state; once the parent approved, the waiting is over and Beranda opens (T-026 required 13). */
        private fun keep(reply: ConsentReply) {
            consent.value = reply.toState()
            if (!reply.waiting) signals.clearAwaitingConsent()
        }

        private fun finish(reply: SignInReply): SignUpResult {
            consent.value = null
            acceptor.acceptSignUp(reply)
            return if (reply.consentLocked) SignUpResult.AwaitingConsent else SignUpResult.SignedIn
        }

        /** A `registration_token` the server no longer knows starts Daftar over, as on the web. */
        private fun forgetSpentToken(error: Throwable) {
            if (error is ApiError && error.fieldError(REGISTRATION_TOKEN) != null) drafts.clearEmailCode()
        }

        private fun expired() =
            ApiError(
                status = UNPROCESSABLE,
                message = EXPIRED_MESSAGE,
                code = "validation",
                fieldErrors = mapOf(REGISTRATION_TOKEN to listOf(EXPIRED_MESSAGE)),
            )

        private companion object {
            const val REGISTRATION_TOKEN = "registration_token"
            const val UNPROCESSABLE = 422

            /** The web's sentence for a sign-up past its 15 minutes (`SignUpController::EXPIRED_MESSAGE`). */
            const val EXPIRED_MESSAGE =
                "Waktu untuk memasukkan kode sudah habis. Tekan Daftar lagi untuk meminta kode baru."
        }
    }

internal val EducationLevel.code: String
    get() =
        when (this) {
            EducationLevel.Sd -> "sd"
            EducationLevel.Smp -> "smp"
            EducationLevel.SmaSmk -> "sma"
            EducationLevel.Kuliah -> "kuliah"
            EducationLevel.Graduated -> "umum"
        }

internal val GuardianChannel.code: String
    get() = if (this == GuardianChannel.Email) ConsentReply.CHANNEL_EMAIL else ConsentReply.CHANNEL_WHATSAPP

private fun channelOf(code: String): GuardianChannel? =
    when (code) {
        ConsentReply.CHANNEL_WHATSAPP -> GuardianChannel.WhatsApp
        ConsentReply.CHANNEL_EMAIL -> GuardianChannel.Email
        else -> null
    }

internal fun GuardianQuestionPayload.toQuestion(): GuardianQuestion =
    GuardianQuestion(
        age = age,
        levels = levels.mapNotNull(::educationLevelOf).toSet(),
        channels = channels.mapNotNull(::channelOf).ifEmpty { listOf(GuardianChannel.WhatsApp) },
    )

internal fun JoinNoticePayload.toNotice(): JoinNotice =
    JoinNotice(isClass = kind == "class", organization = organization, label = label)

internal fun ConsentReply.toState(): ConsentState =
    ConsentState(
        status =
            when (status) {
                "expired" -> ConsentStatus.Expired
                "declined" -> ConsentStatus.Declined
                "revoked" -> ConsentStatus.Revoked
                else -> ConsentStatus.Pending
            },
        contact = contact,
        channel = channelOf(channel) ?: GuardianChannel.WhatsApp,
        channels = channels.mapNotNull(::channelOf),
        delivery =
            when (delivery) {
                "queued" -> ConsentDelivery.Queued
                "sent" -> ConsentDelivery.Sent
                "limited" -> ConsentDelivery.Limited
                else -> ConsentDelivery.Failed
            },
        expiresAt =
            expiresAt?.let {
                try {
                    OffsetDateTime.parse(it).toInstant()
                } catch (_: DateTimeParseException) {
                    null
                }
            },
    )
