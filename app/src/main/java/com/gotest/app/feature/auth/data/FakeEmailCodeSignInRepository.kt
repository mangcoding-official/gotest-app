package com.gotest.app.feature.auth.data

import com.gotest.app.core.data.fake.FakeAccountStore
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.EmailCodeSignInRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy data until the API exists (README): code `123456` signs in gilangpermana@mail.com; any other email
 * gets the same answer, but no code ever matches it; admin@gotest.id is a Gotest team account, which keeps
 * its password. Texts and limits are the web's (EmailCodeSignInController, OtpService).
 */
@Singleton
class FakeEmailCodeSignInRepository
    @Inject
    constructor(
        private val session: SessionStore,
        private val accounts: FakeAccountStore,
    ) : EmailCodeSignInRepository {
        private var pendingEmail: String? = null
        private var wrong = 0

        override suspend fun isAvailable(): Boolean = true

        override suspend fun sendCode(email: String): Result<String> {
            simulateLatency()
            val normalized = email.trim().lowercase()
            return when {
                normalized.isEmpty() -> {
                    Result.failure(AuthFlowError("email", "Tulis email akunmu."))
                }

                !EMAIL.matches(normalized) -> {
                    Result.failure(AuthFlowError("email", "Tulis email yang benar, misalnya nama@contoh.id."))
                }

                else -> {
                    pendingEmail = normalized
                    wrong = 0
                    Result.success(normalized.take(1) + "***@" + normalized.substringAfter('@'))
                }
            }
        }

        override suspend fun resendCode(): Result<Unit> {
            simulateLatency()
            if (pendingEmail == null) return Result.failure(AuthFlowError("code", NO_CODE))
            wrong = 0
            return Result.success(Unit)
        }

        override suspend fun verify(code: String): Result<Unit> {
            simulateLatency()
            val email = pendingEmail
            return when {
                code.length != CODE_LENGTH -> {
                    Result.failure(AuthFlowError("code", "Tulis kode 6 angka dari email."))
                }

                email == null -> {
                    Result.failure(AuthFlowError("code", NO_CODE))
                }

                wrong >= MAX_TRIES -> {
                    Result.failure(AuthFlowError("code", TOO_MANY_TRIES))
                }

                // Only a sent code can match, so an email without an account never signs in.
                code != CODE || email !in ACCOUNTS -> {
                    wrong++
                    val left = MAX_TRIES - wrong
                    Result.failure(
                        AuthFlowError(
                            "code",
                            if (left >
                                0
                            ) {
                                "Kode salah. Sisa $left kali percobaan."
                            } else {
                                TOO_MANY_TRIES
                            },
                        ),
                    )
                }

                email == TEAM_EMAIL -> {
                    Result.failure(AuthFlowError("code", TEAM_MESSAGE))
                }

                else -> {
                    pendingEmail = null
                    accounts.useDefault()
                    session.save(FAKE_TOKEN)
                    Result.success(Unit)
                }
            }
        }

        private companion object {
            const val CODE = "123456"
            const val CODE_LENGTH = 6
            const val MAX_TRIES = 5
            const val TEAM_EMAIL = "admin@gotest.id"
            const val FAKE_TOKEN = "fake-email-code-token"
            const val NO_CODE = "Minta kode dulu dengan email kamu."
            const val TOO_MANY_TRIES = "Terlalu banyak percobaan. Minta kode baru."
            const val TEAM_MESSAGE =
                "Akun ini tidak dapat masuk dengan kode email. Silakan masuk dengan email dan kata sandi."
            val ACCOUNTS = setOf("gilangpermana@mail.com", TEAM_EMAIL)
            val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }
