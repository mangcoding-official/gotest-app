package com.gotest.app.feature.auth.domain

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
}
