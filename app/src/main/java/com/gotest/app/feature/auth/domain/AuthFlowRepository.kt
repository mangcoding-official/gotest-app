package com.gotest.app.feature.auth.domain

import com.gotest.app.core.model.EducationLevel
import kotlinx.coroutines.flow.Flow

/** A refusal with the server's own text (the same as on the web), shown under [field]. */
class AuthFlowError(
    val field: String,
    override val message: String,
) : Exception(message) {
    companion object {
        /** The refusal names no field: it is for the whole form. */
        const val FORM = "form"
    }
}

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

/** After Google's account picker: a known account is signed in, a new one finishes on "Satu langkah lagi". */
sealed interface GoogleSignIn {
    data object SignedIn : GoogleSignIn

    /** [name] fills the name field; [email] is the Google account shown above the form, as on the web. */
    data class NeedsRegistration(
        val name: String,
        val email: String,
    ) : GoogleSignIn
}

/**
 * Sign-in flows the web has and the app gains in T-018: resetting the password from the email link and
 * Masuk Anak, against API module A since T-026 A2a.
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

    /** Sends a 6-digit code to [phone] (a stored `628…` number); returns where it went, in display form. */
    suspend fun sendPhoneCode(phone: String): Result<String>

    suspend fun verifyPhoneCode(
        phone: String,
        code: String,
    ): Result<PhoneVerification>

    /** Masuk dengan Google with the ID token Credential Manager gave (`POST /auth/google`). */
    suspend fun signInWithGoogle(idToken: String): Result<GoogleSignIn>

    /** Finishes a new Google account's sign-up within 15 minutes ("Selesai daftar"). */
    suspend fun completeGoogleRegistration(
        fullName: String,
        level: EducationLevel,
        guardian: GuardianAnswer? = null,
    ): Result<SignUpResult>

    /** Finishes a new number's sign-up within 15 minutes of its code ("Selesai daftar"). */
    suspend fun completePhoneRegistration(
        fullName: String,
        level: EducationLevel,
        guardian: GuardianAnswer? = null,
    ): Result<SignUpResult>
}
