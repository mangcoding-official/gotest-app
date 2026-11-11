package com.gotest.app.feature.auth.ui.register

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    @param:StringRes val fullNameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmationError: Int? = null,
    val isSubmitting: Boolean = false,
)

sealed interface RegisterUiEvent {
    data class FullNameChanged(
        val value: String,
    ) : RegisterUiEvent

    data class EmailChanged(
        val value: String,
    ) : RegisterUiEvent

    data class PasswordChanged(
        val value: String,
    ) : RegisterUiEvent

    data class ConfirmationChanged(
        val value: String,
    ) : RegisterUiEvent

    data object Submit : RegisterUiEvent

    data object GoogleSignUp : RegisterUiEvent
}

sealed interface RegisterEffect {
    data object Registered : RegisterEffect
}
