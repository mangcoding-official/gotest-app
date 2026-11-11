package com.gotest.app.feature.history.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.history.domain.ExportFormat
import com.gotest.app.feature.history.domain.ExportPeriod
import com.gotest.app.feature.history.domain.HistoryAttempt
import com.gotest.app.feature.history.domain.HistoryExport
import com.gotest.app.feature.history.domain.HistoryExporter
import com.gotest.app.feature.history.domain.HistoryRepository
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
import java.time.LocalDate
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
        private val historyRepository: HistoryRepository,
        private val historyExporter: HistoryExporter,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val refreshKey = MutableStateFlow(0)
        private val filter = MutableStateFlow(HistoryFilter.All)
        private val sort = MutableStateFlow(HistorySort.Newest)
        private val sheet = MutableStateFlow<SheetSelection?>(null)

        /** Set by "Unduh File" and written once the student has picked where to save it. */
        private var pendingExport: HistoryExport? = null

        // Kept apart from uiState so the search field updates synchronously while typing.
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
        val effects: Flow<HistoryEffect> = _effects.receiveAsFlow()

        /** Every attempt plus the profile's average; shared by the list and the export sheet. */
        private val history: StateFlow<HistoryData> =
            refreshKey
                .flatMapLatest { observeHistory() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryData.Loading)

        val uiState: StateFlow<HistoryUiState> =
            combine(history, _query, filter, sort, ::buildState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

        val sheetState: StateFlow<HistorySheetUiState?> =
            combine(sheet, history) { selection, data -> selection?.toUiState(data.attempts) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        fun onEvent(event: HistoryUiEvent) {
            when (event) {
                is HistoryUiEvent.QueryChange -> _query.value = event.query
                is HistoryUiEvent.FilterSelect -> filter.value = event.filter
                HistoryUiEvent.Retry -> refreshKey.update { it + 1 }
                HistoryUiEvent.SheetDismiss -> sheet.value = null
                HistoryUiEvent.SortClick -> sheet.value = SheetSelection.Sort(sort.value)
                is HistoryUiEvent.SortSelect -> updateSort { it.copy(selected = event.sort) }
                HistoryUiEvent.SortReset -> applySort(HistorySort.Newest)
                HistoryUiEvent.SortApply -> applySelectedSort()
                HistoryUiEvent.ExportClick -> sheet.value = SheetSelection.Export()
                is HistoryUiEvent.ExportPeriodSelect -> updateExport { it.copy(period = event.period) }
                is HistoryUiEvent.ExportFormatSelect -> updateExport { it.copy(format = event.format) }
                HistoryUiEvent.ExportDownload -> requestExportDestination()
                is HistoryUiEvent.ExportDestinationPick -> export(event.uri)
            }
        }

        private fun observeHistory(): Flow<HistoryData> {
            val loaded: Flow<HistoryData> =
                combine(historyRepository.observeAttempts(), userRepository.observeProfile()) { attempts, profile ->
                    HistoryData.Loaded(attempts, profile.stats.accuracyPercent)
                }
            return loaded
                .onStart { emit(HistoryData.Loading) }
                .catch { emit(HistoryData.Failed) }
        }

        private fun applySelectedSort() {
            val selection = sheet.value as? SheetSelection.Sort ?: return
            applySort(selection.selected)
        }

        private fun applySort(selected: HistorySort) {
            sort.value = selected
            sheet.value = null
        }

        private fun updateSort(transform: (SheetSelection.Sort) -> SheetSelection.Sort) {
            sheet.update { current -> (current as? SheetSelection.Sort)?.let(transform) ?: current }
        }

        private fun updateExport(transform: (SheetSelection.Export) -> SheetSelection.Export) {
            sheet.update { current -> (current as? SheetSelection.Export)?.let(transform) ?: current }
        }

        /** Snapshots what the sheet would export, then asks the screen for a destination. */
        private fun requestExportDestination() {
            val selection = sheet.value as? SheetSelection.Export ?: return
            val today = LocalDate.now()
            val attempts = selection.period.select(history.value.attempts, today)
            if (attempts.isEmpty()) return
            val export = HistoryExport(selection.format, selection.period, attempts, exportedOn = today)
            pendingExport = export
            viewModelScope.launch {
                _effects.send(HistoryEffect.PickExportDestination(export.format, export.fileName))
            }
        }

        private fun export(destination: String) {
            val export = pendingExport
            pendingExport = null
            sheet.value = null
            viewModelScope.launch {
                // No pending export means it was lost with the process while the picker was open.
                val result = export?.let { historyExporter.export(destination, it) }
                val message =
                    if (result?.isSuccess == true) R.string.history_export_success else R.string.history_export_failure
                _effects.send(HistoryEffect.ShowMessage(message))
            }
        }

        private fun buildState(
            data: HistoryData,
            query: String,
            filter: HistoryFilter,
            sort: HistorySort,
        ): HistoryUiState =
            when (data) {
                HistoryData.Loading -> HistoryUiState.Loading
                HistoryData.Failed -> HistoryUiState.Error
                is HistoryData.Loaded -> data.toUiState(query, filter, sort)
            }

        private fun HistoryData.Loaded.toUiState(
            query: String,
            filter: HistoryFilter,
            sort: HistorySort,
        ): HistoryUiState {
            if (attempts.isEmpty()) return HistoryUiState.Empty
            val needle = query.trim()
            val matches =
                attempts.filter { it.summary.matches(filter) && it.summary.title.contains(needle, ignoreCase = true) }
            return HistoryUiState.Success(
                filter = filter,
                sort = sort,
                totalAttempts = attempts.size,
                accuracyPercent = accuracyPercent,
                groups = matches.sortedFor(sort).toGroups(sort),
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

        /**
         * The repository lists attempts newest first; sorting is stable, so ties (same accuracy,
         * same name) stay newest first.
         */
        private val HistoryAttempt.title: String get() = summary.title

        private fun List<HistoryAttempt>.sortedFor(sort: HistorySort): List<HistoryAttempt> {
            val newestFirst = sortedByDescending { it.summary.completedOn }
            return when (sort) {
                HistorySort.Newest -> newestFirst
                HistorySort.Oldest -> newestFirst.asReversed()
                HistorySort.AccuracyHigh -> newestFirst.sortedByDescending { it.accuracyPercent }
                HistorySort.AccuracyLow -> newestFirst.sortedBy { it.accuracyPercent }
                HistorySort.Name -> newestFirst.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
        }

        private fun List<HistoryAttempt>.toGroups(sort: HistorySort): List<HistoryAttemptGroup> =
            when {
                isEmpty() -> {
                    emptyList()
                }

                sort.isByDate -> {
                    groupBy { YearMonth.from(it.summary.completedOn) }
                        .map { (month, attempts) -> HistoryAttemptGroup(month, attempts.map { it.summary }) }
                }

                else -> {
                    listOf(HistoryAttemptGroup(month = null, attempts = map { it.summary }))
                }
            }

        private fun SheetSelection.toUiState(attempts: List<HistoryAttempt>): HistorySheetUiState =
            when (this) {
                is SheetSelection.Sort -> {
                    HistorySheetUiState.Sort(selected)
                }

                is SheetSelection.Export -> {
                    val dates = period.select(attempts, LocalDate.now()).map { it.summary.completedOn }
                    HistorySheetUiState.Export(
                        period = period,
                        format = format,
                        attemptCount = dates.size,
                        firstDate = dates.minOrNull(),
                        lastDate = dates.maxOrNull(),
                    )
                }
            }

        /** The sheet's choices; the export summary is derived from them and the attempts. */
        private sealed interface SheetSelection {
            data class Sort(
                val selected: HistorySort,
            ) : SheetSelection

            data class Export(
                val period: ExportPeriod = ExportPeriod.Last30Days,
                val format: ExportFormat = ExportFormat.Pdf,
            ) : SheetSelection
        }

        private sealed interface HistoryData {
            val attempts: List<HistoryAttempt> get() = emptyList()

            data object Loading : HistoryData

            data object Failed : HistoryData

            data class Loaded(
                override val attempts: List<HistoryAttempt>,
                val accuracyPercent: Int,
            ) : HistoryData
        }
    }
