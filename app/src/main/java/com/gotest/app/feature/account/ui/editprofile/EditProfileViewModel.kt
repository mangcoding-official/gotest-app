package com.gotest.app.feature.account.ui.editprofile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.account.domain.AccountValidator
import com.gotest.app.feature.account.domain.ProfileFieldError
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.ProfilePhotoEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** What the student typed; null fields still show the saved profile value. */
private data class EditProfileForm(
    val name: String? = null,
    val email: String? = null,
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val validator: AccountValidator,
        private val photoEditor: ProfilePhotoEditor,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val form = MutableStateFlow(EditProfileForm())

        private val content: Flow<EditProfileUiState> =
            retryKey.flatMapLatest {
                userRepository
                    .observeProfile()
                    .map<UserProfile, EditProfileUiState> { profile ->
                        EditProfileUiState.Success(
                            avatarUrl = profile.avatarUrl,
                            name = profile.fullName,
                            email = profile.email,
                        )
                    }.onStart { emit(EditProfileUiState.Loading) }
                    .catch { emit(EditProfileUiState.Error) }
            }

        val uiState: StateFlow<EditProfileUiState> =
            combine(content, form, photoEditor.sheet) { state, input, sheet ->
                if (state is EditProfileUiState.Success) {
                    state.copy(
                        name = input.name ?: state.name,
                        email = input.email ?: state.email,
                        nameError = input.nameError,
                        emailError = input.emailError,
                        isSaving = input.isSaving,
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
                EditProfileUiEvent.Retry -> retryKey.update { it + 1 }
                is EditProfileUiEvent.NameChange -> form.update { it.copy(name = event.value, nameError = null) }
                is EditProfileUiEvent.EmailChange -> form.update { it.copy(email = event.value, emailError = null) }
                EditProfileUiEvent.Submit -> submit()
                EditProfileUiEvent.ChangePhotoClick -> photoEditor.open()
                EditProfileUiEvent.TakePhoto -> photoEditor.openCamera()
                is EditProfileUiEvent.PhotoCapture -> onCaptureEvent(event.event)
                is EditProfileUiEvent.PhotoPick -> photoEditor.select(event.photoUri)
                EditProfileUiEvent.SavePhoto -> savePhoto()
                EditProfileUiEvent.DismissPhotoSheet -> photoEditor.dismiss()
            }
        }

        private fun submit() {
            val state = uiState.value as? EditProfileUiState.Success
            if (state == null || state.isSaving) return
            val nameError = validator.validateName(state.name)?.messageRes(isEmail = false)
            val emailError = validator.validateEmail(state.email)?.messageRes(isEmail = true)
            if (nameError != null || emailError != null) {
                form.update { it.copy(nameError = nameError, emailError = emailError) }
            } else {
                save(name = state.name.trim(), email = state.email.trim())
            }
        }

        private fun save(
            name: String,
            email: String,
        ) {
            viewModelScope.launch {
                form.update { it.copy(isSaving = true) }
                userRepository
                    .updateProfile(name, email)
                    .onSuccess { _effects.send(EditProfileEffect.Saved) }
                    .onFailure { _effects.send(EditProfileEffect.ShowMessage(R.string.account_error_save)) }
                form.update { it.copy(isSaving = false) }
            }
        }

        private fun savePhoto() {
            viewModelScope.launch {
                val result = photoEditor.save() ?: return@launch
                val effect =
                    if (result.isSuccess) {
                        EditProfileEffect.ShowMessage(R.string.account_notice_photo_saved)
                    } else {
                        EditProfileEffect.ShowMessage(R.string.account_error_save, host = photoEditor.messageHost)
                    }
                _effects.send(effect)
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

        @StringRes
        private fun ProfileFieldError.messageRes(isEmail: Boolean): Int =
            when (this) {
                ProfileFieldError.Required -> {
                    if (isEmail) R.string.account_error_email_required else R.string.account_error_name_required
                }

                ProfileFieldError.InvalidEmail -> {
                    R.string.account_error_email_invalid
                }
            }
    }
