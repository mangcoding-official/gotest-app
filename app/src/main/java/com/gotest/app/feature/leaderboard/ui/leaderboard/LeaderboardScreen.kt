package com.gotest.app.feature.leaderboard.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardPackageCard
import com.gotest.app.feature.leaderboard.domain.LeaderboardPage
import com.gotest.app.feature.leaderboard.domain.LeaderboardPeriod
import com.gotest.app.feature.leaderboard.domain.LeaderboardRules
import com.gotest.app.feature.leaderboard.domain.LeaderboardScope
import com.gotest.app.feature.leaderboard.domain.NamedOption
import com.gotest.app.feature.leaderboard.ui.components.BoardEmpty
import com.gotest.app.feature.leaderboard.ui.components.BoardPackageCard
import com.gotest.app.feature.leaderboard.ui.components.BoardPodium
import com.gotest.app.feature.leaderboard.ui.components.BoardTable
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardHero
import com.gotest.app.feature.studypackage.ui.components.JenjangDropdown
import kotlinx.coroutines.launch

/** [onOpenPackage] gets the package id and whether it is a Latihan package. */
@Composable
fun LeaderboardRoute(
    onOpenPackage: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) {
        scope.launch { snackbar.showSnackbar(resources.getString(R.string.state_error_message)) }
    }
    LeaderboardScreen(
        uiState = uiState,
        actions =
            LeaderboardActions(
                onSelectTab = viewModel::selectTab,
                onRetry = viewModel::retry,
                onDraftChange = viewModel::onDraftChange,
                onApply = viewModel::applyFilters,
                onMore = viewModel::loadMore,
                onOpenPackage = onOpenPackage,
            ),
        modifier = modifier,
        snackbarHostState = snackbar,
    )
}

data class LeaderboardActions(
    val onSelectTab: (LeaderboardScope) -> Unit,
    val onRetry: () -> Unit,
    val onDraftChange: (LeaderboardFilters) -> Unit,
    val onApply: () -> Unit,
    val onMore: () -> Unit,
    val onOpenPackage: (Long, Boolean) -> Unit,
)

@Composable
fun LeaderboardScreen(
    uiState: LeaderboardUiState,
    actions: LeaderboardActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Tab roots draw under the floating bottom bar.
    val bottomInset =
        GotestTheme.sizes.bottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        LeaderboardList(uiState = uiState, actions = actions, bottomPadding = bottomInset)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset),
        )
    }
}

@Composable
private fun LeaderboardList(
    uiState: LeaderboardUiState,
    actions: LeaderboardActions,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val page = uiState.page
    val shown = uiState.shown
    LazyColumn(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.lg,
                bottom = bottomPadding + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "header") { LeaderboardHeader() }
        item(key = "tabs") { LeaderboardTabs(selected = uiState.scope, onSelect = actions.onSelectTab) }
        // The hero is always the global rank (web LeaderboardHero), so the last page's stays while a tab loads.
        if (page != null) {
            item(
                key = "hero",
            ) { LeaderboardHero(current = page.current?.toRow(), totalParticipants = page.totalParticipants) }
        }
        when {
            shown != null && uiState.scope == LeaderboardScope.Global -> {
                globalBoard(shown, uiState, actions)
            }

            shown != null -> {
                packageCards(
                    shown,
                    practice = uiState.scope == LeaderboardScope.Practice,
                    actions.onOpenPackage,
                )
            }

            uiState.loadFailed -> {
                item(key = "error") { GotestErrorView(onRetry = actions.onRetry) }
            }

            else -> {
                item(key = "loading") { GotestLoadingView() }
            }
        }
    }
}

@Composable
private fun LeaderboardHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.leaderboard_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.leaderboard_lede),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LeaderboardTabs(
    selected: LeaderboardScope,
    onSelect: (LeaderboardScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    PillRow(modifier = modifier) {
        LeaderboardScope.entries.forEach { scope ->
            GotestFilterPill(
                text =
                    stringResource(
                        when (scope) {
                            LeaderboardScope.Global -> R.string.leaderboard_tab_global
                            LeaderboardScope.Package -> R.string.leaderboard_tab_package
                            LeaderboardScope.Practice -> R.string.leaderboard_tab_practice
                        },
                    ),
                selected = scope == selected,
                onClick = { onSelect(scope) },
            )
        }
    }
}

@Composable
private fun PillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) { content() }
}

private fun LazyListScope.globalBoard(
    page: LeaderboardPage,
    uiState: LeaderboardUiState,
    actions: LeaderboardActions,
) {
    item(key = "filters") {
        GlobalFilters(
            categories = page.options.categories,
            subjects = uiState.subjectChoices,
            draft = uiState.draft,
            busy = uiState.isLoading,
            onDraftChange = actions.onDraftChange,
            onApply = actions.onApply,
        )
    }
    item(key = "updated") { UpdatedNote(page.updatedAt, showScale = true) }
    if (page.top.isEmpty()) {
        item(key = "empty") { BoardEmpty(message = stringResource(R.string.leaderboard_empty_global)) }
    } else {
        item(key = "podium") { BoardPodium(top = page.top.map { it.toRow() }, showTime = false) }
    }
    if (page.rows.isNotEmpty()) {
        item(key = "table") { BoardTable(rows = page.rows.map { it.toRow() }) }
    }
    if (page.hasMore) {
        item(key = "more") {
            GotestButton(
                text = stringResource(R.string.leaderboard_more),
                onClick = actions.onMore,
                modifier = Modifier.fillMaxWidth(),
                size = GotestButtonSize.Medium,
                enabled = !uiState.isLoading,
            )
        }
    }
}

