package com.gotest.app.feature.history.ui.history

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.history.data.HistoryCsvStore
import com.gotest.app.feature.history.domain.HistoryCategory
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.domain.HistoryStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HistoryUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val stats: HistoryStats = HistoryStats(),
    val categories: List<HistoryCategory> = emptyList(),
    /**
     * The filters of the list on screen, changed only when a new list arrives; [draft] is what the
     * filters say until "Filter" is pressed, as on the web.
     */
    val applied: HistoryFilters = HistoryFilters(),
    val draft: HistoryFilters = HistoryFilters(),
    val results: List<HistoryResult> = emptyList(),
    val total: Int = 0,
    val nextPage: Int? = null,
    val isFiltering: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isExporting: Boolean = false,
)

sealed interface HistoryEffect {
    data class Exported(
        val uri: Uri,
    ) : HistoryEffect

    data object ExportFailed : HistoryEffect

    /** A new filter or showing more failed: the list and its filters stay as they were (A2e review ulang 1). */
    data object ListFailed : HistoryEffect
}

/** Riwayat (web: History/Index): stats, filters applied with "Filter", 20 results a page, CSV export. */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val repository: HistoryPagesRepository,
        private val csv: HistoryCsvStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HistoryUiState())
        val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

        private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
        val effects: Flow<HistoryEffect> = _effects.receiveAsFlow()

        /**
         * One list request at a time, as on Leaderboard: a new filter cancels the one before (and a "more"
         * page of the old filter), so an older answer never lands on the newer list (A2e review 3).
         */
        private var listJob: Job? = null

        init {
            load()
        }

        /** "Coba lagi": the filters of the list on screen again. */
        fun load() = request(_uiState.value.applied)

        fun onDraftChange(draft: HistoryFilters) = _uiState.update { it.copy(draft = draft) }

        /** "Filter" or Enter in the search: the list starts again from the first page. */
        fun applyFilters() = request(_uiState.value.draft)

        private fun request(filters: HistoryFilters) {
            listJob?.cancel()
            // With a list on screen, a failure keeps it and says so; without one, the page shows the error.
            val onScreen = _uiState.value.let { !it.isLoading && !it.loadFailed }
            _uiState.update {
                it.copy(isLoading = !onScreen, isFiltering = onScreen, loadFailed = false, isLoadingMore = false)
            }
            listJob =
                viewModelScope.launch {
                    repository
                        .history(filters, page = 1)
                        .onSuccess { page ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isFiltering = false,
                                    stats = page.stats,
                                    categories = page.categories,
                                    applied = page.filters,
                                    draft = page.filters,
                                    results = page.attempts,
                                    total = page.pagination.total,
                                    nextPage = page.pagination.nextPage,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, isFiltering = false, loadFailed = !onScreen) }
                            if (onScreen) _effects.send(HistoryEffect.ListFailed)
                        }
                }
        }

        /** "Tampilkan lebih banyak": the next 20, added without duplicates (the web matches on id). */
        fun loadMore() {
            val state = _uiState.value
            val next = state.nextPage ?: return
            if (state.isLoadingMore || state.isFiltering || listJob?.isActive == true) return
            _uiState.update { it.copy(isLoadingMore = true) }
            listJob =
                viewModelScope.launch {
                    repository
                        .history(state.applied, next)
                        .onSuccess { page ->
                            _uiState.update {
                                val known = it.results.map(HistoryResult::id).toSet()
                                it.copy(
                                    isLoadingMore = false,
                                    results = it.results + page.attempts.filterNot { result -> result.id in known },
                                    total = page.pagination.total,
                                    nextPage = page.pagination.nextPage,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoadingMore = false) }
                            _effects.send(HistoryEffect.ListFailed)
                        }
                }
        }

        /** "Export data": the whole history as CSV, whatever the filters, saved to Downloads. */
        fun export() {
            if (_uiState.value.isExporting) return
            _uiState.update { it.copy(isExporting = true) }
            viewModelScope.launch {
                // fold, not mapCatching: a cancelled save must not turn into "belum bisa diunduh".
                val saved = repository.export().fold({ stream -> csv.save(stream) }, { Result.failure(it) })
                _uiState.update { it.copy(isExporting = false) }
                _effects.send(saved.fold({ HistoryEffect.Exported(it) }, { HistoryEffect.ExportFailed }))
            }
        }
    }
