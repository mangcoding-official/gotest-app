package com.gotest.app.feature.home.ui.home

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.UserStats

/** Greeting row: avatar, first name and the bell's unread dot. */
@Immutable
data class HomeProfileSummary(
    val firstName: String,
    /** Coil model for the avatar. */
    val avatarUrl: String?,
    val hasUnreadNotifications: Boolean,
)

/** "Paket Untukmu": both tabs are loaded up front so switching is instant. */
@Immutable
data class HomeRecommendations(
    val selectedType: PackageType,
    val practice: List<StudyPackage>,
    val tryout: List<StudyPackage>,
) {
    val visible: List<StudyPackage>
        get() = if (selectedType == PackageType.Practice) practice else tryout
}

/** "Aktivitas Terkini": the latest finished attempts of the selected type. */
@Immutable
data class HomeActivity(
    val selectedType: PackageType,
    val attempts: List<AttemptSummary>,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Error : HomeUiState

    /** New student: nothing in progress and no finished attempt ("Beranda / Empty State"). */
    @Immutable
    data class Empty(
        val profile: HomeProfileSummary,
        val recommendations: HomeRecommendations,
        val activityType: PackageType,
    ) : HomeUiState

    @Immutable
    data class Success(
        val profile: HomeProfileSummary,
        val stats: UserStats,
        /** Package behind the "Lanjutkan" card; null hides the card. */
        val inProgress: StudyPackage?,
        val recommendations: HomeRecommendations,
        val activity: HomeActivity,
    ) : HomeUiState
}

sealed interface HomeUiEvent {
    data class SelectRecommendedType(
        val type: PackageType,
    ) : HomeUiEvent

    data class SelectActivityType(
        val type: PackageType,
    ) : HomeUiEvent

    data object Retry : HomeUiEvent
}
