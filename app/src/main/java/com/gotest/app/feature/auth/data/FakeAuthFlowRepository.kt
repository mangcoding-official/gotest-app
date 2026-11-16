package com.gotest.app.feature.auth.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.FamilyChild
import com.gotest.app.feature.auth.domain.PinLockout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy data until the API exists (README): family code `KLG123` with Raka and Sinta, PIN `2468`;
 * a reset link whose token is `kedaluwarsa` has expired. Texts are the web's.
 */
@Singleton
class FakeAuthFlowRepository
    @Inject
    constructor(
        private val session: SessionStore,
    ) : AuthFlowRepository {
        private val lockout = PinLockout()
        private val pinStates = mutableMapOf<String, PinLockout.State>()
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
        }
    }