@Composable
private fun GlobalFilters(
    categories: List<NamedOption>,
    subjects: List<NamedOption>,
    draft: LeaderboardFilters,
    busy: Boolean,
    onDraftChange: (LeaderboardFilters) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            JenjangDropdown(
                options = categories.map { it.name },
                selectedIndex = categories.indexOfFirst { it.id == draft.category }.takeIf { it >= 0 },
                onSelect = { index -> onDraftChange(draft.copy(category = index?.let { categories[it].id })) },
                allOptionLabel = stringResource(R.string.leaderboard_category_all),
                modifier = Modifier.weight(1f),
            )
            JenjangDropdown(
                options = subjects.map { it.name },
                selectedIndex = subjects.indexOfFirst { it.id == draft.subject }.takeIf { it >= 0 },
                onSelect = { index -> onDraftChange(draft.copy(subject = index?.let { subjects[it].id })) },
                allOptionLabel = stringResource(R.string.leaderboard_subject_all),
                modifier = Modifier.weight(1f),
            )
            val periods = LeaderboardPeriod.entries
            JenjangDropdown(
                options = periods.map { stringResource(it.label()) },
                selectedIndex = periods.indexOf(draft.period).takeIf { it >= 0 },
                onSelect = { index ->
                    onDraftChange(
                        draft.copy(
                            period =
                                index?.let { periods[it] } ?: LeaderboardPeriod.All,
                        ),
                    )
                },
                allOptionLabel = null,
                modifier = Modifier.weight(1f),
            )
        }
        GotestButton(
            text = stringResource(R.string.leaderboard_filter),
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            size = GotestButtonSize.Medium,
            enabled = !busy,
        )
    }
}

private fun LeaderboardPeriod.label(): Int =
    when (this) {
        LeaderboardPeriod.All -> R.string.leaderboard_period_all
        LeaderboardPeriod.Week -> R.string.leaderboard_period_week
        LeaderboardPeriod.Month -> R.string.leaderboard_period_month
        LeaderboardPeriod.Year -> R.string.leaderboard_period_year
    }

/** "Terakhir diperbarui 18 Nov 2026, 14.05" in the phone's time zone, and on Global the points scale. */
@Composable
private fun UpdatedNote(
    updatedAt: String?,
    showScale: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LeaderboardRules.updatedLabel(updatedAt)?.let {
            Text(
                text = stringResource(R.string.leaderboard_updated, it),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        if (showScale) {
            Text(
                text = stringResource(R.string.leaderboard_scale),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

private fun LazyListScope.packageCards(
    page: LeaderboardPage,
    practice: Boolean,
    onOpenPackage: (Long, Boolean) -> Unit,
) {
    item(key = "cards-head") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
        ) {
            Text(
                text =
                    stringResource(
                        if (practice) R.string.leaderboard_practice_title else R.string.leaderboard_packages_title,
                    ),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        if (practice) R.string.leaderboard_practice_lede else R.string.leaderboard_packages_lede,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
            UpdatedNote(page.updatedAt, showScale = false)
        }
    }
    if (page.packageCards.isEmpty()) {
        item(key = "cards-empty") { PackageCardsEmpty(practice) }
    }
    items(page.packageCards, key = LeaderboardPackageCard::id) { card ->
        BoardPackageCard(card = card, onOpen = { onOpenPackage(card.id, practice) })
    }
}

@Composable
private fun PackageCardsEmpty(
    practice: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = if (practice) R.string.leaderboard_practice_empty_title else R.string.leaderboard_packages_empty_title
    val body = if (practice) R.string.leaderboard_practice_empty_body else R.string.leaderboard_packages_empty_body
    GotestCard(modifier = modifier.fillMaxWidth()) {
        GotestEmptyView(
            icon = GotestIcons.Linear.Leaderboard,
            title = stringResource(title),
            message = stringResource(body),
        )
    }
}

private val previewActions = LeaderboardActions({}, {}, {}, {}, {}, { _, _ -> })

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun LeaderboardScreenPreview() {
    GotestTheme {
        LeaderboardScreen(
            uiState = LeaderboardUiState(page = LeaderboardPreviewData.global, isLoading = false),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun LeaderboardPackagesPreview() {
    GotestTheme {
        LeaderboardScreen(
            uiState =
                LeaderboardUiState(
                    scope = LeaderboardScope.Package,
                    page = LeaderboardPreviewData.packages,
                    isLoading = false,
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardEmptyPreview() {
    GotestTheme {
        LeaderboardScreen(
            uiState = LeaderboardUiState(page = LeaderboardPage(), isLoading = false),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardLoadingPreview() {
    GotestTheme { LeaderboardScreen(uiState = LeaderboardUiState(), actions = previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardErrorPreview() {
    GotestTheme {
        LeaderboardScreen(uiState = LeaderboardUiState(isLoading = false, loadFailed = true), actions = previewActions)
    }
}
