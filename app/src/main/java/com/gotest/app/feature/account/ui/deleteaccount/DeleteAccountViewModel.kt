package com.gotest.app.feature.account.ui.deleteaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.account.domain.AccountValidator
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

@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val validator: AccountValidator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeleteAccountUiState())
        val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

        private val _effects = Channel<DeleteAccountEffect>(Channel.BUFFERED)
        val effects: Flow<DeleteAccountEffect> = _effects.receiveAsFlow()

        fun onEvent(event: DeleteAccountUiEvent) {
            when (event) {
                is DeleteAccountUiEvent.ConfirmationChange -> {
                    _uiState.update {
                        it.copy(confirmation = event.value, canDelete = validator.isDeleteConfirmed(event.value))
                    }
                }

                DeleteAccountUiEvent.Delete -> {
                    delete()
                }
            }
        }

        private fun delete() {
            val state = _uiState.value
            if (!state.canDelete || state.isDeleting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isDeleting = true) }
                userRepository
                    .deleteAccount()
                    .onSuccess { _effects.send(DeleteAccountEffect.AccountDeleted) }
                    .onFailure { _effects.send(DeleteAccountEffect.ShowMessage(R.string.account_delete_error)) }
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }
