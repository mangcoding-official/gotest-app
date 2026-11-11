package com.gotest.app.feature.history.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.history.domain.ExportFormat
import com.gotest.app.feature.history.ui.components.HistoryMonthSection
import com.gotest.app.feature.history.ui.components.HistoryStatTile
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryRoute(
    onViewResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sheet by viewModel.sheetState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val showMessage: (Int) -> Unit = { messageRes ->
        scope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) }
    }
    val pickExportDestination =
        rememberExportDestinationPicker(
            onPick = { uri -> viewModel.onEvent(HistoryUiEvent.ExportDestinationPick(uri)) },
            onUnavailable = { showMessage(R.string.history_export_failure) },
        )
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is HistoryEffect.PickExportDestination -> pickExportDestination(effect.format, effect.fileName)
            is HistoryEffect.ShowMessage -> showMessage(effect.messageRes)
        }
    }
    HistoryScreen(
        uiState = uiState,
        query = query,
        sheet = sheet,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onViewResult = onViewResult,
        modifier = modifier,
    )
}

/**
 * Opens the system "save as" dialog (Storage Access Framework, no storage permission) and
 * reports the new document's URI; cancelling reports nothing. Needs an Activity, so call it from
 * a Route, never from a previewed Screen.
 */
@Composable
private fun rememberExportDestinationPicker(
    onPick: (uri: String) -> Unit,
    onUnavailable: () -> Unit,
): (ExportFormat, String) -> Unit {
    val currentOnPick by rememberUpdatedState(onPick)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val onResult: (Uri?) -> Unit = { uri -> uri?.let { currentOnPick(it.toString()) } }
    val pdf = rememberLauncherForActivityResult(CreateDocument(ExportFormat.Pdf.mimeType), onResult)
    val csv = rememberLauncherForActivityResult(CreateDocument(ExportFormat.Csv.mimeType), onResult)
    return remember(pdf, csv) {
        { format: ExportFormat, fileName: String ->
            val launcher =
                when (format) {
                    ExportFormat.Pdf -> pdf
                    ExportFormat.Csv -> csv
                }
            // Some devices ship without a document provider to save into.
            if (runCatching { launcher.launch(fileName) }.isFailure) currentOnUnavailable()
        }
    }
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    query: String,
    sheet: HistorySheetUiState?,
    snackbarHostState: SnackbarHostState,
    onEvent: (HistoryUiEvent) -> Unit,
    onViewResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tab roots draw under the floating bottom bar.
    val bottomInset =
        GotestTheme.sizes.bottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryHeader(onEvent = onEvent)
            val stateModifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomInset)
            when (uiState) {
                HistoryUiState.Loading -> {
                    GotestLoadingView(modifier = stateModifier)
                }

                HistoryUiState.Error -> {
                    GotestErrorView(onRetry = { onEvent(HistoryUiEvent.Retry) }, modifier = stateModifier)
                }

                HistoryUiState.Empty -> {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.Directbox,
                        title = stringResource(R.string.history_empty_title),
                        message = stringResource(R.string.history_empty_message),
                        modifier = stateModifier,
                    )
                }

                is HistoryUiState.Success -> {
                    HistoryContent(
                        state = uiState,
                        query = query,
                        bottomInset = bottomInset,
                        onEvent = onEvent,
                        onViewResult = onViewResult,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
        )
        sheet?.let { HistorySheet(state = it, onEvent = onEvent) }
    }
}

