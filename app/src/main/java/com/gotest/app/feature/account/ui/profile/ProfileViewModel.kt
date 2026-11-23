package com.gotest.app.feature.account.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.feature.account.domain.ProfilePage
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.ProfilePhotoEditor
import com.gotest.app.feature.account.ui.components.fieldMessage
import com.gotest.app.feature.ruang.domain.RuangRepository
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
import kotlinx.coroutines.flow.flow
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
        private val profilePages: ProfilePagesRepository,
        private val ruang: RuangRepository,
        accountChanges: AccountChanges,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val signOut = MutableStateFlow(SignOutState())

        private val content: Flow<ProfileUiState> =
            // "Coba lagi", or a saved write elsewhere (Ubah Profil, foto, nomor, email, password): read /profil again.
            combine(retryKey, accountChanges.changes) { retry, changed -> retry to changed }.flatMapLatest {
                // GET /profil: the number, whether numbers can be added, and the stats; the rest comes from /akun.
                // Without /profil the page still shows, with Keluar (A2e review): those parts stay as /akun has them.
                val page = flow { emit(profilePages.page().getOrNull()) }
                // The Mitra portal is not in the app yet (A4), so only student spaces are listed.
                val spaces = flow { emit(ruang.memberships().getOrDefault(emptyList()).filterNot { it.opensPortal }) }
                combine(userRepository.observeProfile(), page, spaces) { profile, profilePage, studentSpaces ->
                    ProfileUiState.Success(
                        profile = profilePage?.let(profile::withPage) ?: profile,
                        phoneSignIn = profilePage?.phoneSignIn ?: false,
                        spaces = studentSpaces,
                        daysRemaining = daysUntil(profile),
                    )
                }.map<ProfileUiState.Success, ProfileUiState> { it }
                    .onStart { emit(ProfileUiState.Loading) }
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
                ProfileUiEvent.DeletePhoto -> deletePhoto()
                ProfileUiEvent.DismissPhotoSheet -> photoEditor.dismiss()
                ProfileUiEvent.SignOutClick -> signOut.update { it.copy(showDialog = true) }
                ProfileUiEvent.DismissSignOut -> signOut.update { it.copy(showDialog = false) }
                ProfileUiEvent.ConfirmSignOut -> confirmSignOut()
            }
        }

        private fun savePhoto() {
            viewModelScope.launch {
                // A saved photo shows itself; only a refusal is told (A2e-9).
                photoEditor.save()?.onFailure {
                    _effects.send(ProfileEffect.ShowText(it.fieldMessage("photo"), host = photoEditor.messageHost))
                }
            }
        }

        private fun deletePhoto() {
            viewModelScope.launch {
                photoEditor.delete()?.onFailure {
                    _effects.send(ProfileEffect.ShowText(it.fieldMessage("photo"), host = photoEditor.messageHost))
                }
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

/** Profil's own numbers (web StudentStatCards from /profil); a change the server leaves out shows as none. */
private fun UserProfile.withPage(page: ProfilePage): UserProfile {
    val stats = page.stats ?: return copy(phone = page.phone)
    return copy(
        phone = page.phone,
        stats =
            UserStats(
                questionsAnswered = stats.questionsDone,
                accuracyPercent = stats.accuracy,
                packagesCompleted = stats.packagesDone,
                questionsTrendPercent = stats.questionsDoneChange ?: 0,
                accuracyTrendPercent = stats.accuracyChange ?: 0,
                packagesTrendPercent = stats.packagesDoneChange ?: 0,
            ),
    )
}
