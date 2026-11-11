package com.gotest.app.feature.studypackage.ui.practicedetail

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.StudyPackageDetail

/** A package always has a detail, so there is no Empty state. */
sealed interface PracticeDetailUiState {
    data object Loading : PracticeDetailUiState

    data object Error : PracticeDetailUiState

    @Immutable
    data class Success(
        val detail: StudyPackageDetail,
    ) : PracticeDetailUiState
}

sealed interface PracticeDetailUiEvent {
    data object Retry : PracticeDetailUiEvent

    data object BookmarkClicked : PracticeDetailUiEvent
}

sealed interface PracticeDetailEffect {
    /** Bookmarks are not designed yet. */
    data object ShowComingSoon : PracticeDetailEffect
}
