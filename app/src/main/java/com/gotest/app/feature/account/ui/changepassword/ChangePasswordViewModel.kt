package com.gotest.app.feature.account.ui.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.account.domain.AccountValidator
import com.gotest.app.feature.account.domain.PasswordRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val validator: AccountValidator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChangePasswordUiState())
        val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ChangePasswordEffect>(Channel.BUFFERED)
        val effects: Flow<ChangePasswordEffect> = _effects.receiveAsFlow()

        fun onEvent(event: ChangePasswordUiEvent) {
            when (event) {
                is ChangePasswordUiEvent.CurrentPasswordChange -> edit { it.copy(currentPassword = event.value) }
                is ChangePasswordUiEvent.NewPasswordChange -> edit { it.copy(newPassword = event.value) }
                is ChangePasswordUiEvent.ConfirmationChange -> edit { it.copy(confirmation = event.value) }
                ChangePasswordUiEvent.Submit -> submit()
            }
        }

        /** Applies [change] and re-evaluates the live checklist. */
        private fun edit(change: (ChangePasswordUiState) -> ChangePasswordUiState) {
            _uiState.update { old ->
                val state = change(old)
                val rules = validator.satisfiedPasswordRules(state.currentPassword, state.newPassword)
                val mismatch = state.confirmation.isNotEmpty() && state.confirmation != state.newPassword
                state.copy(
                    satisfiedRules = rules,
                    confirmationMismatch = mismatch,
                    canSubmit = rules.containsAll(PasswordRule.entries) && state.confirmation == state.newPassword,
                )
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (!state.canSubmit || state.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                userRepository
                    .changePassword(state.currentPassword, state.newPassword)
                    .onSuccess { _effects.send(ChangePasswordEffect.PasswordChanged) }
                    .onFailure { _effects.send(ChangePasswordEffect.ShowMessage(R.string.account_password_error)) }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
