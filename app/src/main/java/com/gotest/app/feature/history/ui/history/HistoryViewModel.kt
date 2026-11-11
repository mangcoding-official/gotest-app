package com.gotest.app.feature.history.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

// Exam labels the "UTBK-SNBT" and "CPNS" pills match against (data values, not UI text).
private const val EXAM_UTBK = "UTBK-SNBT"
private const val EXAM_CPNS = "CPNS"

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val examRepository: ExamRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val refreshKey = MutableStateFlow(0)
        private val filter = MutableStateFlow(HistoryFilter.All)

        // Kept apart from uiState so the search field updates synchronously while typing.
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
        val effects: Flow<HistoryEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<HistoryUiState> =
            refreshKey
                .flatMapLatest {
                    combine(
                        examRepository.observeHistory(),
                        userRepository.observeProfile(),
                        _query,
                        filter,
                    ) { history, profile, query, filter ->
                        buildState(history, profile.stats.accuracyPercent, query, filter)
                    }.onStart { emit(HistoryUiState.Loading) }
                        .catch { emit(HistoryUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

        fun onEvent(event: HistoryUiEvent) {
            when (event) {
                is HistoryUiEvent.QueryChange -> _query.value = event.query
                is HistoryUiEvent.FilterSelect -> filter.value = event.filter
                HistoryUiEvent.SortClick, HistoryUiEvent.ExportClick -> showComingSoon()
                HistoryUiEvent.Retry -> refreshKey.update { it + 1 }
            }
        }

        private fun showComingSoon() {
            viewModelScope.launch { _effects.send(HistoryEffect.ShowComingSoon) }
        }

        private fun buildState(
            history: List<AttemptSummary>,
            accuracyPercent: Int,
            query: String,
            filter: HistoryFilter,
        ): HistoryUiState {
            if (history.isEmpty()) return HistoryUiState.Empty
            val needle = query.trim()
            val groups =
                history
                    .filter { it.matches(filter) && it.title.contains(needle, ignoreCase = true) }
                    .groupBy { YearMonth.from(it.completedOn) }
                    .map { (month, attempts) -> HistoryMonthGroup(month, attempts) }
            return HistoryUiState.Success(
                filter = filter,
                totalAttempts = history.size,
                accuracyPercent = accuracyPercent,
                groups = groups,
            )
        }

        private fun AttemptSummary.matches(filter: HistoryFilter): Boolean =
            when (filter) {
                HistoryFilter.All -> true
                HistoryFilter.Practice -> type == PackageType.Practice
                HistoryFilter.Tryout -> type == PackageType.Tryout
                HistoryFilter.Utbk -> examLabel == EXAM_UTBK
                HistoryFilter.Cpns -> examLabel == EXAM_CPNS
            }
    }
