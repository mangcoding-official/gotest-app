package com.gotest.app.feature.ruang.ui.join

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.navigation.InvitationDestination
import com.gotest.app.navigation.JoinCodeDestination
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

/** A failed action: the server's message, or null for no connection (shown as the app's error text). */
@Immutable
data class ActionError(
    val message: String?,
)

private fun Throwable.toActionError(field: String): ActionError =
    ActionError((this as? ApiError)?.let { it.fieldError(field) ?: it.message })

@Immutable
data class JoinWithCodeUiState(
    val code: String = "",
    val isSubmitting: Boolean = false,
    val error: ActionError? = null,
)

/** "Gabung ke kelas": the typed code (web: Mitra/JoinWithCode). */
@HiltViewModel
class JoinWithCodeViewModel
    @Inject
    constructor(
        private val repository: RuangRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(JoinWithCodeUiState())
        val uiState: StateFlow<JoinWithCodeUiState> = _uiState.asStateFlow()

        private val _found = Channel<String>(Channel.BUFFERED)
        val found: Flow<String> = _found.receiveAsFlow()

        /** Upper case as typed, like the web field; at most 30 characters. */
        fun onCodeChange(value: String) {
            _uiState.update { it.copy(code = value.uppercase().take(MAX_CODE), error = null) }
        }

        fun submit() {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                repository
                    .lookupCode(_uiState.value.code)
                    .onSuccess { code ->
                        _uiState.update { it.copy(isSubmitting = false) }
                        _found.send(code)
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSubmitting = false, error = error.toActionError("code")) }
                    }
            }
        }

        private companion object {
            const val MAX_CODE = 30
        }
    }

@Immutable
data class JoinCodeUiState(
    val isLoading: Boolean = true,
    val page: JoinCodePage? = null,
    val isJoining: Boolean = false,
    val error: ActionError? = null,
)

/** The page of a class or event code (web: Mitra/Join). */
@HiltViewModel
class JoinCodeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: RuangRepository,
    ) : ViewModel() {
        private val code = savedStateHandle.toRoute<JoinCodeDestination>().code

        private val _uiState = MutableStateFlow(JoinCodeUiState())
        val uiState: StateFlow<JoinCodeUiState> = _uiState.asStateFlow()

        private val _joined = Channel<JoinedMitra>(Channel.BUFFERED)
        val joined: Flow<JoinedMitra> = _joined.receiveAsFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { JoinCodeUiState() }
            viewModelScope.launch {
                val page = repository.joinCode(code).getOrNull()
                _uiState.update { JoinCodeUiState(isLoading = false, page = page) }
            }
        }

        /** "Gabung", "Daftar", and "Buka ruang" (the web posts the same form, the server opens the space). */
        fun join() {
            if (_uiState.value.isJoining) return
            _uiState.update { it.copy(isJoining = true, error = null) }
            viewModelScope.launch {
                repository
                    .join(code)
                    .onSuccess { joined ->
                        _uiState.update { it.copy(isJoining = false) }
                        _joined.send(joined)
                    }.onFailure { error ->
                        _uiState.update { it.copy(isJoining = false, error = error.toActionError("code")) }
                    }
            }
        }
    }

@Immutable
data class InvitationUiState(
    val isLoading: Boolean = true,
    val page: InvitationPage? = null,
    /** The email code step (F-c FC3), shown after "Kirim kode". */
    val codeStep: Boolean = false,
    val sentTo: String? = null,
    val code: String = "",
    val resendIn: Int = 0,
    val isBusy: Boolean = false,
    val error: ActionError? = null,
)

sealed interface InvitationEffect {
    data class Joined(
        val joined: JoinedMitra,
    ) : InvitationEffect

    data object SignedOut : InvitationEffect
}

/** An invitation link (web: Mitra/Invitation), with the email proof of F-c FC3. */
@HiltViewModel
class InvitationViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: RuangRepository,
        private val auth: AuthRepository,
    ) : ViewModel() {
        private val token = savedStateHandle.toRoute<InvitationDestination>().token

        private val _uiState = MutableStateFlow(InvitationUiState())
        val uiState: StateFlow<InvitationUiState> = _uiState.asStateFlow()

        private val _effects = Channel<InvitationEffect>(Channel.BUFFERED)
        val effects: Flow<InvitationEffect> = _effects.receiveAsFlow()

        private var countdown: Job? = null

        init {
            load()
        }

        fun load() {
            _uiState.update { InvitationUiState() }
            viewModelScope.launch {
                val page = repository.invitation(token).getOrNull()
                val proof = page?.emailProof
                _uiState.update {
                    InvitationUiState(
                        isLoading = false,
                        page = page,
                        codeStep = proof?.codeSent == true,
                        sentTo = proof?.sentTo,
                    )
                }
                proof?.resendIn?.let(::startCountdown)
            }
        }

        fun onCodeChange(value: String) {
            _uiState.update { it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), error = null) }
        }

        /** "Kirim kode" and "Kirim ulang kode": a new code; the wait between sends is 60 seconds. */
        fun sendCode() {
            _uiState.update { it.copy(isBusy = true, error = null) }
            viewModelScope.launch {
                repository
                    .sendInvitationCode(token)
                    .onSuccess { sentTo ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                codeStep = true,
                                sentTo = sentTo ?: it.sentTo,
                                code = "",
                            )
                        }
                        startCountdown(RESEND_SECONDS)
                    }.onFailure { error ->
                        _uiState.update { it.copy(isBusy = false, error = error.toActionError("code")) }
                    }
            }
        }

        fun cancelCode() {
            _uiState.update { it.copy(codeStep = false, code = "", error = null) }
        }

        fun accept() {
            val state = _uiState.value
            _uiState.update { it.copy(isBusy = true, error = null) }
            viewModelScope.launch {
                repository
                    .acceptInvitation(token, state.code.takeIf { state.page?.emailProof != null })
                    .onSuccess { joined ->
                        _uiState.update { it.copy(isBusy = false) }
                        _effects.send(InvitationEffect.Joined(joined))
                    }.onFailure { error ->
                        val field = if ((error as? ApiError)?.fieldError("code") != null) "code" else "invitation"
                        _uiState.update { it.copy(isBusy = false, error = error.toActionError(field)) }
                    }
            }
        }

        /** "Akun berbeda": sign out, then sign in with the invited account. */
        fun signOut() {
            viewModelScope.launch {
                auth.signOut()
                _effects.send(InvitationEffect.SignedOut)
            }
        }

        private fun startCountdown(seconds: Int) {
            countdown?.cancel()
            countdown =
                viewModelScope.launch {
                    for (left in seconds downTo 0) {
                        _uiState.update { it.copy(resendIn = left) }
                        delay(SECOND_MS)
                    }
                }
        }

        private companion object {
            const val CODE_LENGTH = 6
            const val RESEND_SECONDS = 60
            const val SECOND_MS = 1_000L
        }
    }
