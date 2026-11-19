package com.gotest.app.feature.account.ui.editprofile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.account.ui.components.PhotoCaptureEvent
import com.gotest.app.feature.account.ui.components.PhotoMessageHost
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState

/** "Cek email kamu": the new email waits for the code the server sent to it. */
@Immutable
data class EmailCodeStep(
    /** The email as typed, sent again with the code. */
    val email: String,
    /** Where the code went, masked by the server ("r***@contoh.id"). */
    val sentTo: String,
    val code: String = "",
    val codeError: String? = null,
    /** Seconds until "Kirim ulang kode"; the server allows one code a minute. */
    val resendIn: Int = RESEND_SECONDS,
    val isSaving: Boolean = false,
) {
    companion object {
        const val RESEND_SECONDS = 60
        const val CODE_LENGTH = 6
    }
}

sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState

    data object Error : EditProfileUiState

    /** Web EditAccountModal: name, email and level, then the email code when a new email needs one. */
    @Immutable
    data class Success(
        val avatarUrl: String?,
        val name: String,
        val email: String,
        val educationLevel: EducationLevel?,
        /** A new email must be proven with a code (`emailCode` of GET /profil). */
        val emailCode: Boolean = false,
        /** Server messages by field: name, email, education_level. */
        val errors: Map<String, String> = emptyMap(),
        val isSaving: Boolean = false,
        val codeStep: EmailCodeStep? = null,
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

    data class LevelChange(
        val value: EducationLevel,
    ) : EditProfileUiEvent

    data object Submit : EditProfileUiEvent

    data class CodeChange(
        val value: String,
    ) : EditProfileUiEvent

    data object SaveEmail : EditProfileUiEvent

    data object ResendCode : EditProfileUiEvent

    /** "Batal": back to the fields. */
    data object CancelCode : EditProfileUiEvent

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

    data object DeletePhoto : EditProfileUiEvent

    data object DismissPhotoSheet : EditProfileUiEvent
}

sealed interface EditProfileEffect {
    /** Closes the screen; [message] is the server's (a new email: "Email baru tersimpan."), else none (A2e-9). */
    data class Saved(
        val message: String? = null,
    ) : EditProfileEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
        val host: PhotoMessageHost = PhotoMessageHost.Screen,
    ) : EditProfileEffect

    /** A server message, or null when nothing came back (the screen says "try again"). */
    data class ShowText(
        val message: String?,
        val host: PhotoMessageHost = PhotoMessageHost.Screen,
    ) : EditProfileEffect
}
