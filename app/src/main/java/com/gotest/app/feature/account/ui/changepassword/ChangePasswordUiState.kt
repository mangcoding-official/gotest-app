package com.gotest.app.feature.account.ui.changepassword

import androidx.compose.runtime.Immutable

/** Web ChangePasswordModal: the password link box and the three fields; the server checks the rules. */
@Immutable
data class ChangePasswordUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** The account's email, named once the link is sent by email. */
    val email: String = "",
    val googleLinked: Boolean = false,
    val linkByWhatsApp: Boolean = false,
    val current: String = "",
    val password: String = "",
    val confirmation: String = "",
    /** Server messages by field: current_password, password. */
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val linkSending: Boolean = false,
    /** After "Kirim Tautan", the button gives way to the sent note until the screen is opened again. */
    val linkSent: Boolean = false,
    val linkError: String? = null,
)

sealed interface ChangePasswordUiEvent {
    data object Retry : ChangePasswordUiEvent

    data class CurrentChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data class PasswordChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data class ConfirmationChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data object Submit : ChangePasswordUiEvent

    data object SendLink : ChangePasswordUiEvent
}

sealed interface ChangePasswordEffect {
    data object PasswordChanged : ChangePasswordEffect

    /** Null when the request did not reach the server: the screen says "try again". */
    data class ShowMessage(
        val message: String?,
    ) : ChangePasswordEffect
}
