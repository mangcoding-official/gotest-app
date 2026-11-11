package com.gotest.app.feature.studypackage.ui.studypackage

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.StudyCategory
import kotlin.time.Duration.Companion.milliseconds

/**
 * Paket tab. The segmented control and the jenjang dropdown stay on screen while [content]
 * loads, so they live outside the sealed content state.
 */
@Immutable
data class StudyPackageUiState(
    val selectedTab: PackageType = PackageType.Practice,
    /** Categories double as the jenjang options ("Perguruan Tinggi", "SLTA / Sederajat", …). */
    val jenjangOptions: List<StudyCategory> = emptyList(),
    /** Category the dropdown filters to; null is "Semua Jenjang". */
    val selectedJenjangId: String? = null,
    val content: StudyPackageContent = StudyPackageContent.Loading,
    /** Time left on the Seasonal Tryout; ticks every second while the Tryout tab is shown. */
    val countdown: Countdown? = null,
)

sealed interface StudyPackageContent {
    data object Loading : StudyPackageContent

    data object Empty : StudyPackageContent

    data object Error : StudyPackageContent

    @Immutable
    data class Success(
        val categories: List<StudyCategory>,
        /** "Aktivitas Terkini" on the Latihan tab. */
        val recentActivities: List<AttemptSummary>,
        /** "Seasonal Tryout" block on the Tryout tab. */
        val seasonalTryout: SeasonalTryout?,
    ) : StudyPackageContent
}

/** "24H : 41M : 31S". */
@Immutable
data class Countdown(
    val hours: Long,
    val minutes: Int,
    val seconds: Int,
) {
    companion object {
        fun between(
            nowMillis: Long,
            endMillis: Long,
        ): Countdown =
            (endMillis - nowMillis).coerceAtLeast(0L).milliseconds.toComponents { hours, minutes, seconds, _ ->
                Countdown(hours, minutes, seconds)
            }
    }
}

sealed interface StudyPackageUiEvent {
    data class TabSelected(
        val tab: PackageType,
    ) : StudyPackageUiEvent

    data class JenjangSelected(
        val categoryId: String?,
    ) : StudyPackageUiEvent

    data object Retry : StudyPackageUiEvent
}
