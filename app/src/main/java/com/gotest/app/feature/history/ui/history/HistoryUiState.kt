package com.gotest.app.feature.history.ui.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.model.AttemptSummary
import java.time.YearMonth

/** Filter pills above the list, in the design's order. */
enum class HistoryFilter(
    @param:StringRes val labelRes: Int,
) {
    All(R.string.history_filter_all),
    Practice(R.string.history_filter_practice),
    Tryout(R.string.history_filter_tryout),
    Utbk(R.string.history_filter_utbk),
    Cpns(R.string.history_filter_cpns),
}

/** Attempts finished in one calendar month ("Oktober 2026"). */
@Immutable
data class HistoryMonthGroup(
    val month: YearMonth,
    val attempts: List<AttemptSummary>,
) {
    val key: String get() = month.toString()
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Error : HistoryUiState

    /** The student has not finished any latihan or tryout yet. */
    data object Empty : HistoryUiState

    /** [groups] is empty when the search or filter matches nothing. */
    @Immutable
    data class Success(
        val filter: HistoryFilter,
        val totalAttempts: Int,
        val accuracyPercent: Int,
        val groups: List<HistoryMonthGroup>,
    ) : HistoryUiState
}

sealed interface HistoryUiEvent {
    data class QueryChange(
        val query: String,
    ) : HistoryUiEvent

    data class FilterSelect(
        val filter: HistoryFilter,
    ) : HistoryUiEvent

    /** Sort and export are not designed yet. */
    data object SortClick : HistoryUiEvent

    data object ExportClick : HistoryUiEvent

    data object Retry : HistoryUiEvent
}

sealed interface HistoryEffect {
    data object ShowComingSoon : HistoryEffect
}
