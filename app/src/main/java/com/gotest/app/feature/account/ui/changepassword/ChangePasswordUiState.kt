package com.gotest.app.feature.account.ui.changepassword

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.domain.PasswordRule

/**
 * Form-only screen: nothing is loaded, so there is no Loading/Error state; saving is tracked
 * by [isSubmitting] and failures arrive as a snackbar.
 */
@Immutable
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    /** Checklist rules the new password already meets (green tick). */
    val satisfiedRules: Set<PasswordRule> = emptySet(),
    /** True once something is typed in the confirmation and it differs from the new password. */
    val confirmationMismatch: Boolean = false,
    val canSubmit: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface ChangePasswordUiEvent {
    data class CurrentPasswordChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data class NewPasswordChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data class ConfirmationChange(
        val value: String,
    ) : ChangePasswordUiEvent

    data object Submit : ChangePasswordUiEvent
}

sealed interface ChangePasswordEffect {
    data object PasswordChanged : ChangePasswordEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : ChangePasswordEffect
}
