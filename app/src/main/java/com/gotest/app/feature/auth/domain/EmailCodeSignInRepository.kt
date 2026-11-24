package com.gotest.app.feature.auth.domain

/**
 * "Masuk dengan kode email" (T-023, F-c FC6): a code to the account's email instead of the password.
 * Bound to a fake until the API (T-028) is up. Whether it is on comes from [SignInOptionsRepository].
 */
interface EmailCodeSignInRepository {
    /**
     * Sends a code when the email has an account. An email without one gets the same answer and the
     * same limits without any email going out (T-023 K1). Returns the email, masked.
     */
    suspend fun sendCode(email: String): Result<String>

    /** A new code to the same email; the earlier one stops working. */
    suspend fun resendCode(): Result<Unit>

    suspend fun verify(code: String): Result<Unit>
}
