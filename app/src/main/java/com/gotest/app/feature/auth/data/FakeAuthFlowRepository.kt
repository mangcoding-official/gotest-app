package com.gotest.app.feature.auth.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.FamilyChild
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.PhoneVerification
import com.gotest.app.feature.auth.domain.PinLockout
import com.gotest.app.feature.auth.domain.SignUpResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy data until the API exists (README): family code `KLG123` with Raka and Sinta, PIN `2468`;
 * a reset link whose token is `kedaluwarsa` has expired; WhatsApp code `123456`, where 0812-3456-7890 is
 * an existing account and any other number signs up (Daftar: [FakeSignUpRepository]). Texts and limits are the web's.
 */
@Singleton
class FakeAuthFlowRepository
    @Inject
    constructor(
        private val session: SessionStore,
        private val signUps: FakeSignUpRepository,
    ) : AuthFlowRepository {
        private val lockout = PinLockout()
        private val pinStates = mutableMapOf<String, PinLockout.State>()
        private var phoneCode: PhoneCodeState? = null
        private var verifiedPhone: Pair<String, Long>? = null

        private data class PhoneCodeState(
            val phone: String,
            val sentAtMillis: Long,
            val wrong: Int = 0,
        )

        private val family = listOf(FamilyChild("child_raka", "Raka"), FamilyChild("child_sinta", "Sinta"))

        override suspend fun resetPassword(
            token: String,
            email: String,
            password: String,
        ): Result<Unit> {
            simulateLatency()
            return if (token == EXPIRED_TOKEN) {
                Result.failure(
                    AuthFlowError("email", "Tautan atur ulang kata sandi tidak valid atau sudah kedaluwarsa."),
                )
            } else {
                Result.success(Unit)
            }
        }

        override suspend fun findFamily(code: String): Result<List<FamilyChild>> {
            simulateLatency()
            return if (code == FAMILY_CODE) {
                Result.success(family)
            } else {
                Result.failure(AuthFlowError("code", "Kode keluarga tidak ditemukan. Periksa lagi kodenya."))
            }
        }

        override suspend fun signInChild(
            code: String,
            childId: String,
            pin: String,
        ): Result<Unit> {
            simulateLatency()
            val now = System.currentTimeMillis()
            val state = pinStates[childId] ?: PinLockout.State()
            val lockedMinutes = lockout.minutesLeft(state, now)
            return when {
                // While locked even the right PIN is refused (Aturan 11.29).
                lockedMinutes != null -> {
                    Result.failure(locked(lockedMinutes))
                }

                code == FAMILY_CODE && family.any { it.id == childId } && pin == CHILD_PIN -> {
                    pinStates.remove(childId)
                    session.save(FAKE_CHILD_TOKEN)
                    Result.success(Unit)
                }

                else -> {
                    val next = lockout.afterWrong(state, now)
                    pinStates[childId] = next
                    Result.failure(lockout.minutesLeft(next, now)?.let(::locked) ?: wrongPin())
                }
            }
        }

        private fun wrongPin() = AuthFlowError("pin", "Kode keluarga, nama, atau PIN belum cocok. Periksa lagi.")

        override suspend fun isPhoneSignInAvailable(): Boolean = true

        override suspend fun sendPhoneCode(phone: String): Result<String> {
            simulateLatency()
            val now = System.currentTimeMillis()
            val last = phoneCode?.takeIf { it.phone == phone }
            val waitSeconds = last?.let { RESEND_SECONDS - ((now - it.sentAtMillis) / MILLIS_PER_SECOND).toInt() } ?: 0
            if (waitSeconds > 0) {
                return Result.failure(AuthFlowError("phone", "Tunggu $waitSeconds detik sebelum meminta kode lagi."))
            }
            phoneCode = PhoneCodeState(phone = phone, sentAtMillis = now)
            return Result.success(PhoneNumber.display(phone))
        }

        override suspend fun verifyPhoneCode(
            phone: String,
            code: String,
        ): Result<PhoneVerification> {
            simulateLatency()
            val state = phoneCode?.takeIf { it.phone == phone }
            val expired = state != null && System.currentTimeMillis() - state.sentAtMillis > CODE_LIFETIME_MILLIS
            return when {
                state == null -> {
                    Result.failure(AuthFlowError("code", "Minta kode dulu dengan nomor HP kamu."))
                }

                expired || state.wrong >= MAX_CODE_TRIES -> {
                    Result.failure(codeGone(expired))
                }

                code != PHONE_CODE -> {
                    val wrong = state.wrong + 1
                    phoneCode = state.copy(wrong = wrong)
                    val left = MAX_CODE_TRIES - wrong
                    Result.failure(
                        if (left <=
                            0
                        ) {
                            codeGone(false)
                        } else {
                            AuthFlowError("code", "Kode salah. Sisa $left kali percobaan.")
                        },
                    )
                }

                else -> {
                    phoneCode = null
                    Result.success(signInOrRegister(phone))
                }
            }
        }

        override suspend fun completePhoneRegistration(
            fullName: String,
            level: EducationLevel,
            guardian: GuardianAnswer?,
        ): Result<SignUpResult> {
            simulateLatency()
            val pending = verifiedPhone?.takeIf { System.currentTimeMillis() - it.second <= REGISTRATION_MILLIS }
            verifiedPhone = null
            val ownNumber =
                guardian?.channel == GuardianChannel.WhatsApp &&
                    guardian.contact?.let(PhoneNumber::normalize) == pending?.first
            return when {
                pending == null -> {
                    Result.failure(
                        AuthFlowError("name", "Waktu pendaftaran habis. Minta kode baru dengan nomor HP kamu."),
                    )
                }

                ownNumber -> {
                    verifiedPhone = pending
                    Result.failure(
                        AuthFlowError("guardian_phone", "Tulis nomor orang tua atau wali, bukan nomormu sendiri."),
                    )
                }

                else -> {
                    Result.success(signUps.finishSignUp(FAKE_PHONE_TOKEN, guardian))
                }
            }
        }

        private fun signInOrRegister(phone: String): PhoneVerification =
            if (phone == KNOWN_PHONE) {
                session.save(FAKE_PHONE_TOKEN)
                PhoneVerification.SignedIn
            } else {
                verifiedPhone = phone to System.currentTimeMillis()
                PhoneVerification.NeedsRegistration(PhoneNumber.display(phone))
            }

        private fun codeGone(expired: Boolean) =
            AuthFlowError(
                "code",
                if (expired) {
                    "Kode sudah tidak berlaku. Minta kode baru."
                } else {
                    "Terlalu banyak percobaan. Minta kode baru."
                },
            )

        private fun locked(minutes: Int) =
            AuthFlowError(
                "pin",
                "PIN terkunci karena terlalu banyak yang salah. Coba lagi dalam $minutes menit, " +
                    "atau minta orang tua atau gurumu mengatur ulang PIN.",
            )

        private companion object {
            const val FAMILY_CODE = "KLG123"
            const val CHILD_PIN = "2468"
            const val EXPIRED_TOKEN = "kedaluwarsa"
            const val FAKE_CHILD_TOKEN = "fake-child-token"
            const val FAKE_PHONE_TOKEN = "fake-phone-token"
            const val PHONE_CODE = "123456"
            const val KNOWN_PHONE = "6281234567890"
            const val RESEND_SECONDS = 60
            const val MAX_CODE_TRIES = 5
            const val MILLIS_PER_SECOND = 1_000L
            const val CODE_LIFETIME_MILLIS = 5 * 60_000L
            const val REGISTRATION_MILLIS = 15 * 60_000L
        }
    }
