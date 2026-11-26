package com.gotest.app.feature.auth.ui.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.GoogleSignIn
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import com.gotest.app.feature.auth.ui.components.GoogleIdTokenResult
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
import java.io.IOException
import javax.inject.Inject

/** Daftar with email (web: Auth/Register.tsx): the form, then "Cek email kamu" while email codes are on. */
@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val flows: AuthFlowRepository,
        private val signUps: SignUpRepository,
        private val validator: CredentialValidator,
        private val options: SignInOptionsRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        // The form comes back after Android closed the app (T-026 required 10); the password never is kept.
        private val _uiState =
            MutableStateFlow(
                RegisterUiState(
                    fullName = savedState[KEY_NAME] ?: "",
                    email = savedState[KEY_EMAIL] ?: "",
                    level = savedState.get<String>(KEY_LEVEL)?.let(::levelNamed),
                ),
            )
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
                // A sign-up still waiting for its email code opens on "Cek email kamu" again.
                if (_uiState.value.codeStep == null) signUps.pendingEmailCode()?.let { handle(it) }
            }
        }

        fun onEvent(event: RegisterUiEvent) {
            when (event) {
                RegisterUiEvent.Submit -> {
                    submit()
                }

                RegisterUiEvent.ConfirmCode -> {
                    confirmCode()
                }

                RegisterUiEvent.ResendCode -> {
                    resendCode()
                }

                RegisterUiEvent.ChangeEmail -> {
                    leaveCodeStep()
                }

                RegisterUiEvent.DropJoinCode -> {
                    dropJoinCode()
                }

                // The route opens Google's picker; its answer comes back as GoogleResult.
                RegisterUiEvent.GoogleSignUp -> {}

                is RegisterUiEvent.GoogleResult -> {
                    signUpWithGoogle(event.result)
                }

                else -> {
                    _uiState.update { it.edit(event) }
                    keepForm()
                }
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
                                codeErrorRes = null,
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
                    formError = null,
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

        /**
         * A wrong code stays on "Cek email kamu"; anything about the form goes back to it with every field's text,
         * as on the web. No connection keeps the step where it is.
         */
        private fun showError(error: Throwable) {
            val messages = error.fieldMessages()
            val code = messages["code"]
            val aboutTheCode = error is IOException || (code != null && messages.size == 1)
            if (_uiState.value.codeStep != null && aboutTheCode) {
                val offline = R.string.auth_error_offline.takeIf { error is IOException }
                _uiState.update { it.copy(codeStep = it.codeStep?.copy(codeMessage = code, codeErrorRes = offline)) }
                return
            }
            leaveCodeStep()
            _uiState.update { state ->
                messages.entries.fold(state.copy(formError = error.formErrorRes(messages))) { next, (field, text) ->
                    when (field) {
                        "guardian_phone", "guardian_email" -> {
                            next.copy(guardian = next.guardian.copy(contactMessage = text))
                        }

                        in FIELDS -> {
                            next.copy(messages = next.messages + (field to text))
                        }

                        else -> {
                            next.copy(messages = next.messages + (FORM to text))
                        }
                    }
                }
            }
        }

        private fun keepForm() {
            val state = _uiState.value
            savedState[KEY_NAME] = state.fullName
            savedState[KEY_EMAIL] = state.email
            savedState[KEY_LEVEL] = state.level?.name
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

        /** An existing Google account signs in, as on the web; a new one finishes on "Satu langkah lagi". */
        private fun signUpWithGoogle(picked: GoogleIdTokenResult) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(messages = emptyMap(), formError = null) }
            when (picked) {
                GoogleIdTokenResult.Cancelled -> {
                    return
                }

                GoogleIdTokenResult.Failed -> {
                    _uiState.update { it.copy(formError = R.string.auth_google_failed) }
                }

                is GoogleIdTokenResult.Token -> {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isSubmitting = true) }
                        flows
                            .signInWithGoogle(picked.value)
                            .onSuccess { result ->
                                _effects.send(
                                    when (result) {
                                        GoogleSignIn.SignedIn -> {
                                            RegisterEffect.Registered
                                        }

                                        is GoogleSignIn.NeedsRegistration -> {
                                            RegisterEffect.GoogleRegister(
                                                result.name,
                                                result.email,
                                            )
                                        }
                                    },
                                )
                            }.onFailure { error ->
                                // The web shows a refused Google sign-in under the email field.
                                val refusal = error as? AuthFlowError
                                _uiState.update {
                                    when {
                                        refusal == null -> {
                                            it.copy(formError = R.string.auth_google_failed)
                                        }

                                        refusal.field == AuthFlowError.FORM -> {
                                            it.copy(
                                                messages =
                                                    mapOf(FORM to refusal.message),
                                            )
                                        }

                                        else -> {
                                            it.copy(messages = mapOf(EMAIL to refusal.message))
                                        }
                                    }
                                }
                            }
                        _uiState.update { it.copy(isSubmitting = false) }
                    }
                }
            }
        }

        private companion object {
            const val CODE_LENGTH = 6
            const val SECOND_MILLIS = 1_000L
            const val FORM = "form"
            const val EMAIL = "email"
            const val KEY_NAME = "register_name"
            const val KEY_EMAIL = "register_email"
            const val KEY_LEVEL = "register_level"
            val FIELDS = setOf("name", "email", "password", "education_level")
        }
    }

private fun levelNamed(name: String): EducationLevel? = EducationLevel.entries.find { it.name == name }

/** The server's texts by field: an AuthFlowError's one field, or every field of a refused ApiError. */
private fun Throwable.fieldMessages(): Map<String, String> =
    when (this) {
        is AuthFlowError -> {
            mapOf(field to message)
        }

        is ApiError -> {
            fieldErrors
                .mapNotNull { (field, texts) -> texts.firstOrNull()?.let { field to it } }
                .toMap()
                .ifEmpty { mapOf("form" to message) }
        }

        else -> {
            emptyMap()
        }
    }

/** The app's own text when the server said nothing readable. */
private fun Throwable.formErrorRes(messages: Map<String, String>): Int? =
    when {
        messages.isNotEmpty() -> null
        this is IOException -> R.string.auth_error_offline
        else -> R.string.auth_error_server
    }
