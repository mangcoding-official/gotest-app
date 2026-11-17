package com.gotest.app.feature.auth.ui.emailcode

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.EmailCodeSignInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class EmailCodeSignInUiState(
    /** "Kode lewat email" is on; without it the Email tab has only the password form. */
    val isAvailable: Boolean = false,
    /** The Email tab shows the code form instead of the password form. */
    val isCodeMode: Boolean = false,
    val isCodeStep: Boolean = false,
    val email: String = "",
    val emailMessage: String? = null,
    /** The email, masked, as the web shows it ("g***@mail.com"). */
    val sentTo: String = "",
    val code: String = "",
    val codeMessage: String? = null,
    val resendInSeconds: Int = 0,
    val isLoading: Boolean = false,
) {
    val canSubmitCode: Boolean get() = code.length == CODE_LENGTH && !isLoading

    companion object {
        const val CODE_LENGTH = 6
    }
}

sealed interface EmailCodeSignInUiEvent {
    /** "Masuk dengan kode email" (true) or "Masuk dengan kata sandi" (false). */
    data class UseCode(
        val enabled: Boolean,
    ) : EmailCodeSignInUiEvent

    data class EmailChanged(
        val value: String,
    ) : EmailCodeSignInUiEvent

    data object SendCode : EmailCodeSignInUiEvent

    data class CodeChanged(
        val value: String,
    ) : EmailCodeSignInUiEvent

    data object Resend : EmailCodeSignInUiEvent

    data object ChangeEmail : EmailCodeSignInUiEvent

    data object Submit : EmailCodeSignInUiEvent
}

sealed interface EmailCodeSignInEffect {
    data object SignedIn : EmailCodeSignInEffect
}

/**
 * "Masuk dengan kode email" on Masuk (web: Auth/Login.tsx and EmailCodeForm, T-023), with the
 * 60-second resend wait.
 */
@HiltViewModel
class EmailCodeSignInViewModel
    @Inject
    constructor(
        private val repository: EmailCodeSignInRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EmailCodeSignInUiState())
        val uiState: StateFlow<EmailCodeSignInUiState> = _uiState.asStateFlow()
        private val effectChannel = Channel<EmailCodeSignInEffect>(Channel.BUFFERED)
        val effects: Flow<EmailCodeSignInEffect> = effectChannel.receiveAsFlow()
        private var countdown: Job? = null

        init {
            viewModelScope.launch {
                val available = repository.isAvailable()
                _uiState.update { it.copy(isAvailable = available) }
            }
        }

        fun onEvent(event: EmailCodeSignInUiEvent) {
            when (event) {
                is EmailCodeSignInUiEvent.UseCode -> {
                    _uiState.update { it.copy(isCodeMode = event.enabled) }
                }

                is EmailCodeSignInUiEvent.EmailChanged -> {
                    _uiState.update { it.copy(email = event.value, emailMessage = null) }
                }

                EmailCodeSignInUiEvent.SendCode -> {
                    send()
                }

                is EmailCodeSignInUiEvent.CodeChanged -> {
                    _uiState.update {
                        it.copy(
                            code = event.value.filter(Char::isDigit).take(EmailCodeSignInUiState.CODE_LENGTH),
                            codeMessage = null,
                        )
                    }
                }

                EmailCodeSignInUiEvent.Resend -> {
                    resend()
                }

                EmailCodeSignInUiEvent.ChangeEmail -> {
                    countdown?.cancel()
                    _uiState.update { it.copy(isCodeStep = false, code = "", codeMessage = null, resendInSeconds = 0) }
                }

                EmailCodeSignInUiEvent.Submit -> {
                    verify()
                }
            }
        }

        private fun send() {
            val state = _uiState.value
            if (state.isLoading) return
            load {
                repository
                    .sendCode(state.email)
                    .onSuccess { sentTo ->
                        _uiState.update { it.copy(isCodeStep = true, sentTo = sentTo, code = "", codeMessage = null) }
                        startCountdown()
                    }.onFailure { error -> _uiState.update { it.copy(emailMessage = error.message) } }
            }
        }

        private fun resend() {
            val state = _uiState.value
            if (state.isLoading || state.resendInSeconds > 0) return
            load {
                repository
                    .resendCode()
                    .onSuccess {
                        _uiState.update { it.copy(code = "", codeMessage = null) }
                        startCountdown()
                    }.onFailure { error -> _uiState.update { it.copy(codeMessage = error.message) } }
            }
        }

        private fun verify() {
            val state = _uiState.value
            if (!state.canSubmitCode) return
            load {
                repository
                    .verify(state.code)
                    .onSuccess { effectChannel.send(EmailCodeSignInEffect.SignedIn) }
                    .onFailure { error ->
                        // Anything but the code itself belongs to the email step, as on the web.
                        val onCode = (error as? AuthFlowError)?.field != "email"
                        _uiState.update {
                            if (onCode) {
                                it.copy(
                                    code = "",
                                    codeMessage = error.message,
                                )
                            } else {
                                it.copy(isCodeStep = false, emailMessage = error.message)
                            }
                        }
                    }
            }
        }

        private fun load(block: suspend () -> Unit) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                block()
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        private fun startCountdown() {
            countdown?.cancel()
            countdown =
                viewModelScope.launch {
                    for (left in RESEND_SECONDS downTo 0) {
                        _uiState.update { it.copy(resendInSeconds = left) }
                        if (left > 0) delay(SECOND_MILLIS)
                    }
                }
        }

        private companion object {
            const val RESEND_SECONDS = 60
            const val SECOND_MILLIS = 1_000L
        }
    }
