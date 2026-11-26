package com.gotest.app.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.GoogleSignIn
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.ui.components.GoogleIdTokenResult
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
        private val flows: AuthFlowRepository,
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

                // The route opens Google's picker; its answer comes back as GoogleResult.
                LoginUiEvent.GoogleSignIn -> {}

                is LoginUiEvent.GoogleResult -> {
                    signInWithGoogle(event.result)
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

        private fun signInWithGoogle(picked: GoogleIdTokenResult) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(emailMessage = null, formMessage = null, formError = null) }
            when (picked) {
                GoogleIdTokenResult.Cancelled -> {
                    return
                }

                GoogleIdTokenResult.Failed -> {
                    _uiState.update { it.copy(formError = R.string.auth_google_failed) }
                }

                is GoogleIdTokenResult.Token -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isSubmitting = true) }
                        flows
                            .signInWithGoogle(picked.value)
                            .onSuccess { result ->
                                _effects.send(
                                    when (result) {
                                        GoogleSignIn.SignedIn -> {
                                            LoginEffect.SignedIn
                                        }

                                        is GoogleSignIn.NeedsRegistration -> {
                                            LoginEffect.GoogleRegister(
                                                result.name,
                                                result.email,
                                            )
                                        }
                                    },
                                )
                            }.onFailure { error -> _uiState.update { it.withGoogleError(error) } }
                        _uiState.update { it.copy(isSubmitting = false) }
                    }
                }
            }
        }
    }

/** The web shows a refused Google sign-in under the email field; a blocked or team account for the whole form. */
internal fun LoginUiState.withGoogleError(error: Throwable): LoginUiState {
    val refusal = error as? AuthFlowError ?: return copy(formError = R.string.auth_google_failed)
    return if (refusal.field ==
        AuthFlowError.FORM
    ) {
        copy(formMessage = refusal.message)
    } else {
        copy(emailMessage = refusal.message)
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
