package com.gotest.app.feature.account.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.UserProfile
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

private data class SignOutState(
    val showDialog: Boolean = false,
    val inProgress: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
        private val photoEditor: ProfilePhotoEditor,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val signOut = MutableStateFlow(SignOutState())

        private val content: Flow<ProfileUiState> =
            retryKey.flatMapLatest {
                userRepository
                    .observeProfile()
                    .map<UserProfile, ProfileUiState> { profile ->
                        ProfileUiState.Success(profile = profile, daysRemaining = daysUntil(profile))
                    }.onStart { emit(ProfileUiState.Loading) }
                    .catch { emit(ProfileUiState.Error) }
            }

        val uiState: StateFlow<ProfileUiState> =
            combine(content, photoEditor.sheet, signOut) { state, sheet, signOutState ->
                if (state is ProfileUiState.Success) {
                    state.copy(
                        photoSheet = sheet,
                        showSignOutDialog = signOutState.showDialog,
                        isSigningOut = signOutState.inProgress,
                    )
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState.Loading)

        private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
        val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

        fun onEvent(event: ProfileUiEvent) {
            when (event) {
                ProfileUiEvent.Retry -> retryKey.update { it + 1 }
                ProfileUiEvent.AvatarClick -> photoEditor.open()
                ProfileUiEvent.TakePhoto -> photoEditor.openCamera()
                is ProfileUiEvent.PhotoCapture -> onCaptureEvent(event.event)
                is ProfileUiEvent.PhotoPick -> photoEditor.select(event.photoUri)
                ProfileUiEvent.SavePhoto -> savePhoto()
                ProfileUiEvent.DismissPhotoSheet -> photoEditor.dismiss()
                ProfileUiEvent.SignOutClick -> signOut.update { it.copy(showDialog = true) }
                ProfileUiEvent.DismissSignOut -> signOut.update { it.copy(showDialog = false) }
                ProfileUiEvent.ConfirmSignOut -> confirmSignOut()
            }
        }

        private fun savePhoto() {
            viewModelScope.launch {
                val result = photoEditor.save() ?: return@launch
                val effect =
                    if (result.isSuccess) {
                        ProfileEffect.ShowMessage(R.string.account_notice_photo_saved)
                    } else {
                        ProfileEffect.ShowMessage(R.string.account_error_save, host = photoEditor.messageHost)
                    }
                _effects.send(effect)
            }
        }

        private fun onCaptureEvent(event: PhotoCaptureEvent) {
            viewModelScope.launch {
                photoEditor.onCaptureEvent(event)?.onFailure {
                    _effects.send(
                        ProfileEffect.ShowMessage(R.string.account_adjust_error, host = photoEditor.messageHost),
                    )
                }
            }
        }

        private fun confirmSignOut() {
            if (signOut.value.inProgress) return
            signOut.value = SignOutState(showDialog = false, inProgress = true)
            viewModelScope.launch {
                authRepository.signOut()
                _effects.send(ProfileEffect.SignedOut)
            }
        }

        private fun daysUntil(profile: UserProfile): Long =
            ChronoUnit.DAYS.between(LocalDate.now(), profile.subscription.activeUntil)
    }
