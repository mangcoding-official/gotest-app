package com.gotest.app.feature.auth.ui.childlogin

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.FamilyChild
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

/** The three steps of Masuk Anak: family code, pick a name, PIN (web: Auth/ChildLogin.tsx). */
enum class ChildLoginStep { Code, Name, Pin }

@Immutable
data class ChildLoginUiState(
    val step: ChildLoginStep = ChildLoginStep.Code,
    val code: String = "",
    val children: List<FamilyChild> = emptyList(),
    val child: FamilyChild? = null,
    val pin: String = "",
    @param:StringRes val errorRes: Int? = null,
    /** The server's own text (unknown code, wrong or locked PIN). */
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

sealed interface ChildLoginUiEvent {
    data class CodeChanged(
        val value: String,
    ) : ChildLoginUiEvent

    data object SubmitCode : ChildLoginUiEvent

    data class ChildPicked(
        val child: FamilyChild,
    ) : ChildLoginUiEvent

    data object ChangeCode : ChildLoginUiEvent

    data class PinChanged(
        val value: String,
    ) : ChildLoginUiEvent

    data object SubmitPin : ChildLoginUiEvent

    data object NotMe : ChildLoginUiEvent
}

@HiltViewModel
class ChildLoginViewModel
    @Inject
    constructor(
        private val repository: AuthFlowRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChildLoginUiState())
        val uiState: StateFlow<ChildLoginUiState> = _uiState.asStateFlow()
        private val signedIn = Channel<Unit>(Channel.BUFFERED)
        val signInSuccess: Flow<Unit> = signedIn.receiveAsFlow()

        fun onEvent(event: ChildLoginUiEvent) {
            when (event) {
                // Only letters and digits, upper-cased, at most 6 (as the web input does).
                is ChildLoginUiEvent.CodeChanged -> {
                    _uiState.update { it.copy(code = normalizeCode(event.value), errorRes = null, errorMessage = null) }
                }

                ChildLoginUiEvent.SubmitCode -> {
                    submitCode()
                }

                is ChildLoginUiEvent.ChildPicked -> {
                    _uiState.update {
                        it.copy(
                            step = ChildLoginStep.Pin,
                            child = event.child,
                            pin = "",
                            errorRes = null,
                            errorMessage = null,
                        )
                    }
                }

                ChildLoginUiEvent.ChangeCode -> {
                    _uiState.update { ChildLoginUiState(code = it.code) }
                }

                is ChildLoginUiEvent.PinChanged -> {
                    _uiState.update {
                        it.copy(
                            pin = event.value.filter(Char::isDigit).take(PIN_LENGTH),
                            errorRes = null,
                            errorMessage = null,
                        )
                    }
                }

                ChildLoginUiEvent.SubmitPin -> {
                    submitPin()
                }

                ChildLoginUiEvent.NotMe -> {
                    _uiState.update {
                        it.copy(
                            step = ChildLoginStep.Name,
                            child = null,
                            pin = "",
                            errorRes = null,
                            errorMessage = null,
                        )
                    }
                }
            }
        }

        private fun submitCode() {
            val state = _uiState.value
            if (state.isLoading) return
            if (state.code.isBlank()) {
                _uiState.update { it.copy(errorRes = R.string.child_code_required) }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository
                    .findFamily(state.code)
                    .onSuccess { children ->
                        _uiState.update { it.copy(step = ChildLoginStep.Name, children = children) }
                    }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        private fun submitPin() {
            val state = _uiState.value
            val child = state.child
            val error =
                when {
                    state.pin.isEmpty() -> R.string.child_pin_required
                    state.pin.length != PIN_LENGTH -> R.string.child_pin_length
                    else -> null
                }
            when {
                child == null || state.isLoading -> {
                    Unit
                }

                error != null -> {
                    _uiState.update { it.copy(errorRes = error) }
                }

                else -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        repository
                            .signInChild(state.code, child.id, state.pin)
                            .onSuccess { signedIn.send(Unit) }
                            .onFailure { failure ->
                                _uiState.update { it.copy(errorMessage = failure.message, pin = "") }
                            }
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }

        private companion object {
            const val CODE_LENGTH = 6
            const val PIN_LENGTH = 4

            fun normalizeCode(raw: String): String =
                raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(CODE_LENGTH)
        }
    }
