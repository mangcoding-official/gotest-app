package com.gotest.app.feature.studypackage.ui.subjectlist

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.Subject

sealed interface SubjectListUiState {
    data object Loading : SubjectListUiState

    data object Error : SubjectListUiState

    /** The category has no subjects yet; the header and jenjang switch still show. */
    @Immutable
    data class Empty(
        val header: SubjectListHeader,
    ) : SubjectListUiState

    @Immutable
    data class Success(
        val header: SubjectListHeader,
        val subjects: List<Subject>,
    ) : SubjectListUiState
}

/** App bar title plus the jenjang dropdown, which switches between categories. */
@Immutable
data class SubjectListHeader(
    val category: StudyCategory,
    val categories: List<StudyCategory>,
)

sealed interface SubjectListUiEvent {
    data class CategorySelected(
        val categoryId: String,
    ) : SubjectListUiEvent

    data object Retry : SubjectListUiEvent
}
