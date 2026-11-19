package com.gotest.app.feature.account.ui.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.account.domain.PasswordChange
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.ui.components.fieldMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Ganti Password" (web: Components/Student/Profile/ChangePasswordModal.tsx). */
@HiltViewModel
class ChangePasswordViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val profilePages: ProfilePagesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChangePasswordUiState())
        val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ChangePasswordEffect>(Channel.BUFFERED)
        val effects: Flow<ChangePasswordEffect> = _effects.receiveAsFlow()

        init {
            load()
        }

        fun onEvent(event: ChangePasswordUiEvent) {
            when (event) {
                ChangePasswordUiEvent.Retry -> {
                    load()
                }

                is ChangePasswordUiEvent.CurrentChange -> {
                    _uiState.update {
                        it.copy(
                            current = event.value,
                            errors =
                                it.errors - CURRENT,
                        )
                    }
                }

                is ChangePasswordUiEvent.PasswordChange -> {
                    _uiState.update {
                        it.copy(
                            password = event.value,
                            errors =
                                it.errors - PASSWORD,
                        )
                    }
                }

                is ChangePasswordUiEvent.ConfirmationChange -> {
                    _uiState.update { it.copy(confirmation = event.value) }
                }

                ChangePasswordUiEvent.Submit -> {
                    submit()
                }

                ChangePasswordUiEvent.SendLink -> {
                    sendLink()
                }
            }
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                val email = runCatching { userRepository.observeProfile().first().email }.getOrDefault("")
                profilePages
                    .page()
                    .onSuccess { page ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                email = email,
                                googleLinked = page.googleLinked,
                                linkByWhatsApp = page.linkByWhatsApp,
                            )
                        }
                    }.onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            _uiState.update { it.copy(isSubmitting = true, errors = emptyMap()) }
            viewModelScope.launch {
                profilePages
                    .changePassword(PasswordChange(state.current, state.password, state.confirmation))
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                current = "",
                                password = "",
                                confirmation = "",
                            )
                        }
                        _effects.send(ChangePasswordEffect.PasswordChanged)
                    }.onFailure { error ->
                        val fields = (error as? ApiError)?.fieldErrors?.mapValues { it.value.first() }.orEmpty()
                        _uiState.update { it.copy(isSubmitting = false, errors = fields) }
                        if (fields.isEmpty()) {
                            _effects.send(
                                ChangePasswordEffect.ShowMessage((error as? ApiError)?.message),
                            )
                        }
                    }
            }
        }

        private fun sendLink() {
            if (_uiState.value.linkSending || _uiState.value.linkSent) return
            _uiState.update { it.copy(linkSending = true, linkError = null) }
            viewModelScope.launch {
                profilePages
                    .sendPasswordLink()
                    .onSuccess { _uiState.update { it.copy(linkSending = false, linkSent = true) } }
                    .onFailure { error ->
                        val message = error.fieldMessage(LINK)
                        _uiState.update { it.copy(linkSending = false, linkError = message) }
                        if (message == null) _effects.send(ChangePasswordEffect.ShowMessage(null))
                    }
            }
        }

        private companion object {
            const val CURRENT = "current_password"
            const val PASSWORD = "password"
            const val LINK = "password_link"
        }
    }
