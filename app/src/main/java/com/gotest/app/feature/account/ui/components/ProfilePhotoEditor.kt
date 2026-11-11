package com.gotest.app.feature.account.ui.components

import androidx.compose.runtime.Immutable
import com.gotest.app.core.domain.repository.UserRepository
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
) {
    val canSave: Boolean get() = selectedPhotoUri != null && !isSaving
}

/**
 * "Foto Profil" sheet logic shared by Profil and Edit Informasi Akun. Unscoped, so each
 * ViewModel gets its own instance.
 */
class ProfilePhotoEditor
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) {
        private val _sheet = MutableStateFlow<PhotoSheetUiState?>(null)

        /** Null while the sheet is closed. */
        val sheet: StateFlow<PhotoSheetUiState?> = _sheet.asStateFlow()

        fun open() {
            _sheet.value = PhotoSheetUiState()
        }

        /** True while the sheet is on screen, so messages can be shown inside it. */
        val isOpen: Boolean get() = _sheet.value != null

        /** Always closes (scrim, back, "Tutup"); a save already running still completes. */
        fun dismiss() {
            _sheet.value = null
        }

        fun select(photoUri: String) {
            _sheet.update { it?.copy(selectedPhotoUri = photoUri) }
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
