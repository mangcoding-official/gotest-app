package com.gotest.app.feature.account.ui.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data object Error : ProfileUiState

    @Immutable
    data class Success(
        val profile: UserProfile,
        /** Days until the plan ends ("30 hari tersisa"); negative once it has ended. */
        val daysRemaining: Long,
        /** Null while the "Foto Profil" sheet is closed. */
        val photoSheet: PhotoSheetUiState? = null,
        val showSignOutDialog: Boolean = false,
        val isSigningOut: Boolean = false,
    ) : ProfileUiState
}

sealed interface ProfileUiEvent {
    data object Retry : ProfileUiEvent

    data object AvatarClick : ProfileUiEvent

    data class PhotoPick(
        val photoUri: String,
    ) : ProfileUiEvent

    data object SavePhoto : ProfileUiEvent

    data object DismissPhotoSheet : ProfileUiEvent

    data object SignOutClick : ProfileUiEvent

    data object DismissSignOut : ProfileUiEvent

    data object ConfirmSignOut : ProfileUiEvent
}

sealed interface ProfileEffect {
    data object SignedOut : ProfileEffect

    /** [inPhotoSheet] routes the snackbar into the open sheet, where the screen's one would be hidden. */
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val inPhotoSheet: Boolean = false,
    ) : ProfileEffect
}

/** Results Edit Informasi Akun and Ganti Password hand back to Profil, shown as a snackbar. */
enum class ProfileNotice(
    @param:StringRes val messageRes: Int,
) {
    ProfileSaved(R.string.account_notice_profile_saved),
    PasswordChanged(R.string.account_notice_password_changed),
}
