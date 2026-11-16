package com.gotest.app.feature.auth.ui.phoneregister

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import com.gotest.app.feature.auth.ui.components.GuardianFormState
import com.gotest.app.feature.auth.ui.components.asksFor
import com.gotest.app.feature.auth.ui.components.check
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
    val guardianQuestion: GuardianQuestion? = null,
    val guardian: GuardianFormState = GuardianFormState(),
    val isSubmitting: Boolean = false,
) {
    val asksGuardian: Boolean get() = guardianQuestion.asksFor(level)
}

sealed interface PhoneRegisterUiEvent {
    data class NameChanged(
        val value: String,
    ) : PhoneRegisterUiEvent

    data class LevelPicked(
        val level: EducationLevel,
    ) : PhoneRegisterUiEvent

    data class GuardianChanged(
        val state: GuardianFormState,
    ) : PhoneRegisterUiEvent

    data object Submit : PhoneRegisterUiEvent
}

sealed interface PhoneRegisterEffect {
    data object Registered : PhoneRegisterEffect

    /** An under-age account waits for the parent's approval (Aturan 2.15). */
    data object AwaitingConsent : PhoneRegisterEffect
}

/** "Satu langkah lagi" after a new number's WhatsApp code (web: Auth/PhoneRegister.tsx). */
@HiltViewModel
class PhoneRegisterViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: AuthFlowRepository,
        private val signUps: SignUpRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                PhoneRegisterUiState(phoneDisplay = savedStateHandle.toRoute<PhoneRegisterDestination>().phoneDisplay),
            )
        val uiState: StateFlow<PhoneRegisterUiState> = _uiState.asStateFlow()
        private val _effects = Channel<PhoneRegisterEffect>(Channel.BUFFERED)
        val effects: Flow<PhoneRegisterEffect> = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                val question = signUps.registrationInfo().guardianQuestion
                _uiState.update { it.copy(guardianQuestion = question) }
            }
        }

        fun onEvent(event: PhoneRegisterUiEvent) {
            when (event) {
                is PhoneRegisterUiEvent.NameChanged -> {
                    _uiState.update { it.copy(fullName = event.value, nameErrorRes = null, nameMessage = null) }
                }

                is PhoneRegisterUiEvent.LevelPicked -> {
                    _uiState.update { it.copy(level = event.level, levelErrorRes = null) }
                }

                is PhoneRegisterUiEvent.GuardianChanged -> {
                    _uiState.update { it.copy(guardian = event.state) }
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
            val question = state.guardianQuestion.takeIf { state.asksGuardian }
            val (answer, guardian) = question?.let(state.guardian::check) ?: (null to state.guardian)
            val level = state.level
            when {
                state.isSubmitting -> {}

                nameError != null || levelError != null || level == null || (question != null && answer == null) -> {
                    _uiState.update {
                        it.copy(
                            nameErrorRes = nameError,
                            levelErrorRes = levelError,
                            guardian = guardian,
                        )
                    }
                }

                else -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isSubmitting = true) }
                        repository
                            .completePhoneRegistration(state.fullName.trim(), level, answer)
                            .onSuccess { result ->
                                _effects.send(
                                    if (result ==
                                        SignUpResult.AwaitingConsent
                                    ) {
                                        PhoneRegisterEffect.AwaitingConsent
                                    } else {
                                        PhoneRegisterEffect.Registered
                                    },
                                )
                            }.onFailure(::showError)
                        _uiState.update { it.copy(isSubmitting = false) }
                    }
                }
            }
        }

        private fun showError(error: Throwable) {
            val message = error.message
            _uiState.update {
                if ((error as? AuthFlowError)?.field?.startsWith("guardian_") == true) {
                    it.copy(guardian = it.guardian.copy(contactMessage = message))
                } else {
                    it.copy(nameMessage = message)
                }
            }
        }
    }
