package com.gotest.app.feature.account.ui.sessions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.account.domain.AccountSecurityRepository
import com.gotest.app.feature.account.domain.ActiveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Keluar dari perangkat lain?" with the current password. */
@Immutable
data class RevokeSessionsForm(
    val password: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
)

@Immutable
data class ActiveSessionsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val sessions: List<ActiveSession> = emptyList(),
    val revoke: RevokeSessionsForm? = null,
) {
    val otherCount: Int get() = sessions.count { !it.current }
}

sealed interface ActiveSessionsUiEvent {
    data object Retry : ActiveSessionsUiEvent

    data object OpenRevoke : ActiveSessionsUiEvent

    data object DismissRevoke : ActiveSessionsUiEvent

    data class PasswordChanged(
        val value: String,
    ) : ActiveSessionsUiEvent

    data object ConfirmRevoke : ActiveSessionsUiEvent
}

/** "Sesi Aktif" on Profil (web: Components/Student/Profile/ActiveSessionsPanel.tsx, DEC-102, API-K5). */
@HiltViewModel
class ActiveSessionsViewModel
    @Inject
    constructor(
        private val security: AccountSecurityRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ActiveSessionsUiState())
        val uiState: StateFlow<ActiveSessionsUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun onEvent(event: ActiveSessionsUiEvent) {
            when (event) {
                ActiveSessionsUiEvent.Retry -> {
                    load()
                }

                ActiveSessionsUiEvent.OpenRevoke -> {
                    _uiState.update { it.copy(revoke = RevokeSessionsForm()) }
                }

                ActiveSessionsUiEvent.DismissRevoke -> {
                    _uiState.update { it.copy(revoke = null) }
                }

                is ActiveSessionsUiEvent.PasswordChanged -> {
                    _uiState.update { it.copy(revoke = it.revoke?.copy(password = event.value, errorMessage = null)) }
                }

                ActiveSessionsUiEvent.ConfirmRevoke -> {
                    confirmRevoke()
                }
            }
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                security
                    .sessions()
                    .onSuccess { sessions -> _uiState.update { it.copy(isLoading = false, sessions = sessions) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }

        private fun confirmRevoke() {
            val form = _uiState.value.revoke ?: return
            if (form.isSubmitting) return
            _uiState.update { it.copy(revoke = form.copy(isSubmitting = true, errorMessage = null)) }
            viewModelScope.launch {
                security
                    .revokeOtherSessions(form.password)
                    .onSuccess {
                        _uiState.update { it.copy(revoke = null) }
                        load()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                revoke =
                                    it.revoke?.copy(
                                        isSubmitting = false,
                                        password = "",
                                        errorMessage = error.message,
                                    ),
                            )
                        }
                    }
            }
        }
    }
