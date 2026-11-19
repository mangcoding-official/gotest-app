package com.gotest.app.feature.leaderboard.ui.leaderboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.leaderboard.domain.LeaderboardEntry
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardPage
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardRules
import com.gotest.app.feature.leaderboard.domain.LeaderboardScope
import com.gotest.app.feature.leaderboard.domain.NamedOption
import com.gotest.app.feature.leaderboard.ui.components.BoardRow
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
data class LeaderboardUiState(
    /** The tab the student chose; [page] may still be the previous tab's while it loads. */
    val scope: LeaderboardScope = LeaderboardScope.Global,
    val page: LeaderboardPage? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** The Global filters as chosen, sent with "Filter" (web GlobalBoard drafts). */
    val draft: LeaderboardFilters = LeaderboardFilters(),
) {
    /** The page of the chosen tab, or null while it loads. */
    val shown: LeaderboardPage? get() = page?.takeIf { it.filters.scope == scope }

    val subjectChoices: List<NamedOption>
        get() =
            page?.let { LeaderboardRules.subjectChoices(draft.category, it.options.subjects, it.taxonomyHierarchy) }
                ?: emptyList()
}

/** "Filter", "Muat Lebih Banyak" or a tab failed while a board was on screen: it stays as it was. */
data object LeaderboardRefreshFailed

/**
 * Leaderboard (web Leaderboard/Index). Every change asks the server again with the whole filter set, as
 * the web does: a tab keeps the Global filters (LeaderboardTabs sends `...filters`), and "Muat Lebih
 * Banyak" asks for 20 more rows.
 */
@HiltViewModel
class LeaderboardViewModel
    @Inject
    constructor(
        private val repository: LeaderboardPagesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LeaderboardUiState())
        val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

        private val _effects = Channel<LeaderboardRefreshFailed>(Channel.BUFFERED)
        val effects: Flow<LeaderboardRefreshFailed> = _effects.receiveAsFlow()

        private var request: Job? = null

        init {
            load(LeaderboardFilters())
        }

        fun retry() {
            val state = _uiState.value
            load((state.page?.filters ?: LeaderboardFilters()).copy(scope = state.scope))
        }

        fun selectTab(scope: LeaderboardScope) {
            val state = _uiState.value
            if (scope == state.scope) return
            _uiState.update { it.copy(scope = scope) }
            load((state.page?.filters ?: LeaderboardFilters()).copy(scope = scope))
        }

        /** Web: a subject the new category does not have goes back to "Semua Mata pelajaran". */
        fun onDraftChange(draft: LeaderboardFilters) {
            _uiState.update { state ->
                val page = state.page ?: return@update state.copy(draft = draft)
                val choices =
                    LeaderboardRules.subjectChoices(
                        draft.category,
                        page.options.subjects,
                        page.taxonomyHierarchy,
                    )
                state.copy(draft = draft.copy(subject = LeaderboardRules.keepSubject(draft.subject, choices)))
            }
        }

        fun applyFilters() {
            val state = _uiState.value
            val applied = state.page?.filters ?: LeaderboardFilters()
            load(
                applied.copy(
                    category = state.draft.category,
                    subject = state.draft.subject,
                    period = state.draft.period,
                ),
            )
        }

        fun loadMore() {
            val applied = _uiState.value.page?.filters ?: return
            load(applied.copy(limit = applied.limit + LeaderboardFilters.STEP))
        }

        private fun load(filters: LeaderboardFilters) {
            request?.cancel()
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            request =
                viewModelScope.launch {
                    repository
                        .leaderboard(filters)
                        .onSuccess { page ->
                            _uiState.update {
                                it.copy(
                                    page = page,
                                    scope = page.filters.scope,
                                    isLoading = false,
                                    draft = page.filters,
                                )
                            }
                        }.onFailure {
                            val onScreen = _uiState.value.shown != null
                            _uiState.update { it.copy(isLoading = false, loadFailed = !onScreen) }
                            if (onScreen) _effects.send(LeaderboardRefreshFailed)
                        }
                }
        }
    }

internal fun LeaderboardEntry.toRow(): BoardRow =
    BoardRow(rank = rank, userId = userId, name = name, points = points, change = change, isCurrentUser = isCurrentUser)
