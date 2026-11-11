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
        /** Filled bookmark ("Paket / Detail Tryout – Tersimpan"). */
        val isSaved: Boolean,
    ) : TryoutDetailUiState
}

sealed interface TryoutDetailUiEvent {
    data object Retry : TryoutDetailUiEvent

    data object BookmarkClicked : TryoutDetailUiEvent
}

sealed interface TryoutDetailEffect {
    /** The bookmark was toggled; [saved] tells which message to show. */
    data class BookmarkChanged(
        val saved: Boolean,
    ) : TryoutDetailEffect
}
