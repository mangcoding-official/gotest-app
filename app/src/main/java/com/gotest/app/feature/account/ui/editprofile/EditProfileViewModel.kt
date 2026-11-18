package com.gotest.app.feature.account.ui.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfileUpdate
import com.gotest.app.feature.account.domain.apiCode
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.ProfilePhotoEditor
import com.gotest.app.feature.account.ui.components.fieldMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val SECOND_MS = 1_000L

/** What the student typed; null fields still show the saved account value. */
private data class EditProfileForm(
    val name: String? = null,
    val email: String? = null,
    val educationLevel: EducationLevel? = null,
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val codeStep: EmailCodeStep? = null,
)

/** "Edit Informasi Akun" (web: Components/Student/Profile/EditAccountModal.tsx and EmailCodeForm.tsx). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val profilePages: ProfilePagesRepository,
        private val photoEditor: ProfilePhotoEditor,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val form = MutableStateFlow(EditProfileForm())
        private var countdown: Job? = null

        private val content: Flow<EditProfileUiState> =
            retryKey.flatMapLatest {
                // Name, email, photo and level come from /akun; `emailCode` from GET /profil.
                val page = flow { emit(profilePages.page().getOrThrow()) }
                userRepository
                    .observeProfile()
                    .combine(page) { profile, profilePage ->
                        EditProfileUiState.Success(
                            avatarUrl = profile.avatarUrl,
                            name = profile.fullName,
                            email = profile.email,
                            educationLevel = profile.educationLevel,
                            emailCode = profilePage.emailCode,
                        ) as EditProfileUiState
                    }.onStart { emit(EditProfileUiState.Loading) }
                    .catch { emit(EditProfileUiState.Error) }
            }

        val uiState: StateFlow<EditProfileUiState> =
            combine(content, form, photoEditor.sheet) { state, input, sheet ->
                if (state is EditProfileUiState.Success) {
                    state.copy(
                        name = input.name ?: state.name,
                        email = input.email ?: state.email,
                        educationLevel = input.educationLevel ?: state.educationLevel,
                        errors = input.errors,
                        isSaving = input.isSaving,
                        codeStep = input.codeStep,
                        photoSheet = sheet,
                    )
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EditProfileUiState.Loading)

        private val _effects = Channel<EditProfileEffect>(Channel.BUFFERED)
        val effects: Flow<EditProfileEffect> = _effects.receiveAsFlow()

        fun onEvent(event: EditProfileUiEvent) {
            when (event) {
                EditProfileUiEvent.Retry -> {
                    retryKey.update { it + 1 }
                }

                is EditProfileUiEvent.NameChange -> {
                    form.update { it.copy(name = event.value, errors = it.errors - NAME) }
                }

                is EditProfileUiEvent.EmailChange -> {
                    form.update {
                        it.copy(
                            email = event.value,
                            errors =
                                it.errors - EMAIL,
                        )
                    }
                }

                is EditProfileUiEvent.LevelChange -> {
                    form.update {
                        it.copy(
                            educationLevel = event.value,
                            errors =
                                it.errors - LEVEL,
                        )
                    }
                }

                EditProfileUiEvent.Submit -> {
                    submit()
                }

                is EditProfileUiEvent.CodeChange -> {
                    onCodeChange(event.value)
                }

                EditProfileUiEvent.SaveEmail -> {
                    saveEmail()
                }

                EditProfileUiEvent.ResendCode -> {
                    resendCode()
                }

                EditProfileUiEvent.CancelCode -> {
                    cancelCode()
                }

                else -> {
                    onPhotoEvent(event)
                }
            }
        }

        private fun onPhotoEvent(event: EditProfileUiEvent) {
            when (event) {
                EditProfileUiEvent.ChangePhotoClick -> photoEditor.open()
                EditProfileUiEvent.TakePhoto -> photoEditor.openCamera()
                is EditProfileUiEvent.PhotoCapture -> onCaptureEvent(event.event)
                is EditProfileUiEvent.PhotoPick -> photoEditor.select(event.photoUri)
                EditProfileUiEvent.SavePhoto -> savePhoto()
                EditProfileUiEvent.DeletePhoto -> deletePhoto()
                EditProfileUiEvent.DismissPhotoSheet -> photoEditor.dismiss()
                else -> Unit
            }
        }

        /** PATCH /profil; with `emailCodeSentTo` the new email waits for its code (name and level are saved). */
        private fun submit() {
            val state = uiState.value as? EditProfileUiState.Success
            if (state == null || state.isSaving) return
            form.update { it.copy(isSaving = true, errors = emptyMap()) }
            viewModelScope.launch {
                profilePages
                    .update(ProfileUpdate(state.name, state.email, state.educationLevel?.apiCode.orEmpty()))
                    .onSuccess { reply ->
                        val sentTo = reply.emailCodeSentTo
                        if (sentTo == null) {
                            form.update { it.copy(isSaving = false) }
                            _effects.send(EditProfileEffect.Saved)
                        } else {
                            form.update {
                                it.copy(
                                    isSaving = false,
                                    codeStep = EmailCodeStep(email = state.email.trim(), sentTo = sentTo),
                                )
                            }
                            startCountdown()
                        }
                    }.onFailure { error -> onFieldsRefused(error) }
            }
        }

        private suspend fun onFieldsRefused(error: Throwable) {
            val fields = (error as? ApiError)?.fieldErrors?.mapValues { it.value.first() }.orEmpty()
            form.update { it.copy(isSaving = false, errors = fields) }
            if (fields.isEmpty()) _effects.send(EditProfileEffect.ShowText((error as? ApiError)?.message))
        }

        private fun onCodeChange(value: String) =
            updateCode { it.copy(code = value.filter(Char::isDigit).take(EmailCodeStep.CODE_LENGTH), codeError = null) }

        /** PUT /profil/email: "Email baru tersimpan." closes the screen. */
        private fun saveEmail() {
            val step = form.value.codeStep ?: return
            if (step.isSaving) return
            updateCode { it.copy(isSaving = true, codeError = null) }
            viewModelScope.launch {
                profilePages
                    .saveEmail(step.email, step.code)
                    .onSuccess {
                        countdown?.cancel()
                        form.update { EditProfileForm() }
                        _effects.send(EditProfileEffect.Saved)
                    }.onFailure { error ->
                        val message = error.fieldMessage(CODE)
                        updateCode { it.copy(isSaving = false, code = "", codeError = message) }
                        if (message == null) _effects.send(EditProfileEffect.ShowText(null))
                    }
            }
        }

        /** POST /profil/email/kode; the API puts its refusal under `email`, shown under the code (spec Q4). */
        private fun resendCode() {
            val step = form.value.codeStep ?: return
            if (step.resendIn > 0 || step.isSaving) return
            viewModelScope.launch {
                profilePages
                    .resendEmailCode(step.email)
                    .onSuccess { reply ->
                        updateCode { it.copy(sentTo = reply.emailCodeSentTo ?: it.sentTo, code = "", codeError = null) }
                        startCountdown()
                    }.onFailure { error ->
                        val message = error.fieldMessage(EMAIL)
                        updateCode { it.copy(codeError = message) }
                        if (message == null) _effects.send(EditProfileEffect.ShowText(null))
                    }
            }
        }

        private fun cancelCode() {
            countdown?.cancel()
            form.update { it.copy(codeStep = null) }
        }

        private fun startCountdown() {
            countdown?.cancel()
            updateCode { it.copy(resendIn = EmailCodeStep.RESEND_SECONDS) }
            countdown =
                viewModelScope.launch {
                    while ((form.value.codeStep?.resendIn ?: 0) > 0) {
                        delay(SECOND_MS)
                        updateCode { it.copy(resendIn = (it.resendIn - 1).coerceAtLeast(0)) }
                    }
                }
        }

        private fun updateCode(transform: (EmailCodeStep) -> EmailCodeStep) =
            form.update { input -> input.codeStep?.let { input.copy(codeStep = transform(it)) } ?: input }

        private fun savePhoto() {
            viewModelScope.launch {
                val result = photoEditor.save() ?: return@launch
                val effect =
                    result.fold(
                        { EditProfileEffect.ShowMessage(R.string.account_notice_photo_saved) },
                        { EditProfileEffect.ShowText(it.fieldMessage(PHOTO), host = photoEditor.messageHost) },
                    )
                _effects.send(effect)
            }
        }

        private fun deletePhoto() {
            viewModelScope.launch {
                photoEditor.delete()?.onFailure {
                    _effects.send(EditProfileEffect.ShowText(it.fieldMessage(PHOTO), host = photoEditor.messageHost))
                }
            }
        }

        private fun onCaptureEvent(event: PhotoCaptureEvent) {
            viewModelScope.launch {
                photoEditor.onCaptureEvent(event)?.onFailure {
                    _effects.send(
                        EditProfileEffect.ShowMessage(R.string.account_adjust_error, host = photoEditor.messageHost),
                    )
                }
            }
        }

        private companion object {
            const val NAME = "name"
            const val EMAIL = "email"
            const val LEVEL = "education_level"
            const val CODE = "code"
            const val PHOTO = "photo"
        }
    }
