package com.gotest.app.feature.account.ui.phone

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.ui.components.fieldMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The web modal's three steps: the saved number, a new number, and its code. */
enum class PhoneStep { View, Phone, Code }

@Immutable
data class AccountPhoneUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** The saved number in display form, or null. */
    val current: String? = null,
    /** A new number needs WhatsApp codes on the server (the web's `phoneSignIn`). */
    val canAdd: Boolean = false,
    /** Without an email the number is the only way in, so it cannot be removed. */
    val canRemove: Boolean = false,
    val step: PhoneStep = PhoneStep.Phone,
    val phone: String = "",
    val phoneMessage: String? = null,
    val sentTo: String = "",
    val code: String = "",
    val codeMessage: String? = null,
    val isSubmitting: Boolean = false,
) {
    val canSend: Boolean get() = phone.isNotBlank() && !isSubmitting
    val canSave: Boolean get() = code.length == CODE_LENGTH && !isSubmitting

    companion object {
        const val CODE_LENGTH = 6
    }
}

sealed interface AccountPhoneUiEvent {
    data object Retry : AccountPhoneUiEvent

    data class PhoneChanged(
        val value: String,
    ) : AccountPhoneUiEvent

    data class CodeChanged(
        val value: String,
    ) : AccountPhoneUiEvent

    data object SendCode : AccountPhoneUiEvent

    data object Save : AccountPhoneUiEvent

    data object Remove : AccountPhoneUiEvent

    /** "Ganti nomor": to the number step, from the saved number or from the code. */
    data object ChangeNumber : AccountPhoneUiEvent

    /** "Kembali": from the number step back to the saved number. */
    data object BackToCurrent : AccountPhoneUiEvent
}

sealed interface AccountPhoneEffect {
    /** The server's confirmation, such as "Nomor WhatsApp dihapus dari akun.". */
    data class ShowMessage(
        /** Null when the request did not reach the server: the screen says "try again". */
        val message: String?,
    ) : AccountPhoneEffect

    /** Removed while WhatsApp codes are off: nothing is left to do here, as the web closes its modal. */
    data object Close : AccountPhoneEffect
}

/** "Nomor WhatsApp" on Profil (web: Components/Student/Profile/PhoneModal.tsx, DEC-123). */
@HiltViewModel
class AccountPhoneViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val profilePages: ProfilePagesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AccountPhoneUiState())
        val uiState: StateFlow<AccountPhoneUiState> = _uiState.asStateFlow()

        private val _effects = Channel<AccountPhoneEffect>(Channel.BUFFERED)
        val effects: Flow<AccountPhoneEffect> = _effects.receiveAsFlow()

        private var loading: Job? = null

        init {
            load()
        }

        fun onEvent(event: AccountPhoneUiEvent) {
            when (event) {
                AccountPhoneUiEvent.Retry -> {
                    load()
                }

                is AccountPhoneUiEvent.PhoneChanged -> {
                    _uiState.update { it.copy(phone = event.value, phoneMessage = null) }
                }

                is AccountPhoneUiEvent.CodeChanged -> {
                    _uiState.update {
                        it.copy(
                            code = event.value.filter(Char::isDigit).take(AccountPhoneUiState.CODE_LENGTH),
                            codeMessage = null,
                        )
                    }
                }

                AccountPhoneUiEvent.SendCode -> {
                    sendCode()
                }

                AccountPhoneUiEvent.Save -> {
                    save()
                }

                AccountPhoneUiEvent.Remove -> {
                    remove()
                }

                AccountPhoneUiEvent.ChangeNumber -> {
                    _uiState.update {
                        it.copy(
                            step = PhoneStep.Phone,
                            code = "",
                            codeMessage = null,
                            phoneMessage = null,
                        )
                    }
                }

                AccountPhoneUiEvent.BackToCurrent -> {
                    _uiState.update { it.copy(step = PhoneStep.View, phone = "", phoneMessage = null) }
                }
            }
        }

        private fun load() {
            loading?.cancel()
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            loading =
                viewModelScope.launch {
                    // `phoneSignIn` of GET /profil; the number itself comes from /akun.
                    val canAdd =
                        profilePages
                            .page()
                            .getOrElse {
                                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                                return@launch
                            }.phoneSignIn
                    userRepository
                        .observeProfile()
                        .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                        .collect { profile ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    current = profile.phone,
                                    canAdd = canAdd,
                                    canRemove = profile.hasEmail,
                                    // The first load opens on the saved number, or on adding one.
                                    step =
                                        if (it.isLoading) {
                                            (
                                                if (profile.phone !=
                                                    null
                                                ) {
                                                    PhoneStep.View
                                                } else {
                                                    PhoneStep.Phone
                                                }
                                            )
                                        } else {
                                            it.step
                                        },
                                )
                            }
                        }
                }
        }

        private fun sendCode() {
            val state = _uiState.value
            if (!state.canSend) return
            submit {
                profilePages
                    .sendPhoneCode(state.phone)
                    .onSuccess { reply ->
                        // The web names the number as typed (PhoneModal.tsx); the reply's form is the fallback.
                        val sentTo = state.phone.trim().ifEmpty { reply.phoneCodeSentTo.orEmpty() }
                        _uiState.update { it.copy(step = PhoneStep.Code, sentTo = sentTo, code = "") }
                    }.onFailure { error -> _uiState.update { it.copy(phoneMessage = error.fieldMessage(PHONE)) } }
            }
        }

        private fun save() {
            val state = _uiState.value
            if (!state.canSave) return
            submit {
                profilePages
                    .savePhone(state.phone, state.code)
                    .onSuccess { reply ->
                        _uiState.update { it.copy(step = PhoneStep.View, phone = "", code = "") }
                        reply.message?.let { _effects.send(AccountPhoneEffect.ShowMessage(it)) }
                    }.onFailure { error ->
                        val phoneError = (error as? ApiError)?.fieldError(PHONE)
                        _uiState.update {
                            if (phoneError != null) {
                                it.copy(step = PhoneStep.Phone, phoneMessage = phoneError, code = "")
                            } else {
                                it.copy(codeMessage = error.fieldMessage(CODE), code = "")
                            }
                        }
                    }
            }
        }

        private fun remove() {
            submit {
                profilePages
                    .removePhone()
                    .onSuccess { reply ->
                        val message = reply.message
                        _uiState.update { it.copy(step = PhoneStep.Phone) }
                        _effects.send(
                            if (_uiState.value.canAdd) {
                                AccountPhoneEffect.ShowMessage(
                                    message,
                                )
                            } else {
                                AccountPhoneEffect.Close
                            },
                        )
                    }.onFailure { error -> _effects.send(AccountPhoneEffect.ShowMessage(error.fieldMessage(PHONE))) }
            }
        }

        private fun submit(block: suspend () -> Unit) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                block()
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private companion object {
            const val PHONE = "phone"
            const val CODE = "code"
        }
    }
