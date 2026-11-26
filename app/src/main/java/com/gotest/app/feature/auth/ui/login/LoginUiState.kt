package com.gotest.app.feature.auth.ui.login

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.feature.auth.ui.components.GoogleIdTokenResult

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    /** Shown under the password field when the sign-in fails without the server's own sentence. */
    @param:StringRes val formError: Int? = null,
    /** The server's sentence on `email` (wrong email or password, too many tries), as the web shows it. */
    val emailMessage: String? = null,
    /**
     * The server's sentence for the whole sign-in: the reason an account was blocked (401 `account_blocked`,
     * T-026 required 2) or that a Gotest team account signs in on gotest.id (403).
     */
    val formMessage: String? = null,
    val isSubmitting: Boolean = false,
    /** "Masuk dengan Google" only while the server offers it (`/referensi`). */
    val showGoogle: Boolean = false,
)

sealed interface LoginUiEvent {
    data class EmailChanged(
        val value: String,
    ) : LoginUiEvent

    data class PasswordChanged(
        val value: String,
    ) : LoginUiEvent

    data object Submit : LoginUiEvent

    /** "Masuk dengan Google": the route opens Google's account picker and sends [GoogleResult]. */
    data object GoogleSignIn : LoginUiEvent

    data class GoogleResult(
        val result: GoogleIdTokenResult,
    ) : LoginUiEvent
}

/** One-off results the route turns into navigation. */
sealed interface LoginEffect {
    data object SignedIn : LoginEffect

    /** A Google account without a Gotest account finishes on "Satu langkah lagi" (as the web's GoogleRegister). */
    data class GoogleRegister(
        val name: String,
        val email: String,
    ) : LoginEffect
}
