package com.gotest.app.feature.home.ui.search

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.home.domain.HelpArticle
import com.gotest.app.feature.home.domain.SearchAttempt

/** Filter pills under the query: Semua / Paket / Topik / Hasil / Artikel. */
enum class SearchFilter { All, Packages, Topics, Results, Articles }

sealed interface SearchUiState {
    /** Nothing typed yet. */
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data object Error : SearchUiState

    /** No match for [query] under [filter]; the pills stay so the student can widen the filter. */
    data class Empty(
        val query: String,
        val filter: SearchFilter,
    ) : SearchUiState

    /** Sections already narrowed to [filter]; empty sections are hidden. */
    @Immutable
    data class Success(
        val query: String,
        val filter: SearchFilter,
        val tryouts: List<StudyPackage>,
        val practices: List<StudyPackage>,
        val topics: List<String>,
        val attempts: List<SearchAttempt>,
        val articles: List<HelpArticle>,
    ) : SearchUiState {
        val isEmpty: Boolean
            get() =
                tryouts.isEmpty() && practices.isEmpty() && topics.isEmpty() &&
                    attempts.isEmpty() && articles.isEmpty()
    }
}

sealed interface SearchUiEvent {
    data class QueryChange(
        val query: String,
    ) : SearchUiEvent

    data object ClearQuery : SearchUiEvent

    data class SelectFilter(
        val filter: SearchFilter,
    ) : SearchUiEvent

    data object Retry : SearchUiEvent
}
