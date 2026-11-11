package com.gotest.app.feature.account.ui.editprofile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.PhotoMessageHost
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState

sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState

    data object Error : EditProfileUiState

    @Immutable
    data class Success(
        val avatarUrl: String?,
        val name: String,
        val email: String,
        @param:StringRes val nameError: Int? = null,
        @param:StringRes val emailError: Int? = null,
        val isSaving: Boolean = false,
        /** Null while the "Foto Profil" sheet is closed. */
        val photoSheet: PhotoSheetUiState? = null,
    ) : EditProfileUiState
}

sealed interface EditProfileUiEvent {
    data object Retry : EditProfileUiEvent

    data class NameChange(
        val value: String,
    ) : EditProfileUiEvent

    data class EmailChange(
        val value: String,
    ) : EditProfileUiEvent

    data object Submit : EditProfileUiEvent

    data object ChangePhotoClick : EditProfileUiEvent

    /** "Ambil Foto" with the camera permission granted. */
    data object TakePhoto : EditProfileUiEvent

    data class PhotoCapture(
        val event: PhotoCaptureEvent,
    ) : EditProfileUiEvent

    data class PhotoPick(
        val photoUri: String,
    ) : EditProfileUiEvent

    data object SavePhoto : EditProfileUiEvent

    data object DismissPhotoSheet : EditProfileUiEvent
}

sealed interface EditProfileEffect {
    data object Saved : EditProfileEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val host: PhotoMessageHost = PhotoMessageHost.Screen,
    ) : EditProfileEffect
}
