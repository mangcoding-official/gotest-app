package com.gotest.app.feature.home.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.home.domain.SearchRepository
import com.gotest.app.feature.home.domain.SearchResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Wait for a pause in typing before searching. */
private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val searchRepository: SearchRepository,
    ) : ViewModel() {
        /**
         * The text field's value. Compose state rather than a flow so typing updates the field
         * synchronously; the search pipeline reads it through [snapshotFlow].
         */
        var query: String by mutableStateOf("")
            private set

        private val filter = MutableStateFlow(SearchFilter.All)
        private val retryCount = MutableStateFlow(0)

        private val results: Flow<ResultsState> =
            combine(
                snapshotFlow { query }
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .map(String::trim)
                    .distinctUntilChanged(),
                retryCount,
            ) { text, _ -> text }
                .flatMapLatest { text ->
                    if (text.isEmpty()) {
                        flowOf(ResultsState.Idle)
                    } else {
                        searchRepository
                            .search(text)
                            .map<SearchResults, ResultsState> { ResultsState.Loaded(text, it) }
                            .onStart { emit(ResultsState.Loading) }
                            .catch { emit(ResultsState.Failed) }
                    }
                }

        val uiState: StateFlow<SearchUiState> =
            combine(results, filter, ::toUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState.Idle)

        fun onEvent(event: SearchUiEvent) {
            when (event) {
                is SearchUiEvent.QueryChange -> query = event.query
                SearchUiEvent.ClearQuery -> query = ""
                is SearchUiEvent.SelectFilter -> filter.value = event.filter
                SearchUiEvent.Retry -> retryCount.update { it + 1 }
            }
        }

        private fun toUiState(
            results: ResultsState,
            filter: SearchFilter,
        ): SearchUiState =
            when (results) {
                ResultsState.Idle -> SearchUiState.Idle
                ResultsState.Loading -> SearchUiState.Loading
                ResultsState.Failed -> SearchUiState.Error
                is ResultsState.Loaded -> results.toUiState(filter)
            }

        private fun ResultsState.Loaded.toUiState(filter: SearchFilter): SearchUiState {
            fun shows(section: SearchFilter) = filter == SearchFilter.All || filter == section
            val packages = if (shows(SearchFilter.Packages)) results.packages else emptyList()
            val success =
                SearchUiState.Success(
                    query = query,
                    filter = filter,
                    tryouts = packages.filter { it.type == PackageType.Tryout },
                    practices = packages.filter { it.type == PackageType.Practice },
                    topics = if (shows(SearchFilter.Topics)) results.topics else emptyList(),
                    attempts = if (shows(SearchFilter.Results)) results.attempts else emptyList(),
                    articles = if (shows(SearchFilter.Articles)) results.articles else emptyList(),
                )
            return if (success.isEmpty) SearchUiState.Empty(query, filter) else success
        }

        private sealed interface ResultsState {
            data object Idle : ResultsState

            data object Loading : ResultsState

            data object Failed : ResultsState

            data class Loaded(
                val query: String,
                val results: SearchResults,
            ) : ResultsState
        }
    }
