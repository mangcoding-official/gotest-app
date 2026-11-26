package com.gotest.app.feature.auth.ui.consent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.auth.domain.ConsentState
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.SignUpRepository
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

/** "Ganti kontak orang tua", open below the message. */
@Immutable
data class ConsentChangeForm(
    val channel: GuardianChannel,
    val contact: String = "",
    val errorMessage: String? = null,
)

@Immutable
data class GuardianConsentUiState(
    val consent: ConsentState? = null,
    val change: ConsentChangeForm? = null,
    /** The server's confirmation, such as "Tautan baru dikirim ke WhatsApp 0812-****-890.". */
    val notice: String? = null,
    val resendError: String? = null,
    val isResending: Boolean = false,
    val isChanging: Boolean = false,
    val isSigningOut: Boolean = false,
    /** Pulled down to read the waiting page again. */
    val isRefreshing: Boolean = false,
    /** Why the sign-up's Mitra link was dropped, right after the email was verified (DEC-154); shown once. */
    val emailVerifiedReason: String? = null,
)

sealed interface GuardianConsentUiEvent {
    data object Resend : GuardianConsentUiEvent

    data object OpenChange : GuardianConsentUiEvent

    data object CancelChange : GuardianConsentUiEvent

    data class ChannelPicked(
        val channel: GuardianChannel,
    ) : GuardianConsentUiEvent

    data class ContactChanged(
        val value: String,
    ) : GuardianConsentUiEvent

    data object SubmitChange : GuardianConsentUiEvent

    data object SignOut : GuardianConsentUiEvent

    /** Opened again from the background, or pulled down: has the parent approved meanwhile? */
    data object Refresh : GuardianConsentUiEvent
}

sealed interface GuardianConsentEffect {
    data object SignedOut : GuardianConsentEffect

    /** The parent approved: the waiting is over and Beranda opens (T-026 required 13). */
    data object Approved : GuardianConsentEffect
}

/** What an account waiting for a parent's approval sees (web: Auth/GuardianConsentWaiting.tsx). */
@HiltViewModel
class GuardianConsentViewModel
    @Inject
    constructor(
        private val repository: SignUpRepository,
        private val authRepository: AuthRepository,
        notices: SessionNotices,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(GuardianConsentUiState(emailVerifiedReason = notices.takeEmailVerified()))
        val uiState: StateFlow<GuardianConsentUiState> = _uiState.asStateFlow()

        private val _effects = Channel<GuardianConsentEffect>(Channel.BUFFERED)
        val effects: Flow<GuardianConsentEffect> = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                repository.observeConsent().collect { consent -> _uiState.update { it.copy(consent = consent) } }
            }
        }

        fun onEvent(event: GuardianConsentUiEvent) {
            when (event) {
                GuardianConsentUiEvent.Resend -> {
                    resend()
                }

                GuardianConsentUiEvent.OpenChange -> {
                    _uiState.update { it.copy(change = it.consent?.let(::openForm)) }
                }

                GuardianConsentUiEvent.CancelChange -> {
                    _uiState.update { it.copy(change = null) }
                }

                is GuardianConsentUiEvent.ChannelPicked -> {
                    _uiState.update {
                        it.copy(
                            change = it.change?.copy(channel = event.channel, contact = "", errorMessage = null),
                        )
                    }
                }

                is GuardianConsentUiEvent.ContactChanged -> {
                    _uiState.update { it.copy(change = it.change?.copy(contact = event.value, errorMessage = null)) }
                }

                GuardianConsentUiEvent.SubmitChange -> {
                    submitChange()
                }

                GuardianConsentUiEvent.SignOut -> {
                    signOut()
                }

                GuardianConsentUiEvent.Refresh -> {
                    refresh()
                }
            }
        }

        /** The form opens on the latest channel when it is still ready, otherwise the first one ready. */
        private fun openForm(consent: ConsentState): ConsentChangeForm =
            ConsentChangeForm(
                channel =
                    consent.channel.takeIf { it in consent.channels } ?: consent.channels.firstOrNull()
                        ?: GuardianChannel.WhatsApp,
            )

        private fun resend() {
            if (_uiState.value.isResending) return
            _uiState.update { it.copy(isResending = true, resendError = null, notice = null) }
            viewModelScope.launch {
                repository
                    .resendConsent()
                    .onSuccess { message -> _uiState.update { it.copy(notice = message) } }
                    .onFailure { error -> _uiState.update { it.copy(resendError = error.message) } }
                _uiState.update { it.copy(isResending = false) }
            }
        }

        private fun submitChange() {
            val state = _uiState.value
            val form = state.change ?: return
            if (state.isChanging) return
            _uiState.update { it.copy(isChanging = true, notice = null) }
            viewModelScope.launch {
                repository
                    .changeConsentContact(form.channel, form.contact)
                    .onSuccess { message ->
                        _uiState.update { it.copy(change = null, notice = message, resendError = null) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(change = it.change?.copy(errorMessage = error.message)) }
                    }
                _uiState.update { it.copy(isChanging = false) }
            }
        }

        private fun refresh() {
            if (_uiState.value.isRefreshing) return
            _uiState.update { it.copy(isRefreshing = true) }
            viewModelScope.launch {
                repository.refreshConsent().onSuccess { waiting ->
                    if (!waiting) _effects.send(GuardianConsentEffect.Approved)
                }
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }

        private fun signOut() {
            if (_uiState.value.isSigningOut) return
            _uiState.update { it.copy(isSigningOut = true) }
            viewModelScope.launch {
                authRepository.signOut()
                _effects.send(GuardianConsentEffect.SignedOut)
            }
        }
    }
