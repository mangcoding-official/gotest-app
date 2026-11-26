package com.gotest.app.feature.home.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.home.domain.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** The web waits 250 ms after the last key before searching. */
private const val SEARCH_DEBOUNCE_MS = 250L

/** Cari against `GET /search` (T-026 A2c-1 = a), as the web's search panel. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) : ViewModel() {
        /**
         * The text field's value. Compose state rather than a flow so typing updates the field
         * synchronously; the search pipeline reads it through [snapshotFlow].
         */
        var query: String by mutableStateOf("")
            private set

        val uiState: StateFlow<SearchUiState> =
            snapshotFlow { query }
                .map(String::trim)
                .distinctUntilChanged()
                .transformLatest { text ->
                    if (text.isEmpty()) {
                        emit(SearchUiState.Idle)
                        return@transformLatest
                    }
                    emit(SearchUiState.Results(query = text, isLoading = true))
                    delay(SEARCH_DEBOUNCE_MS)
                    val groups =
                        repository
                            .search(text)
                            .getOrNull()
                            ?.groups
                            .orEmpty()
                    emit(SearchUiState.Results(query = text, groups = groups))
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState.Idle)

        fun onEvent(event: SearchUiEvent) {
            when (event) {
                is SearchUiEvent.QueryChange -> query = event.query
                SearchUiEvent.ClearQuery -> query = ""
            }
        }
    }
