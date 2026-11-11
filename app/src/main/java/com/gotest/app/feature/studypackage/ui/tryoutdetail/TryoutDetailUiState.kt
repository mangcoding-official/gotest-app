package com.gotest.app.feature.studypackage.ui.tryoutdetail

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.StudyPackageDetail

/** A package always has a detail, so there is no Empty state. */
sealed interface TryoutDetailUiState {
    data object Loading : TryoutDetailUiState

    data object Error : TryoutDetailUiState

    @Immutable
    data class Success(
        val detail: StudyPackageDetail,
    ) : TryoutDetailUiState
}

sealed interface TryoutDetailUiEvent {
    data object Retry : TryoutDetailUiEvent

    data object BookmarkClicked : TryoutDetailUiEvent
}

sealed interface TryoutDetailEffect {
    /** Bookmarks are not designed yet. */
    data object ShowComingSoon : TryoutDetailEffect
}
