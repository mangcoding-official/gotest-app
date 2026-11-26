package com.gotest.app.feature.auth.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationState
import com.gotest.app.feature.auth.domain.EmailVerified
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug data for "Menunggu verifikasi email" (DEC-154; debug builds only). The code is `123456`; the first
 * "Kirim ulang kode" still fails to send, as when the server's email is down. Messages are the server's
 * (EmailVerificationController, OtpService).
 */
@Singleton
class FakeEmailVerificationRepository
    @Inject
    constructor() : EmailVerificationRepository {
        private var state =
            EmailVerificationState(
                waiting = true,
                email = EMAIL,
                sentTo = "g***@mail.com",
                resendIn = 0,
                sendFailed = true,
            )
        private var resends = 0

        override suspend fun state(): Result<EmailVerificationState> {
            simulateLatency()
            return Result.success(state)
        }

        override suspend fun resend(): Result<EmailVerificationState> {
            simulateLatency()
            resends++
            if (resends == 1) {
                return refuse("Kode belum bisa dikirim ke email. Coba lagi beberapa saat lagi.")
            }
            state = state.copy(resendIn = RESEND_SECONDS, sendFailed = false)
            return Result.success(state)
        }

        override suspend fun verify(code: String): Result<EmailVerified> {
            simulateLatency()
            return when {
                !code.matches(Regex("\\d{6}")) -> {
                    refuse("Tulis kode 6 angka dari email.")
                }

                code != CODE -> {
                    refuse("Kode salah. Sisa 4 kali percobaan.")
                }

                else -> {
                    state = state.copy(waiting = false, resendIn = 0, sendFailed = false)
                    Result.success(EmailVerified())
                }
            }
        }

        private fun <T> refuse(message: String): Result<T> =
            Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf("code" to listOf(message))))

        private companion object {
            const val EMAIL = "gilangpermana@mail.com"
            const val CODE = "123456"
            const val RESEND_SECONDS = 60
            const val UNPROCESSABLE = 422
        }
    }
