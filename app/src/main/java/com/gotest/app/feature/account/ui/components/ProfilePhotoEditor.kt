package com.gotest.app.feature.account.ui.components

import androidx.compose.runtime.Immutable
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.account.domain.PhotoCrop
import com.gotest.app.feature.account.domain.ProfilePhotoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** State of the open "Foto Profil" sheet. */
@Immutable
data class PhotoSheetUiState(
    /** Photo picked in this session and not saved yet; the preview shows it instead of the current one. */
    val selectedPhotoUri: String? = null,
    val isSaving: Boolean = false,
    /** The full-screen "Ambil Foto" flow on top of the sheet; null while it is closed. */
    val capture: PhotoCaptureUiState? = null,
) {
    val canSave: Boolean get() = selectedPhotoUri != null && !isSaving
}

/** "Ambil Foto" (frame 22963), then "Sesuaikan Foto" (frame 22994) once [capturedPhotoUri] is set. */
@Immutable
data class PhotoCaptureUiState(
    val useFrontCamera: Boolean = true,
    val flashOn: Boolean = false,
    /** The shot waiting in "Sesuaikan Foto"; null while the camera shows. */
    val capturedPhotoUri: String? = null,
    /** "Gunakan Foto" is cropping the shot. */
    val isApplying: Boolean = false,
)

sealed interface PhotoCaptureEvent {
    data object SwitchCamera : PhotoCaptureEvent

    data object ToggleFlash : PhotoCaptureEvent

    data class Captured(
        val photoUri: String,
    ) : PhotoCaptureEvent

    /** "Ambil Ulang", the back arrow or system back in "Sesuaikan Foto". */
    data object Retake : PhotoCaptureEvent

    /** "Gunakan Foto". */
    data class Use(
        val crop: PhotoCrop,
    ) : PhotoCaptureEvent

    /** The camera's close button or system back. */
    data object Close : PhotoCaptureEvent
}

/** Where a message has to show to be seen: in the camera flow, in the sheet, or on the screen. */
enum class PhotoMessageHost { Screen, Sheet, Capture }

/**
 * "Foto Profil" sheet logic shared by Profil and Edit Informasi Akun, including the "Ambil
 * Foto" flow opened from it. Unscoped, so each ViewModel gets its own instance.
 */
class ProfilePhotoEditor
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val photoProcessor: ProfilePhotoProcessor,
    ) {
        private val _sheet = MutableStateFlow<PhotoSheetUiState?>(null)

        /** Null while the sheet is closed. */
        val sheet: StateFlow<PhotoSheetUiState?> = _sheet.asStateFlow()

        fun open() {
            _sheet.value = PhotoSheetUiState()
        }

        /** The innermost open layer, so a message shows where the student is looking. */
        val messageHost: PhotoMessageHost
            get() {
                val sheet = _sheet.value
                return when {
                    sheet == null -> PhotoMessageHost.Screen
                    sheet.capture == null -> PhotoMessageHost.Sheet
                    else -> PhotoMessageHost.Capture
                }
            }

        /** Always closes (scrim, back, "Tutup"); a save already running still completes. */
        fun dismiss() {
            _sheet.value
                ?.capture
                ?.capturedPhotoUri
                ?.let(photoProcessor::discard)
            _sheet.value = null
        }

        /** A gallery pick, from the sheet or from the camera's gallery button (which it closes). */
        fun select(photoUri: String) {
            _sheet.value
                ?.capture
                ?.capturedPhotoUri
                ?.let(photoProcessor::discard)
            _sheet.update { it?.copy(selectedPhotoUri = photoUri, capture = null) }
        }

        /** "Ambil Foto", once the camera permission is granted. */
        fun openCamera() {
            _sheet.update { it?.copy(capture = PhotoCaptureUiState()) }
        }

        /**
         * Applies [event] to the camera flow. Only [PhotoCaptureEvent.Use] returns a result: on
         * success the cropped photo becomes the sheet's pick (like a gallery pick) and the flow
         * closes; on failure it stays on "Sesuaikan Foto".
         */
        suspend fun onCaptureEvent(event: PhotoCaptureEvent): Result<Unit>? {
            val capture = _sheet.value?.capture
            return when {
                capture == null -> {
                    null
                }

                event is PhotoCaptureEvent.Use -> {
                    use(capture, event.crop)
                }

                else -> {
                    applyToCapture(capture, event)
                    null
                }
            }
        }

        private fun applyToCapture(
            capture: PhotoCaptureUiState,
            event: PhotoCaptureEvent,
        ) {
            when (event) {
                PhotoCaptureEvent.SwitchCamera -> {
                    updateCapture { it.copy(useFrontCamera = !it.useFrontCamera) }
                }

                PhotoCaptureEvent.ToggleFlash -> {
                    updateCapture { it.copy(flashOn = !it.flashOn) }
                }

                is PhotoCaptureEvent.Captured -> {
                    updateCapture { it.copy(capturedPhotoUri = event.photoUri) }
                }

                PhotoCaptureEvent.Retake -> {
                    capture.capturedPhotoUri?.let(photoProcessor::discard)
                    updateCapture { it.copy(capturedPhotoUri = null, isApplying = false) }
                }

                PhotoCaptureEvent.Close -> {
                    capture.capturedPhotoUri?.let(photoProcessor::discard)
                    _sheet.update { it?.copy(capture = null) }
                }

                is PhotoCaptureEvent.Use -> {
                    // Cropping suspends; onCaptureEvent sends it to use().
                    Unit
                }
            }
        }

        private suspend fun use(
            capture: PhotoCaptureUiState,
            crop: PhotoCrop,
        ): Result<Unit>? {
            val photoUri = capture.capturedPhotoUri
            if (photoUri == null || capture.isApplying) return null
            updateCapture { it.copy(isApplying = true) }
            val result = photoProcessor.crop(photoUri, crop)
            result
                .onSuccess { cropped -> _sheet.update { it?.copy(selectedPhotoUri = cropped, capture = null) } }
                .onFailure { updateCapture { it.copy(isApplying = false) } }
            return result.map { }
        }

        private fun updateCapture(transform: (PhotoCaptureUiState) -> PhotoCaptureUiState) {
            _sheet.update { sheet -> sheet?.capture?.let { sheet.copy(capture = transform(it)) } ?: sheet }
        }

        /**
         * Saves the picked photo and closes the sheet on success; on failure the sheet stays open
         * (if the student has not closed it meanwhile). Null when there was nothing to save.
         */
        suspend fun save(): Result<Unit>? {
            val current = _sheet.value
            val photoUri = current?.selectedPhotoUri
            if (photoUri == null || current.isSaving) return null
            _sheet.value = current.copy(isSaving = true)
            val result = userRepository.updateAvatar(photoUri)
            _sheet.update { open -> if (open == null || result.isSuccess) null else open.copy(isSaving = false) }
            return result
        }
    }
