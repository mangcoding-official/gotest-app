package com.gotest.app.feature.auth.ui.resetpassword

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.ui.components.messageRes
import com.gotest.app.navigation.ResetPasswordDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ResetPasswordUiState(
    val email: String = "",
    /** The link carried the email, so it cannot be changed (as on the web). */
    val emailLocked: Boolean = false,
    val password: String = "",
    val confirmation: String = "",
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmationError: Int? = null,
    @param:StringRes val emailErrorRes: Int? = null,
    /** The server's own text for a link that is no longer valid. */
    val emailMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isDone: Boolean = false,
)

sealed interface ResetPasswordUiEvent {
    data class EmailChanged(
        val value: String,
    ) : ResetPasswordUiEvent

    data class PasswordChanged(
        val value: String,
    ) : ResetPasswordUiEvent

    data class ConfirmationChanged(
        val value: String,
    ) : ResetPasswordUiEvent

    data object Submit : ResetPasswordUiEvent
}

/** "Buat kata sandi baru" from `https://gotest.id/reset-password/{token}?email=…` (T-018). */
@HiltViewModel
class ResetPasswordViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: AuthFlowRepository,
        private val validator: CredentialValidator,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<ResetPasswordDestination>()
        private val _uiState =
            MutableStateFlow(ResetPasswordUiState(email = route.email.orEmpty(), emailLocked = route.email != null))
        val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

        fun onEvent(event: ResetPasswordUiEvent) {
            when (event) {
                is ResetPasswordUiEvent.EmailChanged -> {
                    _uiState.update { it.copy(email = event.value, emailErrorRes = null, emailMessage = null) }
                }

                is ResetPasswordUiEvent.PasswordChanged -> {
                    _uiState.update { it.copy(password = event.value, passwordError = null) }
                }

                is ResetPasswordUiEvent.ConfirmationChanged -> {
                    _uiState.update { it.copy(confirmation = event.value, confirmationError = null) }
                }

                ResetPasswordUiEvent.Submit -> {
                    submit()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            val emailError = validator.validateEmail(state.email)?.messageRes()
            val passwordError = validator.validateNewPassword(state.password)?.messageRes()
            val confirmationError = validator.validateConfirmation(state.password, state.confirmation)?.messageRes()
            if (emailError != null || passwordError != null || confirmationError != null) {
                _uiState.update {
                    it.copy(
                        emailErrorRes = emailError,
                        passwordError = passwordError,
                        confirmationError = confirmationError,
                    )
                }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                repository
                    .resetPassword(route.token, state.email.trim(), state.password)
                    .onSuccess { _uiState.update { it.copy(isDone = true) } }
                    .onFailure { error -> _uiState.update { it.copy(emailMessage = error.message) } }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
