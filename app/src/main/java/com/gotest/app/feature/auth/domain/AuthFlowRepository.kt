package com.gotest.app.feature.auth.domain

import com.gotest.app.core.model.EducationLevel
import kotlinx.coroutines.flow.Flow

/** A refusal with the server's own text (the same as on the web), shown under [field]. */
class AuthFlowError(
    val field: String,
    override val message: String,
) : Exception(message)

/** A child of a family code, as listed on "Pilih namamu." */
data class FamilyChild(
    val id: String,
    val name: String,
)

/** After the WhatsApp code: a known number is signed in, a new one finishes on "Satu langkah lagi". */
sealed interface PhoneVerification {
    data object SignedIn : PhoneVerification

    data class NeedsRegistration(
        val phoneDisplay: String,
    ) : PhoneVerification
}

/**
 * Sign-in flows the web has and the app gains in T-018: resetting the password from the email link and
 * Masuk Anak. Bound to a fake until the API contract (Jalur 5 A1) is approved.
 */
interface AuthFlowRepository {
    suspend fun resetPassword(
        token: String,
        email: String,
        password: String,
    ): Result<Unit>

    /** The children of a family code; an unknown code and a family without children fail the same way. */
    suspend fun findFamily(code: String): Result<List<FamilyChild>>

    suspend fun signInChild(
        code: String,
        childId: String,
        pin: String,
    ): Result<Unit>

    /** Whether the server offers signing in with a WhatsApp code (the web's `phoneSignIn`). */
    suspend fun isPhoneSignInAvailable(): Boolean

    /** Sends a 6-digit code to [phone] (a stored `628…` number); returns where it went, in display form. */
    suspend fun sendPhoneCode(phone: String): Result<String>

    suspend fun verifyPhoneCode(
        phone: String,
        code: String,
    ): Result<PhoneVerification>

    /** Finishes a new number's sign-up within 15 minutes of its code ("Selesai daftar"). */
    suspend fun completePhoneRegistration(
        fullName: String,
        level: EducationLevel,
        guardian: GuardianAnswer? = null,
    ): Result<SignUpResult>
}
