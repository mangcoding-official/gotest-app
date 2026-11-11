package com.gotest.app.feature.studypackage.ui.savedpackages

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.studypackage.domain.SavedPackage
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetState

/** Paket Tersimpan. The filter pills stay visible while [content] reloads. */
@Immutable
data class SavedPackagesUiState(
    /** Null is "Semua". */
    val selectedType: PackageType? = null,
    /** Every saved package whatever the filter, for "Semua (3)"; null until loaded. */
    val totalCount: Int? = null,
    val content: SavedPackagesContent = SavedPackagesContent.Loading,
    /** "Detail Latihan" sheet; null when closed. */
    val sheet: PracticeSheetState? = null,
)

sealed interface SavedPackagesContent {
    data object Loading : SavedPackagesContent

    /** Nothing saved, or nothing of the selected type. */
    data object Empty : SavedPackagesContent

    data object Error : SavedPackagesContent

    @Immutable
    data class Success(
        val packages: List<SavedPackage>,
    ) : SavedPackagesContent
}

sealed interface SavedPackagesUiEvent {
    /** Null selects "Semua". */
    data class TypeSelected(
        val type: PackageType?,
    ) : SavedPackagesUiEvent

    data object Retry : SavedPackagesUiEvent

    /** The filled bookmark on a card. */
    data class UnsaveClicked(
        val packageId: String,
    ) : SavedPackagesUiEvent

    /** "Mulai Latihan" opens the sheet and "Mulai Tryout" the detail page, as on Paket Belajar. */
    data class StartClicked(
        val pkg: StudyPackage,
    ) : SavedPackagesUiEvent

    data object SheetDismissed : SavedPackagesUiEvent

    data object SheetRetry : SavedPackagesUiEvent

    data class SheetStartClicked(
        val packageId: String,
    ) : SavedPackagesUiEvent
}

sealed interface SavedPackagesEffect {
    data class OpenTryout(
        val packageId: String,
    ) : SavedPackagesEffect

    data class StartExam(
        val packageId: String,
    ) : SavedPackagesEffect

    /** A card's bookmark removed its package. */
    data object PackageRemoved : SavedPackagesEffect
}
