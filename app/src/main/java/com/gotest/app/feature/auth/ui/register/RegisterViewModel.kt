package com.gotest.app.feature.auth.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.ui.components.messageRes
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
class RegisterViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val validator: CredentialValidator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RegisterUiState())
        val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

        private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
        val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

        fun onEvent(event: RegisterUiEvent) {
            when (event) {
                is RegisterUiEvent.FullNameChanged -> {
                    _uiState.update { it.copy(fullName = event.value, fullNameError = null) }
                }

                is RegisterUiEvent.EmailChanged -> {
                    _uiState.update { it.copy(email = event.value, emailError = null) }
                }

                is RegisterUiEvent.PasswordChanged -> {
                    _uiState.update { it.copy(password = event.value, passwordError = null) }
                }

                is RegisterUiEvent.ConfirmationChanged -> {
                    _uiState.update { it.copy(confirmation = event.value, confirmationError = null) }
                }

                RegisterUiEvent.Submit -> {
                    submit()
                }

                RegisterUiEvent.GoogleSignUp -> {
                    signUpWithGoogle()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            val validated =
                state.copy(
                    fullNameError = validator.validateRequired(state.fullName)?.messageRes(),
                    emailError = validator.validateEmail(state.email)?.messageRes(),
                    passwordError = validator.validatePassword(state.password)?.messageRes(),
                    confirmationError =
                        validator
                            .validateConfirmation(
                                state.password,
                                state.confirmation,
                            )?.messageRes(),
                )
            val hasError =
                listOf(
                    validated.fullNameError,
                    validated.emailError,
                    validated.passwordError,
                    validated.confirmationError,
                ).any { it != null }
            if (hasError) {
                _uiState.value = validated
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                authRepository
                    .register(state.fullName.trim(), state.email.trim(), state.password)
                    .onSuccess { _effects.send(RegisterEffect.Registered) }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private fun signUpWithGoogle() {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                authRepository.signInWithGoogle().onSuccess { _effects.send(RegisterEffect.Registered) }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
