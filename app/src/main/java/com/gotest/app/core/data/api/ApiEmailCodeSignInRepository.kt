package com.gotest.app.core.data.api

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.EmailCodeSignInRepository
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Masuk dengan kode email" against API module A (T-028): an email without an account gets the same answer and
 * no email, so the app cannot tell the difference either.
 */
@Singleton
class ApiEmailCodeSignInRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
        private val acceptor: SignInAcceptor,
    ) : EmailCodeSignInRepository {
        private val api = retrofit.create(SignInFlowApi::class.java)
        private var pendingEmail: String? = null

        override suspend fun sendCode(email: String): Result<String> {
            val normalized = email.trim()
            return apiCall(json) { api.sendEmailCode(EmailRequest(normalized)) }
                .asAuthFlow()
                .map { reply ->
                    pendingEmail = normalized
                    reply.sentTo.orEmpty()
                }
        }

        override suspend fun resendCode(): Result<Unit> {
            val email = pendingEmail ?: return Result.failure(AuthFlowError("code", NO_CODE))
            return apiCall(json) { api.resendEmailCode(EmailRequest(email)) }.asAuthFlow().map { }
        }

        override suspend fun verify(code: String): Result<Unit> {
            val email = pendingEmail ?: return Result.failure(AuthFlowError("code", NO_CODE))
            return apiCall(json) { api.signInWithEmailCode(EmailCodeSignInRequest(email, code, acceptor.deviceName)) }
                .asAuthFlow()
                .map { reply ->
                    pendingEmail = null
                    acceptor.accept(reply)
                }
        }

        private companion object {
            /** The web's sentence when there is no code to check (`EmailCodeSignIn`). */
            const val NO_CODE = "Minta kode dulu dengan email kamu."
        }
    }
