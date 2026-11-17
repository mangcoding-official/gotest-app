package com.gotest.app.feature.account.data

import com.gotest.app.core.data.fake.FakeAccountStore
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.feature.account.domain.AccountFieldError
import com.gotest.app.feature.account.domain.AccountSecurityRepository
import com.gotest.app.feature.account.domain.ActiveSession
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy data until the API exists (README): WhatsApp code `123456`; 0812-1111-2222 belongs to another
 * account; the current password is `rahasia123`; three devices, this one first. Texts are the web's.
 */
@Singleton
class FakeAccountSecurityRepository
    @Inject
    constructor(
        private val store: FakeAccountStore,
    ) : AccountSecurityRepository {
        private var pendingPhone: String? = null
        private var wrongCodes = 0
        private var devices =
            listOf(
                ActiveSession("session_this", "Gotest di Android", "103.10.66.12", null, current = true),
                ActiveSession(
                    "session_web",
                    "Chrome di Windows",
                    "36.71.140.5",
                    Instant.now().minus(2, ChronoUnit.HOURS),
                    current = false,
                ),
                ActiveSession(
                    "session_iphone",
                    "Safari di iPhone",
                    null,
                    Instant.now().minus(3, ChronoUnit.DAYS),
                    current = false,
                ),
            )

        override suspend fun isPhoneSignInAvailable(): Boolean = true

        override suspend fun sendPhoneCode(phone: String): Result<String> {
            simulateLatency()
            val stored = PhoneNumber.normalize(phone)
            return when {
                stored == null -> {
                    fail("phone", "Tulis nomor HP Indonesia yang aktif di WhatsApp, misalnya 0812 3456 7890.")
                }

                stored == TAKEN_PHONE -> {
                    fail("phone", TAKEN)
                }

                else -> {
                    pendingPhone = stored
                    wrongCodes = 0
                    Result.success(PhoneNumber.display(stored))
                }
            }
        }

        override suspend fun savePhone(
            phone: String,
            code: String,
        ): Result<String> {
            simulateLatency()
            val stored = pendingPhone?.takeIf { it == PhoneNumber.normalize(phone) }
            return when {
                stored == null -> {
                    fail("code", "Minta kode dulu untuk nomor yang ingin ditautkan.")
                }

                wrongCodes >= MAX_CODE_TRIES -> {
                    fail("code", TOO_MANY_TRIES)
                }

                code != PHONE_CODE -> {
                    wrongCodes++
                    val left = MAX_CODE_TRIES - wrongCodes
                    fail("code", if (left > 0) "Kode salah. Sisa $left kali percobaan." else TOO_MANY_TRIES)
                }

                else -> {
                    pendingPhone = null
                    store.profile.update { it.copy(phone = PhoneNumber.display(stored)) }
                    Result.success("Nomor WhatsApp tersimpan. Kamu kini juga bisa masuk dengan nomor ini.")
                }
            }
        }

        override suspend fun removePhone(): Result<String> {
            simulateLatency()
            if (!store.profile.value.hasEmail) {
                return fail("phone", "Nomor ini satu-satunya cara masuk ke akunmu, jadi tidak bisa dihapus.")
            }
            store.profile.update { it.copy(phone = null) }
            return Result.success("Nomor WhatsApp dihapus dari akun.")
        }

        override suspend fun sessions(): Result<List<ActiveSession>> {
            simulateLatency()
            return Result.success(devices)
        }

        override suspend fun revokeOtherSessions(currentPassword: String): Result<Unit> {
            simulateLatency()
            return when {
                currentPassword.isEmpty() -> {
                    Result.failure(AccountFieldError("current_password", "Kata sandi saat ini wajib diisi."))
                }

                currentPassword != CURRENT_PASSWORD -> {
                    Result.failure(AccountFieldError("current_password", "Password saat ini tidak sesuai."))
                }

                else -> {
                    devices = devices.filter { it.current }
                    Result.success(Unit)
                }
            }
        }

        private fun fail(
            field: String,
            message: String,
        ): Result<String> = Result.failure(AccountFieldError(field, message))

        private companion object {
            const val PHONE_CODE = "123456"
            const val TAKEN_PHONE = "6281211112222"
            const val TAKEN = "Nomor ini sudah dipakai akun lain."
            const val TOO_MANY_TRIES = "Terlalu banyak percobaan. Minta kode baru."
            const val MAX_CODE_TRIES = 5
            const val CURRENT_PASSWORD = "rahasia123"
        }
    }
