package com.gotest.app.feature.account.ui.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.PhotoMessageHost
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState
import com.gotest.app.feature.ruang.domain.MitraMembership

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data object Error : ProfileUiState

    @Immutable
    data class Success(
        val profile: UserProfile,
        /** Whether the server offers WhatsApp codes: "Tambah Nomor WhatsApp" needs it (the web's `phoneSignIn`). */
        val phoneSignIn: Boolean = false,
        /** Mitra spaces of the account where it is a student: "Ruang {nama}" rows (T-026 A2b). */
        val spaces: List<MitraMembership> = emptyList(),
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

    /** "Ambil Foto" with the camera permission granted. */
    data object TakePhoto : ProfileUiEvent

    data class PhotoCapture(
        val event: PhotoCaptureEvent,
    ) : ProfileUiEvent

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

    /** [host] routes the snackbar into the open sheet or camera, where the screen's one would be hidden. */
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val host: PhotoMessageHost = PhotoMessageHost.Screen,
    ) : ProfileEffect
}
