package com.gotest.app.feature.auth.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import com.gotest.app.feature.auth.ui.components.check
import com.gotest.app.feature.auth.ui.components.messageRes
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

/** Daftar with email (web: Auth/Register.tsx): the form, then "Cek email kamu" while email codes are on. */
@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val signUps: SignUpRepository,
        private val validator: CredentialValidator,
        private val options: SignInOptionsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RegisterUiState())
        val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

        private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
        val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

        private var countdown: Job? = null

        init {
            viewModelScope.launch {
                val info = signUps.registrationInfo()
                val google = options.options().google
                _uiState.update {
                    it.copy(
                        guardianQuestion = info.guardianQuestion,
                        emailCode = info.emailCode,
                        joinNotice = info.joinNotice,
                        showGoogle = google,
                    )
                }
            }
        }

        fun onEvent(event: RegisterUiEvent) {
            when (event) {
                RegisterUiEvent.Submit -> submit()
                RegisterUiEvent.ConfirmCode -> confirmCode()
                RegisterUiEvent.ResendCode -> resendCode()
                RegisterUiEvent.ChangeEmail -> leaveCodeStep()
                RegisterUiEvent.DropJoinCode -> dropJoinCode()
                RegisterUiEvent.GoogleSignUp -> signUpWithGoogle()
                else -> _uiState.update { it.edit(event) }
            }
        }

        private fun RegisterUiState.edit(event: RegisterUiEvent): RegisterUiState =
            when (event) {
                is RegisterUiEvent.FullNameChanged -> {
                    copy(
                        fullName = event.value,
                        fullNameError = null,
                        messages =
                            messages - "name",
                    )
                }

                is RegisterUiEvent.EmailChanged -> {
                    copy(
                        email = event.value,
                        emailError = null,
                        messages =
                            messages - "email",
                    )
                }

                is RegisterUiEvent.PasswordChanged -> {
                    copy(
                        password = event.value,
                        passwordError = null,
                        messages =
                            messages - "password",
                    )
                }

                is RegisterUiEvent.ConfirmationChanged -> {
                    copy(confirmation = event.value, confirmationError = null)
                }

                is RegisterUiEvent.LevelPicked -> {
                    copy(
                        level = event.level,
                        levelError = null,
                        messages =
                            messages - "education_level",
                    )
                }

                is RegisterUiEvent.GuardianChanged -> {
                    copy(guardian = event.state)
                }

                is RegisterUiEvent.CodeChanged -> {
                    copy(
                        codeStep =
                            codeStep?.copy(
                                code = event.value.filter(Char::isDigit).take(CODE_LENGTH),
                                codeMessage = null,
                            ),
                    )
                }

                else -> {
                    this
                }
            }

        private fun submit() {
            val state = _uiState.value
            if (state.isSubmitting) return
            val question = state.guardianQuestion.takeIf { state.asksGuardian }
            val (answer, guardian) = question?.let(state.guardian::check) ?: (null to state.guardian)
            val validated =
                state.copy(
                    fullNameError = validator.validateRequired(state.fullName)?.messageRes(),
                    emailError = validator.validateEmail(state.email)?.messageRes(),
                    passwordError = validator.validateNewPassword(state.password)?.messageRes(),
                    confirmationError =
                        validator
                            .validateConfirmation(
                                state.password,
                                state.confirmation,
                            )?.messageRes(),
                    levelError = if (state.level == null) R.string.education_required else null,
                    guardian = guardian,
                    messages = emptyMap(),
                )
            val level = state.level
            val hasError =
                listOf(
                    validated.fullNameError,
                    validated.emailError,
                    validated.passwordError,
                    validated.confirmationError,
                    validated.levelError,
                ).any { it != null } || (question != null && answer == null)
            if (hasError || level == null) {
                _uiState.value = validated
                return
            }
            _uiState.value = validated.copy(isSubmitting = true)
            viewModelScope.launch {
                signUps
                    .register(SignUpForm(state.fullName.trim(), state.email.trim(), state.password, level, answer))
                    .onSuccess { handle(it) }
                    .onFailure { error ->
                        showError(error)
                        // As on the web, a refused form with email codes asks for the password again.
                        if (state.emailCode) _uiState.update { it.copy(password = "", confirmation = "") }
                    }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private fun confirmCode() {
            val state = _uiState.value
            val step = state.codeStep ?: return
            if (state.isSubmitting || step.code.length != CODE_LENGTH) return
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                signUps
                    .confirmEmailCode(step.code)
                    .onSuccess { handle(it) }
                    .onFailure { error ->
                        _uiState.update { it.copy(codeStep = it.codeStep?.copy(code = "")) }
                        showError(error)
                    }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private fun resendCode() {
            val step = _uiState.value.codeStep ?: return
            if (step.resendInSeconds > 0) return
            viewModelScope.launch {
                signUps
                    .resendEmailCode()
                    .onSuccess { seconds ->
                        _uiState.update { it.copy(codeStep = it.codeStep?.copy(code = "", codeMessage = null)) }
                        startCountdown(seconds)
                    }.onFailure(::showError)
            }
        }

        private suspend fun handle(result: SignUpResult) {
            when (result) {
                SignUpResult.SignedIn -> {
                    _effects.send(RegisterEffect.Registered)
                }

                SignUpResult.AwaitingConsent -> {
                    _effects.send(RegisterEffect.AwaitingConsent)
                }

                is SignUpResult.EmailCodeSent -> {
                    _uiState.update { it.copy(codeStep = RegisterCodeStep(sentTo = result.sentTo)) }
                    startCountdown(result.resendInSeconds)
                }
            }
        }

        /** A wrong code stays on "Cek email kamu"; anything about the form goes back to it, as on the web. */
        private fun showError(error: Throwable) {
            val field = (error as? AuthFlowError)?.field ?: FORM
            val message = error.message.orEmpty()
            if (field == "code" && _uiState.value.codeStep != null) {
                _uiState.update { it.copy(codeStep = it.codeStep?.copy(codeMessage = message)) }
                return
            }
            leaveCodeStep()
            _uiState.update {
                when (field) {
                    "guardian_phone", "guardian_email" -> it.copy(guardian = it.guardian.copy(contactMessage = message))
                    in FIELDS -> it.copy(messages = it.messages + (field to message))
                    else -> it.copy(messages = it.messages + (FORM to message))
                }
            }
        }

        private fun leaveCodeStep() {
            countdown?.cancel()
            _uiState.update { it.copy(codeStep = null) }
        }

        private fun dropJoinCode() {
            viewModelScope.launch {
                signUps.dropJoinCode()
                _uiState.update { it.copy(joinNotice = null) }
            }
        }

        private fun startCountdown(seconds: Int) {
            countdown?.cancel()
            countdown =
                viewModelScope.launch {
                    for (left in seconds downTo 0) {
                        _uiState.update { it.copy(codeStep = it.codeStep?.copy(resendInSeconds = left)) }
                        if (left > 0) delay(SECOND_MILLIS)
                    }
                }
        }

        private fun signUpWithGoogle() {
            if (_uiState.value.isSubmitting) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true) }
                authRepository.signInWithGoogle().onSuccess { _effects.send(RegisterEffect.Registered) }
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }

        private companion object {
            const val CODE_LENGTH = 6
            const val SECOND_MILLIS = 1_000L
            const val FORM = "form"
            val FIELDS = setOf("name", "email", "password", "education_level")
        }
    }
