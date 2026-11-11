package com.gotest.app.feature.auth.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.ui.components.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val validator: CredentialValidator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ForgotPasswordUiState())
        val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

        fun onEvent(event: ForgotPasswordUiEvent) {
            when (event) {
                is ForgotPasswordUiEvent.EmailChanged -> {
                    _uiState.update { it.copy(email = event.value, emailError = null, sentTo = null) }
                }

                ForgotPasswordUiEvent.Submit -> {
                    submit()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            val error = validator.validateEmail(state.email)?.messageRes()
            if (error != null) {
                _uiState.update { it.copy(emailError = error) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                val email = state.email.trim()
                authRepository.requestPasswordReset(email).onSuccess { _uiState.update { it.copy(sentTo = email) } }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
