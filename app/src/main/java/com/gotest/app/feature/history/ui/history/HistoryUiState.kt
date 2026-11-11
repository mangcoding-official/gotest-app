package com.gotest.app.feature.history.ui.history

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.feature.history.domain.ExportFormat
import com.gotest.app.feature.history.domain.ExportPeriod
import java.time.LocalDate
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

/** Options of the "Urutkan" sheet, in the design's order. */
enum class HistorySort(
    @param:StringRes val labelRes: Int,
) {
    Newest(R.string.history_sort_newest),
    Oldest(R.string.history_sort_oldest),
    AccuracyHigh(R.string.history_sort_accuracy_high),
    AccuracyLow(R.string.history_sort_accuracy_low),
    Name(R.string.history_sort_name),
    ;

    /** Date orders keep the month headers; the others list every attempt in one card. */
    val isByDate: Boolean get() = this == Newest || this == Oldest
}

/**
 * Attempts shown in one card: those finished in [month] ("Oktober 2026"), or, when the list is
 * not ordered by date, all of them with no header ([month] is null).
 */
@Immutable
data class HistoryAttemptGroup(
    val month: YearMonth?,
    val attempts: List<AttemptSummary>,
) {
    val key: String get() = month?.toString() ?: "all"
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
        val sort: HistorySort,
        val totalAttempts: Int,
        val accuracyPercent: Int,
        val groups: List<HistoryAttemptGroup>,
    ) : HistoryUiState
}

/** The open bottom sheet; kept apart from [HistoryUiState] so a reload does not close it. */
sealed interface HistorySheetUiState {
    /** "Urutkan" (frame 20122); [selected] is only applied by "Terapkan". */
    data class Sort(
        val selected: HistorySort,
    ) : HistorySheetUiState

    /**
     * "Ekspor Riwayat" (frame 20581). [attemptCount], [firstDate] and [lastDate] describe the
     * attempts [period] would export; the dates are null when there are none.
     */
    data class Export(
        val period: ExportPeriod,
        val format: ExportFormat,
        val attemptCount: Int,
        val firstDate: LocalDate?,
        val lastDate: LocalDate?,
    ) : HistorySheetUiState {
        val canDownload: Boolean get() = attemptCount > 0
    }
}

sealed interface HistoryUiEvent {
    data class QueryChange(
        val query: String,
    ) : HistoryUiEvent

    data class FilterSelect(
        val filter: HistoryFilter,
    ) : HistoryUiEvent

    data object Retry : HistoryUiEvent

    /** Closes whichever sheet is open without applying it. */
    data object SheetDismiss : HistoryUiEvent

    data object SortClick : HistoryUiEvent

    data class SortSelect(
        val sort: HistorySort,
    ) : HistoryUiEvent

    /** "Reset": back to [HistorySort.Newest] and close. */
    data object SortReset : HistoryUiEvent

    /** "Terapkan": apply the selected order and close. */
    data object SortApply : HistoryUiEvent

    data object ExportClick : HistoryUiEvent

    data class ExportPeriodSelect(
        val period: ExportPeriod,
    ) : HistoryUiEvent

    data class ExportFormatSelect(
        val format: ExportFormat,
    ) : HistoryUiEvent

    /** "Unduh File": asks where to save before writing anything. */
    data object ExportDownload : HistoryUiEvent

    /** The student picked where to save; [uri] is the new document's content URI. */
    data class ExportDestinationPick(
        val uri: String,
    ) : HistoryUiEvent
}

sealed interface HistoryEffect {
    /** Opens the system "save as" picker for a [format] document suggested as [fileName]. */
    data class PickExportDestination(
        val format: ExportFormat,
        val fileName: String,
    ) : HistoryEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : HistoryEffect
}
