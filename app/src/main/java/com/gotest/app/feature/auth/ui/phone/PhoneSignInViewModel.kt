package com.gotest.app.feature.auth.ui.phone

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.model.PhoneNumber
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.PhoneVerification
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
data class PhoneSignInUiState(
    /** The server offers WhatsApp codes; without it Masuk and Daftar show only the email form. */
    val isAvailable: Boolean = false,
    val isCodeStep: Boolean = false,
    val phone: String = "",
    val code: String = "",
    /** Where the code went, as the web shows it (0812-3456-7890). */
    val sentTo: String = "",
    val resendInSeconds: Int = 0,
    @param:StringRes val phoneErrorRes: Int? = null,
    val phoneMessage: String? = null,
    val codeMessage: String? = null,
    val isLoading: Boolean = false,
) {
    val canSubmitCode: Boolean get() = code.length == CODE_LENGTH && !isLoading

    companion object {
        const val CODE_LENGTH = 6
    }
}

sealed interface PhoneSignInUiEvent {
    data class PhoneChanged(
        val value: String,
    ) : PhoneSignInUiEvent

    data object SendCode : PhoneSignInUiEvent

    data class CodeChanged(
        val value: String,
    ) : PhoneSignInUiEvent

    data object Resend : PhoneSignInUiEvent

    data object ChangeNumber : PhoneSignInUiEvent

    data object Submit : PhoneSignInUiEvent
}

sealed interface PhoneSignInEffect {
    data object SignedIn : PhoneSignInEffect

    data class NeedsRegistration(
        val phoneDisplay: String,
    ) : PhoneSignInEffect
}

/** The WhatsApp code form shared by Masuk and Daftar (web: PhoneOtpForm), with the 60-second resend wait. */
@HiltViewModel
class PhoneSignInViewModel
    @Inject
    constructor(
        private val repository: AuthFlowRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PhoneSignInUiState())
        val uiState: StateFlow<PhoneSignInUiState> = _uiState.asStateFlow()
        private val effectChannel = Channel<PhoneSignInEffect>(Channel.BUFFERED)
        val effects: Flow<PhoneSignInEffect> = effectChannel.receiveAsFlow()
        private var countdown: Job? = null

        init {
            viewModelScope.launch {
                val available = repository.isPhoneSignInAvailable()
                _uiState.update { it.copy(isAvailable = available) }
            }
        }

        fun onEvent(event: PhoneSignInUiEvent) {
            when (event) {
                is PhoneSignInUiEvent.PhoneChanged -> {
                    _uiState.update { it.copy(phone = event.value, phoneErrorRes = null, phoneMessage = null) }
                }

                PhoneSignInUiEvent.SendCode, PhoneSignInUiEvent.Resend -> {
                    send()
                }

                is PhoneSignInUiEvent.CodeChanged -> {
                    _uiState.update {
                        it.copy(
                            code = event.value.filter(Char::isDigit).take(PhoneSignInUiState.CODE_LENGTH),
                            codeMessage = null,
                        )
                    }
                }

                PhoneSignInUiEvent.ChangeNumber -> {
                    countdown?.cancel()
                    _uiState.update { it.copy(isCodeStep = false, code = "", codeMessage = null, resendInSeconds = 0) }
                }

                PhoneSignInUiEvent.Submit -> {
                    verify()
                }
            }
        }

        private fun send() {
            val state = _uiState.value
            val phone = PhoneNumber.normalize(state.phone)
            when {
                state.isLoading -> {}

                phone == null -> {
                    _uiState.update { it.copy(phoneErrorRes = R.string.phone_invalid) }
                }

                else -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        repository
                            .sendPhoneCode(phone)
                            .onSuccess { sentTo ->
                                _uiState.update { it.copy(isCodeStep = true, sentTo = sentTo, codeMessage = null) }
                                startCountdown()
                            }.onFailure(::showError)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }

        private fun verify() {
            val state = _uiState.value
            val phone = PhoneNumber.normalize(state.phone)
            if (!state.canSubmitCode || phone == null) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository
                    .verifyPhoneCode(phone, state.code)
                    .onSuccess { result ->
                        effectChannel.send(
                            when (result) {
                                PhoneVerification.SignedIn -> {
                                    PhoneSignInEffect.SignedIn
                                }

                                is PhoneVerification.NeedsRegistration -> {
                                    PhoneSignInEffect.NeedsRegistration(
                                        result.phoneDisplay,
                                    )
                                }
                            },
                        )
                    }.onFailure(::showError)
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        // In the code step every message stays under the code field, as on the web (errors.code ?? errors.phone).
        private fun showError(error: Throwable) {
            val message = error.message
            _uiState.update {
                if (it.isCodeStep ||
                    (error as? AuthFlowError)?.field == "code"
                ) {
                    it.copy(codeMessage = message)
                } else {
                    it.copy(phoneMessage = message)
                }
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
