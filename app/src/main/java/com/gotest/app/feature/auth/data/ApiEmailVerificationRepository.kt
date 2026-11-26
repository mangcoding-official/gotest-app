package com.gotest.app.feature.auth.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.auth.domain.EmailVerificationCode
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationState
import com.gotest.app.feature.auth.domain.EmailVerified
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

/** `api/v1/akun/verifikasi-email` (DEC-154, T-042). */
interface EmailVerificationApi {
    @GET("api/v1/akun/verifikasi-email")
    suspend fun state(): EmailVerificationState

    @POST("api/v1/akun/verifikasi-email/kirim-ulang")
    suspend fun resend(): EmailVerificationState

    @POST("api/v1/akun/verifikasi-email/kode")
    suspend fun verify(
        @Body code: EmailVerificationCode,
    ): EmailVerified
}

@Singleton
class ApiEmailVerificationRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : EmailVerificationRepository {
        private val api = retrofit.create(EmailVerificationApi::class.java)

        override suspend fun state(): Result<EmailVerificationState> = apiCall(json) { api.state() }

        override suspend fun resend(): Result<EmailVerificationState> = apiCall(json) { api.resend() }

        override suspend fun verify(code: String): Result<EmailVerified> =
            apiCall(json) { api.verify(EmailVerificationCode(code)) }
    }
