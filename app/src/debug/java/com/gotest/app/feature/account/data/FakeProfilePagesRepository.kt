package com.gotest.app.feature.account.data

import com.gotest.app.core.data.fake.FakeAccountStore
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.account.domain.PasswordChange
import com.gotest.app.feature.account.domain.ProfilePage
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfileSession
import com.gotest.app.feature.account.domain.ProfileStats
import com.gotest.app.feature.account.domain.ProfileUpdate
import com.gotest.app.feature.account.domain.ProfileWriteReply
import com.gotest.app.feature.account.domain.apiCode
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug Profil (debug builds only). Codes are `123456`, the current password `rahasia123`, 0812-1111-2222
 * belongs to another account. Name, email, photo and number are written to [FakeAccountStore], as the
 * app reloads `/akun` after a write. Rules and messages follow ProfileAccount, OtpService and UserSessions.
 */
@Singleton
class FakeProfilePagesRepository
    @Inject
    constructor(
        private val store: FakeAccountStore,
    ) : ProfilePagesRepository {
        private var pendingEmail: String? = null
        private var pendingPhone: String? = null
        private var wrongCodes = 0
        private var sessions =
            listOf(
                ProfileSession("app-17", "app", "Google Pixel 8", null, Instant.now().toString(), current = true),
                ProfileSession("kV3a", "browser", "Chrome di Windows", "36.71.140.5", ago(hours = 2), current = false),
                ProfileSession("pQ9z", "browser", "Safari di iPhone", null, ago(hours = 72), current = false),
            )

        override suspend fun page(): Result<ProfilePage> {
            simulateLatency()
            return Result.success(
                ProfilePage(
                    phone = store.profile.value.phone,
                    phoneSignIn = true,
                    emailCode = true,
                    emailVerified = true,
                    soleManagerOf = emptyList(),
                    stats = ProfileStats(120, 74, 6, questionsDoneChange = 25, accuracyChange = -3),
                    sessions = sessions,
                ),
            )
        }

        override suspend fun update(update: ProfileUpdate): Result<ProfileWriteReply> {
            simulateLatency()
            val email = update.email.trim().lowercase()
            val errors = mutableMapOf<String, String>()
            if (update.name.isBlank()) errors["name"] = "Nama wajib diisi."
            if (email.isEmpty()) {
                errors["email"] = "Email wajib diisi."
            } else if (!EMAIL.matches(email)) {
                errors["email"] = "Email harus berupa alamat email yang valid."
            }
            if (errors.isNotEmpty()) return Result.failure(invalid(errors))
            val level = EducationLevel.entries.firstOrNull { it.apiCode == update.educationLevel }
            store.profile.update { it.copy(fullName = update.name.trim(), educationLevel = level ?: it.educationLevel) }
            if (email ==
                store.profile.value.email
                    .lowercase()
            ) {
                return Result.success(ProfileWriteReply())
            }
            pendingEmail = email
            wrongCodes = 0
            return Result.success(ProfileWriteReply(emailCodeSentTo = mask(email)))
        }

        override suspend fun resendEmailCode(email: String): Result<ProfileWriteReply> {
            simulateLatency()
            pendingEmail = email.trim().lowercase()
            wrongCodes = 0
            return Result.success(ProfileWriteReply(emailCodeSentTo = mask(email)))
        }

        override suspend fun saveEmail(
            email: String,
            code: String,
        ): Result<ProfileWriteReply> {
            simulateLatency()
            checkCode(code)?.let { return Result.failure(it) }
            val saved = pendingEmail ?: email.trim().lowercase()
            store.profile.update { it.copy(email = saved) }
            pendingEmail = null
            return Result.success(ProfileWriteReply(message = "Email baru tersimpan."))
        }

        override suspend fun uploadPhoto(jpeg: ByteArray): Result<ProfileWriteReply> {
            simulateLatency()
            if (jpeg.size > MAX_PHOTO_BYTES) {
                return Result.failure(invalid(mapOf("photo" to "Photo tidak boleh lebih dari 2048 kilobyte.")))
            }
            return Result.success(ProfileWriteReply())
        }

        override suspend fun deletePhoto(): Result<ProfileWriteReply> {
            simulateLatency()
            store.profile.update { it.copy(avatarUrl = null) }
            return Result.success(ProfileWriteReply())
        }

        override suspend fun changePassword(change: PasswordChange): Result<ProfileWriteReply> {
            simulateLatency()
            val errors = mutableMapOf<String, String>()
            when {
                change.currentPassword.isEmpty() -> errors["current_password"] = "Kata sandi saat ini wajib diisi."
                change.currentPassword != CURRENT_PASSWORD -> errors["current_password"] = "Kata sandi salah."
            }
            passwordError(change)?.let { errors["password"] = it }
            if (errors.isNotEmpty()) return Result.failure(invalid(errors))
            sessions = sessions.filter { it.current || !it.isApp }
            return Result.success(ProfileWriteReply())
        }

        override suspend fun sendPasswordLink(): Result<ProfileWriteReply> {
            simulateLatency()
            return Result.success(ProfileWriteReply())
        }

        override suspend fun sendPhoneCode(phone: String): Result<ProfileWriteReply> {
            simulateLatency()
            val stored = PhoneNumber.normalize(phone)
            return when (stored) {
                null -> {
                    Result.failure(
                        invalid(
                            mapOf(
                                "phone" to "Tulis nomor HP Indonesia yang aktif di WhatsApp, misalnya 0812 3456 7890.",
                            ),
                        ),
                    )
                }

                TAKEN_PHONE -> {
                    Result.failure(invalid(mapOf("phone" to "Nomor ini sudah dipakai akun lain.")))
                }

                else -> {
                    pendingPhone = stored
                    wrongCodes = 0
                    Result.success(ProfileWriteReply(phoneCodeSentTo = PhoneNumber.display(stored)))
                }
            }
        }

        override suspend fun savePhone(
            phone: String,
            code: String,
        ): Result<ProfileWriteReply> {
            simulateLatency()
            val stored =
                pendingPhone?.takeIf { it == PhoneNumber.normalize(phone) }
                    ?: return Result.failure(
                        invalid(mapOf("code" to "Minta kode dulu untuk nomor yang ingin ditautkan.")),
                    )
            checkCode(code)?.let { return Result.failure(it) }
            pendingPhone = null
            store.profile.update { it.copy(phone = PhoneNumber.display(stored)) }
            return Result.success(
                ProfileWriteReply(message = "Nomor WhatsApp tersimpan. Kamu kini juga bisa masuk dengan nomor ini."),
            )
        }

        override suspend fun removePhone(): Result<ProfileWriteReply> {
            simulateLatency()
            if (!store.profile.value.hasEmail) {
                return Result.failure(
                    invalid(mapOf("phone" to "Nomor ini satu-satunya cara masuk ke akunmu, jadi tidak bisa dihapus.")),
                )
            }
            store.profile.update { it.copy(phone = null) }
            return Result.success(ProfileWriteReply(message = "Nomor WhatsApp dihapus dari akun."))
        }

        override suspend fun signOutOtherDevices(currentPassword: String): Result<ProfileWriteReply> {
            simulateLatency()
            return when {
                currentPassword.isEmpty() -> {
                    Result.failure(invalid(mapOf("current_password" to "Kata sandi saat ini wajib diisi.")))
                }

                currentPassword != CURRENT_PASSWORD -> {
                    Result.failure(invalid(mapOf("current_password" to "Password saat ini tidak sesuai.")))
                }

                else -> {
                    val others = sessions.count { !it.current }
                    sessions = sessions.filter { it.current }
                    Result.success(ProfileWriteReply(signedOut = others))
                }
            }
        }

        override suspend fun deleteAccount(confirmation: String): Result<ProfileWriteReply> {
            simulateLatency()
            return when {
                confirmation.isBlank() -> {
                    Result.failure(invalid(mapOf("confirmation" to "Confirmation wajib diisi.")))
                }

                confirmation.trim() != "gotest.id" -> {
                    Result.failure(invalid(mapOf("confirmation" to "Ketik ulang gotest.id untuk menghapus akun.")))
                }

                else -> {
                    Result.success(ProfileWriteReply())
                }
            }
        }

        private fun checkCode(code: String): ApiError? =
            when {
                !CODE.matches(code) -> {
                    invalid(mapOf("code" to "Tulis kode 6 angka dari email."))
                }

                wrongCodes >= MAX_CODE_TRIES -> {
                    invalid(mapOf("code" to TOO_MANY_TRIES))
                }

                code != CODE_VALUE -> {
                    wrongCodes++
                    val left = MAX_CODE_TRIES - wrongCodes
                    invalid(mapOf("code" to if (left > 0) "Kode salah. Sisa $left kali percobaan." else TOO_MANY_TRIES))
                }

                else -> {
                    null
                }
            }

        private fun passwordError(change: PasswordChange): String? =
            when {
                change.password.isEmpty() -> "Kata sandi wajib diisi."
                change.password.length < MIN_PASSWORD -> "Kata sandi minimal 8 karakter."
                change.password.length > MAX_PASSWORD -> "Kata sandi tidak boleh lebih dari 72 karakter."
                change.password.none(Char::isLetter) -> "Kata sandi harus memuat minimal satu huruf."
                change.password.none(Char::isDigit) -> "Kata sandi harus memuat minimal satu angka."
                change.password != change.passwordConfirmation -> "Konfirmasi kata sandi tidak cocok."
                else -> null
            }

        private fun invalid(errors: Map<String, String>): ApiError =
            ApiError(UNPROCESSABLE, errors.values.first(), "validation", errors.mapValues { listOf(it.value) })

        private fun mask(email: String): String {
            val name = email.substringBefore('@')
            return "${name.take(1)}***@${email.substringAfter('@')}"
        }

        private companion object {
            const val CODE_VALUE = "123456"
            const val CURRENT_PASSWORD = "rahasia123"
            const val TAKEN_PHONE = "6281211112222"
            const val TOO_MANY_TRIES = "Terlalu banyak percobaan. Minta kode baru."
            const val MAX_CODE_TRIES = 5
            const val MIN_PASSWORD = 8
            const val MAX_PASSWORD = 72
            const val MAX_PHOTO_BYTES = 2 * 1024 * 1024
            const val UNPROCESSABLE = 422
            val CODE = Regex("^\\d{6}$")
            val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

            fun ago(hours: Long): String = Instant.now().minus(hours, ChronoUnit.HOURS).toString()
        }
    }
