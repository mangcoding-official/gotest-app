package com.gotest.app.feature.account.ui.deleteaccount

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.ui.components.fieldMessage
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

@Immutable
data class DeleteAccountUiState(
    val isLoading: Boolean = true,
    /** GET /profil did not reach the server; a server refusal still shows the form. */
    val loadFailed: Boolean = false,
    /** Active Mitra where this account is the only manager (`soleManagerOf` of GET /profil). */
    val soleManagerOf: List<String> = emptyList(),
    val confirmation: String = "",
    val error: String? = null,
    val isDeleting: Boolean = false,
)

sealed interface DeleteAccountEffect {
    data object AccountDeleted : DeleteAccountEffect

    /** The request did not reach the server. */
    data object Failed : DeleteAccountEffect
}

/**
 * "Hapus Akun" (web: Components/Student/Profile/DeleteAccountModal.tsx). The server checks the typed
 * "gotest.id"; once deleted every token is gone, so the app forgets its own. GET /profil only adds the
 * sole-manager paragraph: an account waiting for, refused, or withdrawn parental approval reaches only
 * DELETE /profil (API-K6 = B), so the form shows when the server refuses /profil (A2e review 1). Without a
 * connection the page asks to try again, so the sole-manager paragraph is not lost silently (A2e review ulang).
 */
@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val profilePages: ProfilePagesRepository,
        private val session: SessionStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeleteAccountUiState())
        val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

        private val _effects = Channel<DeleteAccountEffect>(Channel.BUFFERED)
        val effects: Flow<DeleteAccountEffect> = _effects.receiveAsFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                val page = profilePages.page()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = page.exceptionOrNull()?.let { error -> error !is ApiError } ?: false,
                        soleManagerOf = page.getOrNull()?.soleManagerOf.orEmpty(),
                    )
                }
            }
        }

        fun onConfirmationChange(value: String) = _uiState.update { it.copy(confirmation = value, error = null) }

        fun delete() {
            val state = _uiState.value
            if (state.isDeleting) return
            _uiState.update { it.copy(isDeleting = true, error = null) }
            viewModelScope.launch {
                profilePages
                    .deleteAccount(state.confirmation)
                    .onSuccess {
                        session.clear()
                        _effects.send(DeleteAccountEffect.AccountDeleted)
                    }.onFailure { error ->
                        _uiState.update { it.copy(isDeleting = false, error = error.fieldMessage(CONFIRMATION)) }
                        if (error !is ApiError) _effects.send(DeleteAccountEffect.Failed)
                    }
            }
        }

        private companion object {
            const val CONFIRMATION = "confirmation"
        }
    }

/** Web: one name as it is; more as "A, B dan C". */
internal fun joinNames(names: List<String>): String =
    if (names.size <= 1) {
        names.firstOrNull().orEmpty()
    } else {
        names.dropLast(1).joinToString(", ") + " dan " + names.last()
    }
