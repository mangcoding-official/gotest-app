package com.gotest.app.feature.auth.ui.login

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    /** Shown under the password field when the server rejects the credentials. */
    @param:StringRes val formError: Int? = null,
    val isSubmitting: Boolean = false,
)

sealed interface LoginUiEvent {
    data class EmailChanged(
        val value: String,
    ) : LoginUiEvent

    data class PasswordChanged(
        val value: String,
    ) : LoginUiEvent

    data object Submit : LoginUiEvent

    data object GoogleSignIn : LoginUiEvent
}

/** One-off results the route turns into navigation. */
sealed interface LoginEffect {
    data object SignedIn : LoginEffect
}
