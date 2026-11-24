package com.gotest.app.feature.auth.ui.emailverification

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationState
import com.gotest.app.feature.auth.domain.EmailVerified
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

/** A refusal under the code field: the server's sentence, or a dropped connection. */
sealed interface CodeProblem {
    data class Server(
        val message: String,
    ) : CodeProblem

    data object Offline : CodeProblem
}

@Immutable
data class EmailVerificationUiState(
    val page: EmailVerificationState? = null,
    val loadFailed: Boolean = false,
    val code: String = "",
    val problem: CodeProblem? = null,
    /** Seconds before "Kirim ulang kode" may be pressed again. */
    val resendIn: Int = 0,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val isSigningOut: Boolean = false,
) {
    val canSubmit: Boolean get() = code.length == CODE_LENGTH && !isVerifying

    companion object {
        const val CODE_LENGTH = 6
    }
}

sealed interface EmailVerificationEffect {
    /** The email is proven; [joined] is the Mitra the sign-up link joined (its Ruang opens), if any. */
    data class Verified(
        val joined: String?,
    ) : EmailVerificationEffect

    data object SignedOut : EmailVerificationEffect
}

/**
 * "Menunggu verifikasi email" (DEC-154, T-042; web Auth/EmailVerificationWaiting): send the code again, type
 * it, sign out, or delete the account. The right code opens the account; the parent's approval page follows
 * when it is asked now.
 */
@HiltViewModel
class EmailVerificationViewModel
    @Inject
    constructor(
        private val repository: EmailVerificationRepository,
        private val authRepository: AuthRepository,
        private val signals: SessionSignals,
        private val notices: SessionNotices,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EmailVerificationUiState())
        val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

        private val _effects = Channel<EmailVerificationEffect>(Channel.BUFFERED)
        val effects: Flow<EmailVerificationEffect> = _effects.receiveAsFlow()

        private var countdown: Job? = null

        init {
            load()
        }

        private var loading = false

        fun load() {
            // "Coba lagi" pressed twice asks once (review of the required items).
            if (loading) return
            loading = true
            _uiState.update { it.copy(loadFailed = false) }
            viewModelScope.launch {
                repository
                    .state()
                    .onSuccess(::show)
                    .onFailure { _uiState.update { it.copy(loadFailed = true) } }
                loading = false
            }
        }

        private fun show(page: EmailVerificationState) {
            // Verified meanwhile (by the code on another device, or by the Gotest team): the account is open.
            if (!page.waiting) {
                opened(EmailVerified())
                return
            }
            _uiState.update { it.copy(page = page) }
            startCountdown(page.resendIn)
        }

        fun onCode(value: String) {
            val digits = value.filter(Char::isDigit).take(EmailVerificationUiState.CODE_LENGTH)
            _uiState.update { it.copy(code = digits, problem = null) }
        }

        fun verify() {
            val state = _uiState.value
            if (!state.canSubmit) return
            _uiState.update { it.copy(isVerifying = true, problem = null) }
            viewModelScope.launch {
                repository
                    .verify(state.code)
                    .onSuccess(::opened)
                    .onFailure { error -> _uiState.update { it.copy(code = "", problem = problemOf(error)) } }
                _uiState.update { it.copy(isVerifying = false) }
            }
        }

        fun resend() {
            val state = _uiState.value
            if (state.resendIn > 0 || state.isResending) return
            _uiState.update { it.copy(isResending = true, problem = null) }
            viewModelScope.launch {
                repository
                    .resend()
                    .onSuccess { page ->
                        _uiState.update { it.copy(page = page, code = "") }
                        startCountdown(page.resendIn)
                    }.onFailure { error -> _uiState.update { it.copy(problem = problemOf(error)) } }
                _uiState.update { it.copy(isResending = false) }
            }
        }

        fun signOut() {
            if (_uiState.value.isSigningOut) return
            _uiState.update { it.copy(isSigningOut = true) }
            viewModelScope.launch {
                authRepository.signOut()
                signals.clearAwaitingEmailVerification()
                _effects.send(EmailVerificationEffect.SignedOut)
            }
        }

        private fun opened(verified: EmailVerified) {
            // Why the sign-up's Mitra link was dropped: Beranda (or the parent's approval page) says it once.
            verified.message?.takeIf { it.isNotBlank() }?.let(notices::postEmailVerified)
            signals.clearAwaitingEmailVerification()
            if (verified.consent == EmailVerified.CONSENT_PENDING) {
                // The app shell opens "Menunggu persetujuan orang tua" for it.
                signals.markAwaitingConsent()
            } else {
                _effects.trySend(EmailVerificationEffect.Verified(verified.joined?.code))
            }
        }

        private fun startCountdown(seconds: Int) {
            countdown?.cancel()
            _uiState.update { it.copy(resendIn = seconds.coerceAtLeast(0)) }
            if (seconds <= 0) return
            countdown =
                viewModelScope.launch {
                    while (_uiState.value.resendIn > 0) {
                        delay(SECOND_MS)
                        _uiState.update { it.copy(resendIn = it.resendIn - 1) }
                    }
                }
        }

        private fun problemOf(error: Throwable): CodeProblem =
            if (error is ApiError) {
                CodeProblem.Server(error.fieldError("code") ?: error.message)
            } else {
                CodeProblem.Offline
            }

        private companion object {
            const val SECOND_MS = 1_000L
        }
    }
