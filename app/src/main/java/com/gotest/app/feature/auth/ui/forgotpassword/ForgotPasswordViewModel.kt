package com.gotest.app.feature.auth.ui.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.CredentialError
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.ui.components.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val validator: CredentialValidator,
        options: SignInOptionsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ForgotPasswordUiState())
        val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val byWhatsApp = options.options().passwordResetWhatsApp
                _uiState.update { it.copy(byWhatsApp = byWhatsApp) }
            }
        }

        fun onEvent(event: ForgotPasswordUiEvent) {
            when (event) {
                is ForgotPasswordUiEvent.EmailChanged -> {
                    _uiState.update {
                        it.copy(
                            email = event.value,
                            emailError = null,
                            emailMessage = null,
                            sentTo = null,
                            sentMessage = null,
                        )
                    }
                }

                ForgotPasswordUiEvent.Submit -> {
                    submit()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            // A WhatsApp number is not an email; the server checks what was typed, as on the web.
            val error =
                if (state.byWhatsApp) {
                    CredentialError.Required.takeIf { state.email.isBlank() }?.messageRes()
                } else {
                    validator.validateEmail(state.email)?.messageRes()
                }
            if (error != null) {
                _uiState.update { it.copy(emailError = error) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                val email = state.email.trim()
                authRepository
                    .requestPasswordReset(email)
                    .onSuccess { message ->
                        _uiState.update { it.copy(sentTo = email, sentMessage = message.ifBlank { null }) }
                    }.onFailure { error ->
                        _uiState.update {
                            when (error) {
                                is ApiError -> it.copy(emailMessage = error.fieldError("email") ?: error.message)
                                is IOException -> it.copy(emailError = R.string.auth_error_offline)
                                else -> it.copy(emailError = R.string.auth_error_server)
                            }
                        }
                    }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
