package com.gotest.app.feature.account.ui.deleteaccount

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Form-only screen: nothing is loaded, so there is no Loading/Error state; deletion progress is
 * [isDeleting] and failures arrive as a snackbar.
 */
@Immutable
data class DeleteAccountUiState(
    val confirmation: String = "",
    /** True once the student has retyped the confirmation word exactly. */
    val canDelete: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface DeleteAccountUiEvent {
    data class ConfirmationChange(
        val value: String,
    ) : DeleteAccountUiEvent

    data object Delete : DeleteAccountUiEvent
}

sealed interface DeleteAccountEffect {
    data object AccountDeleted : DeleteAccountEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : DeleteAccountEffect
}
