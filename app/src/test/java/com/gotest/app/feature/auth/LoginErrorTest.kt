package com.gotest.app.feature.auth

import com.gotest.app.R
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.ui.login.LoginUiState
import com.gotest.app.feature.auth.ui.login.withSignInError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** Masuk shows the server's sentence where the web shows it (T-026 required 2). */
class LoginErrorTest {
    @Test
    fun theServerSaysWhereAndWhy() {
        val wrong = "Email atau kata sandi tidak sesuai."
        val onEmail =
            LoginUiState().withSignInError(
                ApiError(422, wrong, "validation", mapOf("email" to listOf(wrong))),
            )
        assertEquals(wrong, onEmail.emailMessage)

        val blocked = LoginUiState().withSignInError(ApiError(401, "Akunmu diblokir.", "account_blocked"))
        assertEquals("Akunmu diblokir.", blocked.formMessage)

        assertEquals(R.string.auth_error_offline, LoginUiState().withSignInError(IOException()).formError)
        assertEquals(R.string.auth_error_sign_in, LoginUiState().withSignInError(IllegalArgumentException()).formError)
    }
}
