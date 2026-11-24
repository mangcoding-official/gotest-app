package com.gotest.app.core.data.api

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.apiCall
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.FamilyChild
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.PhoneVerification
import com.gotest.app.feature.auth.domain.SignUpResult
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Masuk with a WhatsApp code (and the name step of a new number), Masuk Anak, and resetting the password from
 * the email link, against API module A. A proven new number keeps its `registration_token` outside memory
 * (T-026 required 10) and its sign-up carries the Mitra link opened before signing in (required 4).
 */
@Singleton
class ApiAuthFlowRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
        private val acceptor: SignInAcceptor,
        private val drafts: SignUpDraftStore,
    ) : AuthFlowRepository {
        private val api = retrofit.create(AuthApi::class.java)
        private val flows = retrofit.create(SignInFlowApi::class.java)

        override suspend fun resetPassword(
            token: String,
            email: String,
            password: String,
        ): Result<Unit> =
            apiCall(json) { api.resetPassword(ResetPasswordRequest(token, email, password, password)) }
                .asAuthFlow()
                .map { }

        override suspend fun findFamily(code: String): Result<List<FamilyChild>> =
            apiCall(json) { flows.family(FamilyCodeRequest(code)) }
                .asAuthFlow()
                .map { reply -> reply.children.map { FamilyChild(id = it.id.toString(), name = it.name) } }

        override suspend fun signInChild(
            code: String,
            childId: String,
            pin: String,
        ): Result<Unit> {
            val id = childId.toLongOrNull() ?: return Result.failure(AuthFlowError("child_id", PICK_NAME))
            return apiCall(json) { flows.signInChild(ChildSignInRequest(code, id, pin, acceptor.deviceName)) }
                .asAuthFlow()
                .map { acceptor.accept(it) }
        }

        override suspend fun sendPhoneCode(phone: String): Result<String> =
            apiCall(json) { flows.sendPhoneCode(PhoneRequest(phone)) }
                .asAuthFlow()
                .map { it.sentTo.orEmpty() }

        override suspend fun verifyPhoneCode(
            phone: String,
            code: String,
        ): Result<PhoneVerification> {
            val result = apiCall(json) { flows.signInWithPhone(PhoneSignInRequest(phone, code, acceptor.deviceName)) }
            val error = result.exceptionOrNull()
            val token = (error as? ApiError)?.takeIf { it.code == PHONE_NOT_REGISTERED }?.detail(REGISTRATION_TOKEN)
            if (token != null) {
                drafts.savePhoneToken(token)
                val display = (error as ApiError).detail("phone").orEmpty()
                return Result.success(PhoneVerification.NeedsRegistration(display))
            }
            return result.asAuthFlow().map { reply ->
                acceptor.accept(reply)
                PhoneVerification.SignedIn
            }
        }

        override suspend fun completePhoneRegistration(
            fullName: String,
            level: EducationLevel,
            guardian: GuardianAnswer?,
        ): Result<SignUpResult> {
            val token = drafts.phoneToken ?: return Result.failure(AuthFlowError("name", PHONE_EXPIRED))
            val underAge = guardian?.adult == false
            val request =
                PhoneSignUpRequest(
                    registrationToken = token,
                    name = fullName,
                    educationLevel = level.code,
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
            return apiCall(json) { flows.signUpWithPhone(request) }
                .onFailure { if ((it as? ApiError)?.fieldError(REGISTRATION_TOKEN) != null) drafts.clearPhoneToken() }
                .asAuthFlow()
                .map { reply ->
                    acceptor.accept(reply)
                    if (reply.consentLocked) SignUpResult.AwaitingConsent else SignUpResult.SignedIn
                }
        }

        private companion object {
            const val PHONE_NOT_REGISTERED = "phone_not_registered"
            const val REGISTRATION_TOKEN = "registration_token"

            /** The web's sentences (Masuk Anak, and the name step after 15 minutes). */
            const val PICK_NAME = "Pilih namamu."
            const val PHONE_EXPIRED = "Waktu pendaftaran habis. Minta kode baru dengan nomor HP kamu."
        }
    }
