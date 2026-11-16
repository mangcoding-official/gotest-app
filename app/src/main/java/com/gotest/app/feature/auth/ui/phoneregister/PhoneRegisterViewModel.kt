package com.gotest.app.feature.auth.ui.phoneregister

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.navigation.PhoneRegisterDestination
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
data class PhoneRegisterUiState(
    val phoneDisplay: String = "",
    val fullName: String = "",
    val level: EducationLevel? = null,
    @param:StringRes val nameErrorRes: Int? = null,
    /** The server's own text, such as "Waktu pendaftaran habis. …". */
    val nameMessage: String? = null,
    @param:StringRes val levelErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
)

sealed interface PhoneRegisterUiEvent {
    data class NameChanged(
        val value: String,
    ) : PhoneRegisterUiEvent

    data class LevelPicked(
        val level: EducationLevel,
    ) : PhoneRegisterUiEvent

    data object Submit : PhoneRegisterUiEvent
}

/** "Satu langkah lagi" after a new number's WhatsApp code (web: Auth/PhoneRegister.tsx). */
@HiltViewModel
class PhoneRegisterViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: AuthFlowRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                PhoneRegisterUiState(phoneDisplay = savedStateHandle.toRoute<PhoneRegisterDestination>().phoneDisplay),
            )
        val uiState: StateFlow<PhoneRegisterUiState> = _uiState.asStateFlow()
        private val registered = Channel<Unit>(Channel.BUFFERED)
        val registerSuccess: Flow<Unit> = registered.receiveAsFlow()

        fun onEvent(event: PhoneRegisterUiEvent) {
            when (event) {
                is PhoneRegisterUiEvent.NameChanged -> {
                    _uiState.update { it.copy(fullName = event.value, nameErrorRes = null, nameMessage = null) }
                }

                is PhoneRegisterUiEvent.LevelPicked -> {
                    _uiState.update { it.copy(level = event.level, levelErrorRes = null) }
                }

                PhoneRegisterUiEvent.Submit -> {
                    submit()
                }
            }
        }

        private fun submit() {
            val state = _uiState.value
            val nameError = if (state.fullName.isBlank()) R.string.phone_register_name_required else null
            val levelError = if (state.level == null) R.string.education_required else null
            val level = state.level
            when {
                state.isSubmitting -> {}

                nameError != null || levelError != null || level == null -> {
                    _uiState.update { it.copy(nameErrorRes = nameError, levelErrorRes = levelError) }
                }

                else -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isSubmitting = true) }
                        repository
                            .completePhoneRegistration(state.fullName.trim(), level)
                            .onSuccess { registered.send(Unit) }
                            .onFailure { error -> _uiState.update { it.copy(nameMessage = error.message) } }
                        _uiState.update { it.copy(isSubmitting = false) }
                    }
                }
            }
        }
    }
