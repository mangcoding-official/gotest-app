package com.gotest.app.feature.history.ui.history

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestSkeletonCardList
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.history.data.HistoryCsvSaver
import com.gotest.app.feature.history.domain.HistoryCategory
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.domain.HistoryStats
import kotlinx.coroutines.launch

@Composable
fun HistoryRoute(
    onOpenResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val context = LocalContext.current
    ObserveAsEvents(viewModel.effects) { effect ->
        scope.launch {
            when (effect) {
                is HistoryEffect.Exported -> {
                    val result =
                        snackbar.showSnackbar(
                            message = resources.getString(R.string.history_export_saved),
                            actionLabel = resources.getString(R.string.history_export_share),
                            duration = SnackbarDuration.Long,
                        )
                    if (result == SnackbarResult.ActionPerformed) context.shareCsv(effect.uri)
                }

                HistoryEffect.ExportFailed -> {
                    snackbar.showSnackbar(resources.getString(R.string.history_export_failed))
                }

                HistoryEffect.ListFailed -> {
                    snackbar.showSnackbar(resources.getString(R.string.state_error_message))
                }
            }
        }
    }
    HistoryScreen(
        uiState = uiState,
        actions =
            HistoryActions(
                onRetry = viewModel::load,
                onDraftChange = viewModel::onDraftChange,
                onApply = viewModel::applyFilters,
                onMore = viewModel::loadMore,
                onExport = viewModel::export,
                onOpenResult = onOpenResult,
            ),
        modifier = modifier,
        snackbarHostState = snackbar,
    )
}

private fun android.content.Context.shareCsv(uri: Uri) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType(HistoryCsvSaver.MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(send, null))
}

data class HistoryActions(
    val onRetry: () -> Unit,
    val onDraftChange: (HistoryFilters) -> Unit,
    val onApply: () -> Unit,
    val onMore: () -> Unit,
    val onExport: () -> Unit,
    val onOpenResult: (Long) -> Unit,
)

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Tab roots draw under the floating bottom bar.
    val bottomInset =
        GotestTheme.sizes.bottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryHeader()
            when {
                uiState.isLoading -> {
                    GotestSkeletonCardList(
                        modifier =
                            Modifier.padding(
                                horizontal = GotestTheme.spacing.screenHorizontal,
                                vertical = GotestTheme.spacing.md,
                            ),
                    )
                }

                uiState.loadFailed -> {
                    GotestErrorView(
                        onRetry = actions.onRetry,
                        modifier = Modifier.fillMaxSize().padding(bottom = bottomInset),
                    )
                }

                else -> {
                    HistoryList(uiState = uiState, actions = actions, bottomPadding = bottomInset)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset),
        )
    }
}

@Composable
private fun HistoryHeader(modifier: Modifier = Modifier) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = GotestTheme.sizes.appBarHeight)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.history_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.history_lede),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun HistoryList(
    uiState: HistoryUiState,
    actions: HistoryActions,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val groups = remember(uiState.results) { groupByMonth(uiState.results) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.lg,
                bottom = bottomPadding + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item { HistoryStatTiles(uiState.stats) }
        item {
            HistoryFilterPanel(
                categories = uiState.categories,
                draft = uiState.draft,
                busy = uiState.isFiltering,
                onDraftChange = actions.onDraftChange,
                onApply = actions.onApply,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history_showing, uiState.results.size, uiState.total),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
                GotestButton(
                    text =
                        stringResource(
                            if (uiState.isExporting) R.string.history_loading else R.string.history_export,
                        ),
                    onClick = actions.onExport,
                    variant = GotestButtonVariant.Plain,
                    size = GotestButtonSize.Small,
                    leadingIcon = GotestIcons.Linear.Export,
                    enabled = !uiState.isExporting,
                )
            }
        }
        if (uiState.results.isEmpty()) {
            item {
                HistoryEmpty(
                    filtered = uiState.applied.isFiltered,
                    onResetFilters = {
                        actions.onDraftChange(HistoryFilters())
                        actions.onApply()
                    },
                )
            }
        }
        groups.forEach { (month, results) ->
            item(key = "month-$month") { MonthLabel(month) }
            items(results, key = { it.id }) { result ->
                HistoryResultRow(result = result, onOpen = { actions.onOpenResult(result.id) })
            }
        }
        if (uiState.nextPage != null) {
            item {
                GotestButton(
                    text =
                        stringResource(
                            if (uiState.isLoadingMore) R.string.history_loading else R.string.history_more,
                        ),
                    onClick = actions.onMore,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                    enabled = !uiState.isLoadingMore,
                )
            }
        }
    }
}

/** No results: "Reset Filter" when the filters left nothing, otherwise the first-time empty state. */
@Composable
private fun HistoryEmpty(
    filtered: Boolean,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filtered) {
        GotestEmptyView(
            icon = GotestIcons.Linear.SearchNormal,
            title = stringResource(R.string.history_empty_filtered_title),
            message = stringResource(R.string.history_empty_filtered_message),
            modifier = modifier,
            actionText = stringResource(R.string.history_reset_filter),
            onAction = onResetFilters,
        )
    } else {
        GotestEmptyView(
            icon = GotestIcons.Linear.Directbox,
            title = stringResource(R.string.history_empty_title),
            message = stringResource(R.string.history_empty_message),
            modifier = modifier,
        )
    }
}

private val previewResults =
    listOf(
        HistoryResult(
            id = 1,
            packageName = "Try Out UTBK-SNBT 3",
            sessionTitle = "Try Out Bersama November",
            categoryName = "UTBK-SNBT",
            mode = "tryout",
            submittedAt = "2026-11-17T08:30:00Z",
            durationLabel = "95 Menit 12 Detik",
            accuracy = 71,
            score = 612.5,
        ),
        HistoryResult(
            id = 2,
            packageName = "Latihan Aljabar",
            categoryName = "TKA SMA",
            mode = "practice",
            submittedAt = "2026-10-30T08:30:00Z",
            durationLabel = "18 Menit 4 Detik",
            accuracy = 90,
            score = 92.0,
            scoreLabel = "Nilai",
        ),
    )

private val previewActions = HistoryActions({}, {}, {}, {}, {}, {})

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HistoryScreenPreview() {
    GotestTheme {
        HistoryScreen(
            uiState =
                HistoryUiState(
                    isLoading = false,
                    stats = HistoryStats(480, 68, 19),
                    categories = listOf(HistoryCategory(1, "TKA SMA"), HistoryCategory(2, "UTBK-SNBT")),
                    results = previewResults,
                    total = 27,
                    nextPage = 2,
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    GotestTheme { HistoryScreen(uiState = HistoryUiState(isLoading = false), actions = previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenLoadingPreview() {
    GotestTheme { HistoryScreen(uiState = HistoryUiState(), actions = previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenErrorPreview() {
    GotestTheme {
        HistoryScreen(
            uiState = HistoryUiState(isLoading = false, loadFailed = true),
            actions = previewActions,
        )
    }
}
