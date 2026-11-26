package com.gotest.app.feature.auth.ui.forgotpassword

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class ForgotPasswordUiState(
    val email: String = "",
    @param:StringRes val emailError: Int? = null,
    /** The server's sentence when the link was refused, such as too many requests. */
    val emailMessage: String? = null,
    val isSubmitting: Boolean = false,
    /** Email the reset link went to; non-null once sent. */
    val sentTo: String? = null,
    /** The server's sentence once the link was sent, shown in place of the app's own. */
    val sentMessage: String? = null,
    /** The link goes to WhatsApp: the field takes an email or a WhatsApp number (`password_reset_whatsapp`). */
    val byWhatsApp: Boolean = false,
)

sealed interface ForgotPasswordUiEvent {
    data class EmailChanged(
        val value: String,
    ) : ForgotPasswordUiEvent

    data object Submit : ForgotPasswordUiEvent
}
