package com.gotest.app.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
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
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val validator: CredentialValidator,
        private val options: SignInOptionsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
        val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                val google = options.options().google
                _uiState.update { it.copy(showGoogle = google) }
            }
        }

        fun onEvent(event: LoginUiEvent) {
            when (event) {
                is LoginUiEvent.EmailChanged -> {
                    _uiState.update {
                        it.copy(
                            email = event.value,
                            emailError = null,
                            formError = null,
                            emailMessage = null,
                            formMessage = null,
                        )
                    }
                }

                is LoginUiEvent.PasswordChanged -> {
                    _uiState.update {
                        it.copy(password = event.value, passwordError = null, formError = null, formMessage = null)
                    }
                }

                LoginUiEvent.Submit -> {
                    submit()
                }

                LoginUiEvent.GoogleSignIn -> {
                    signInWithGoogle()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            val emailError = validator.validateEmail(state.email)?.messageRes()
            val passwordError = validator.validatePassword(state.password)?.messageRes()
            if (emailError != null || passwordError != null) {
                _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                authRepository
                    .signIn(state.email.trim(), state.password)
                    .onSuccess { _effects.send(LoginEffect.SignedIn) }
                    .onFailure { error -> _uiState.update { it.withSignInError(error) } }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private fun signInWithGoogle() {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                authRepository.signInWithGoogle().onSuccess { _effects.send(LoginEffect.SignedIn) }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

/** Where a refused sign-in says why: the server's sentence on `email`, for the whole form, or the app's own. */
internal fun LoginUiState.withSignInError(error: Throwable): LoginUiState =
    when {
        error is ApiError && error.fieldError("email") != null -> copy(emailMessage = error.fieldError("email"))
        error is ApiError && error.status != 0 -> copy(formMessage = error.message)
        error is IOException -> copy(formError = R.string.auth_error_offline)
        else -> copy(formError = R.string.auth_error_sign_in)
    }
