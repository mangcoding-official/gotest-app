package com.gotest.app.feature.studypackage.ui.packagelist

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetState

/** Paket Belajar. Type switch and level pills stay visible while [content] reloads. */
@Immutable
data class PackageListUiState(
    val selectedType: PackageType = PackageType.Practice,
    /** Filter pills after "Semua" ("UTBK-SNBT", "SMA", …). */
    val levels: List<String> = emptyList(),
    /** Null is "Semua". */
    val selectedLevel: String? = null,
    val content: PackageListContent = PackageListContent.Loading,
    /** "Detail Latihan" sheet; null when closed. */
    val sheet: PracticeSheetState? = null,
)

sealed interface PackageListContent {
    data object Loading : PackageListContent

    /** No package matches the selected type and level. */
    data object Empty : PackageListContent

    data object Error : PackageListContent

    @Immutable
    data class Success(
        val packages: List<StudyPackage>,
    ) : PackageListContent
}

sealed interface PackageListUiEvent {
    data class TypeSelected(
        val type: PackageType,
    ) : PackageListUiEvent

    data class LevelSelected(
        val level: String?,
    ) : PackageListUiEvent

    data object Retry : PackageListUiEvent

    /** The card's button: opens the sheet for latihan, the detail page for tryout. */
    data class PackageActionClicked(
        val pkg: StudyPackage,
    ) : PackageListUiEvent

    data object SheetDismissed : PackageListUiEvent

    data object SheetRetry : PackageListUiEvent

    data class SheetStartClicked(
        val packageId: String,
    ) : PackageListUiEvent
}

sealed interface PackageListEffect {
    data class OpenTryout(
        val packageId: String,
    ) : PackageListEffect

    data class StartExam(
        val packageId: String,
    ) : PackageListEffect
}
