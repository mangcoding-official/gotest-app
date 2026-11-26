package com.gotest.app.feature.home.ui.search

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.home.domain.SearchGroup

/** Cari (web SearchPanel): nothing typed yet, or the groups for what was typed. */
sealed interface SearchUiState {
    data object Idle : SearchUiState

    /**
     * [isLoading] while the server is asked ("Mencari…"); a failed search shows no groups, as the web shows
     * "Tidak ada hasil yang cocok." then.
     */
    @Immutable
    data class Results(
        val query: String,
        val groups: List<SearchGroup> = emptyList(),
        val isLoading: Boolean = false,
    ) : SearchUiState
}

sealed interface SearchUiEvent {
    data class QueryChange(
        val query: String,
    ) : SearchUiEvent

    data object ClearQuery : SearchUiEvent
}
