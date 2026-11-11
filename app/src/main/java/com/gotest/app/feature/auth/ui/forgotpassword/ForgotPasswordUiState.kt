package com.gotest.app.feature.auth.ui.forgotpassword

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class ForgotPasswordUiState(
    val email: String = "",
    @param:StringRes val emailError: Int? = null,
    val isSubmitting: Boolean = false,
    /** Email the reset link went to; non-null once sent. */
    val sentTo: String? = null,
)

sealed interface ForgotPasswordUiEvent {
    data class EmailChanged(
        val value: String,
    ) : ForgotPasswordUiEvent

    data object Submit : ForgotPasswordUiEvent
}