@Composable
private fun HistoryHeader(
    onEvent: (HistoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestTopAppBar(
        title = stringResource(R.string.history_title),
        subtitle = stringResource(R.string.history_subtitle),
        style = GotestTopAppBarStyle.Leading,
        modifier = modifier,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                GotestIconButton(
                    icon = GotestIcons.Linear.Sort,
                    contentDescription = stringResource(R.string.history_cd_sort),
                    onClick = { onEvent(HistoryUiEvent.SortClick) },
                )
                GotestIconButton(
                    icon = GotestIcons.Linear.Export,
                    contentDescription = stringResource(R.string.history_cd_export),
                    onClick = { onEvent(HistoryUiEvent.ExportClick) },
                )
            }
        },
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState.Success,
    query: String,
    bottomInset: Dp,
    onEvent: (HistoryUiEvent) -> Unit,
    onViewResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val gutter = Modifier.padding(horizontal = spacing.screenHorizontal)
    // Sections sit 16dp apart, month groups 8dp apart (design "content" vs "Frame 557").
    val sectionGap = Modifier.padding(bottom = spacing.sm)
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = spacing.sm, bottom = bottomInset + spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "search") {
            GotestTextField(
                value = query,
                onValueChange = { onEvent(HistoryUiEvent.QueryChange(it)) },
                modifier = gutter.then(sectionGap).fillMaxWidth(),
                placeholder = stringResource(R.string.history_search_hint),
                leadingIcon = GotestIcons.Linear.SearchNormal,
            )
        }
        item(key = "filters") {
            LazyRow(
                modifier = sectionGap,
                contentPadding = PaddingValues(horizontal = spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(HistoryFilter.entries, key = { it.name }) { filter ->
                    GotestFilterPill(
                        text = stringResource(filter.labelRes),
                        selected = filter == state.filter,
                        onClick = { onEvent(HistoryUiEvent.FilterSelect(filter)) },
                    )
                }
            }
        }
        item(key = "stats") {
            Row(
                modifier = gutter.then(sectionGap),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                HistoryStatTile(
                    value = GotestFormat.number(state.totalAttempts),
                    label = stringResource(R.string.history_stat_attempts),
                    modifier = Modifier.weight(1f),
                )
                HistoryStatTile(
                    value = stringResource(R.string.percent_value, state.accuracyPercent),
                    label = stringResource(R.string.history_stat_accuracy),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.groups.isEmpty()) {
            item(key = "no_results") {
                GotestEmptyView(
                    icon = GotestIcons.Linear.SearchNormal,
                    title = stringResource(R.string.history_no_results_title),
                    message = stringResource(R.string.history_no_results_message),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(state.groups, key = { it.key }) { group ->
                HistoryMonthSection(
                    monthStart = group.month?.atDay(1),
                    attempts = group.attempts,
                    onViewResult = onViewResult,
                    modifier = gutter,
                )
            }
        }
    }
}

private val previewHistory =
    listOf(
        AttemptSummary(
            "a1",
            "p1",
            "Simulasi UTBK-SNBT Session 2",
            PackageType.Tryout,
            "UTBK-SNBT",
            LocalDate.of(2026, 10, 21),
            1_232,
            540,
        ),
        AttemptSummary(
            "a2",
            "p2",
            "Latihan September Session 1",
            PackageType.Practice,
            "UTBK-SNBT",
            LocalDate.of(2026, 10, 20),
            720,
            31,
        ),
        AttemptSummary(
            "a3",
            "p3",
            "Latihan Logaritma Dasar",
            PackageType.Practice,
            "UTBK-SNBT",
            LocalDate.of(2026, 10, 18),
            540,
            7,
        ),
        AttemptSummary(
            "a4",
            "p4",
            "Simulasi UTBK-SNBT Session 1",
            PackageType.Tryout,
            "UTBK-SNBT",
            LocalDate.of(2026, 9, 19),
            1_232,
            540,
        ),
        AttemptSummary(
            "a5",
            "p5",
            "Latihan TWK CPNS Session 1",
            PackageType.Practice,
            "CPNS",
            LocalDate.of(2026, 9, 12),
            900,
            14,
        ),
    )

@Composable
private fun HistoryPreviewHost(
    uiState: HistoryUiState,
    query: String = "",
) {
    GotestTheme {
        HistoryScreen(
            uiState = uiState,
            query = query,
            sheet = null,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onViewResult = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenSuccessPreview() {
    HistoryPreviewHost(
        uiState =
            HistoryUiState.Success(
                filter = HistoryFilter.All,
                sort = HistorySort.Newest,
                totalAttempts = 24,
                accuracyPercent = 86,
                groups =
                    previewHistory
                        .groupBy { YearMonth.from(it.completedOn) }
                        .map { (month, attempts) -> HistoryAttemptGroup(month, attempts) },
            ),
    )
}

/** "Nama paket (A–Z)": not by date, so one card without month headers. */
@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenSortedByNamePreview() {
    HistoryPreviewHost(
        uiState =
            HistoryUiState.Success(
                filter = HistoryFilter.All,
                sort = HistorySort.Name,
                totalAttempts = 24,
                accuracyPercent = 86,
                groups = listOf(HistoryAttemptGroup(month = null, attempts = previewHistory.sortedBy { it.title })),
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenNoResultsPreview() {
    HistoryPreviewHost(
        uiState =
            HistoryUiState.Success(
                filter = HistoryFilter.Cpns,
                sort = HistorySort.Newest,
                totalAttempts = 24,
                accuracyPercent = 86,
                groups = emptyList(),
            ),
        query = "kimia",
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenLoadingPreview() {
    HistoryPreviewHost(uiState = HistoryUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenEmptyPreview() {
    HistoryPreviewHost(uiState = HistoryUiState.Empty)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryScreenErrorPreview() {
    HistoryPreviewHost(uiState = HistoryUiState.Error)
}
