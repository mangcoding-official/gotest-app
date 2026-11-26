package com.gotest.app.feature.home.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.home.domain.SearchItem
import com.gotest.app.feature.home.domain.SearchTarget
import com.gotest.app.feature.practice.ui.catalog.PracticeStartElsewhere
import com.gotest.app.feature.tryout.ui.catalog.TryoutStartElsewhere

/** Where a search result leads (`target` of `GET /search`). */
@Immutable
data class SearchActions(
    val onBack: () -> Unit,
    val onOpenPracticeRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenPracticeResult: (attemptId: Long, packageName: String) -> Unit,
    val onOpenTryoutRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenTryoutResult: (attemptId: Long) -> Unit,
    val onOpenHelpTopic: (slug: String) -> Unit,
)

@Composable
fun SearchRoute(
    actions: SearchActions,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // A package opens its start sheet, as on Beranda; the catalog finds the package by its id.
    var starting by remember { mutableStateOf<SearchTarget?>(null) }
    SearchScreen(
        query = viewModel.query,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = actions.onBack,
        onOpen = { item -> item.target?.let { target -> open(target, item.title, actions) { starting = it } } },
        modifier = modifier,
    )
    val target = starting
    val id = target?.id
    when {
        id == null -> {}

        target.type == TARGET_PRACTICE_PACKAGE -> {
            PracticeStartElsewhere(
                packageId = id,
                onClose = { starting = null },
                onOpenRunner = actions.onOpenPracticeRunner,
                onOpenResult = actions.onOpenPracticeResult,
            )
        }

        else -> {
            TryoutStartElsewhere(
                packageId = id,
                onClose = { starting = null },
                onOpenRunner = actions.onOpenTryoutRunner,
                onOpenResult = actions.onOpenTryoutResult,
            )
        }
    }
}

/** Web `target` types: a package's start sheet, the attempt in progress, a result, or a Bantuan topic. */
private fun open(
    target: SearchTarget,
    title: String,
    actions: SearchActions,
    onStart: (SearchTarget) -> Unit,
) {
    val id = target.id
    when (target.type) {
        TARGET_PRACTICE_PACKAGE, TARGET_TRYOUT_PACKAGE -> onStart(target)
        "practice_attempt" -> id?.let { actions.onOpenPracticeRunner(it, title) }
        "tryout_attempt" -> id?.let { actions.onOpenTryoutRunner(it, title) }
        "practice_result" -> id?.let { actions.onOpenPracticeResult(it, title) }
        "tryout_result" -> id?.let(actions.onOpenTryoutResult)
        "help_topic" -> target.slug?.let(actions.onOpenHelpTopic)
    }
}

private const val TARGET_PRACTICE_PACKAGE = "practice_package"
private const val TARGET_TRYOUT_PACKAGE = "tryout_package"

@Composable
fun SearchScreen(
    query: String,
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpen: (SearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keyboard and navigation bar push the results up; the search bar pads the status bar.
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgApp)
                .windowInsetsPadding(insets),
    ) {
        SearchBar(
            query = query,
            onQueryChange = { onEvent(SearchUiEvent.QueryChange(it)) },
            onClear = { onEvent(SearchUiEvent.ClearQuery) },
            onBack = onBack,
        )
        when (uiState) {
            SearchUiState.Idle -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.SearchNormal,
                    title = stringResource(R.string.home_search_idle_title),
                    message = stringResource(R.string.home_search_idle_body),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }

            is SearchUiState.Results -> {
                SearchResults(results = uiState, onOpen = onOpen, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** Back button plus the search field, focused when the screen opens. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Open the keyboard on first entry only, not when coming back from a result.
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focusRequester) {
        if (!autoFocused) {
            autoFocused = true
            focusRequester.requestFocus()
        }
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestBackButton(onClick = onBack)
        GotestTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = stringResource(R.string.home_search_placeholder),
            leadingIcon = GotestIcons.Linear.SearchNormal,
            trailingContent =
                if (query.isNotEmpty()) {
                    {
                        // Same compact 20dp target as the password eye, to keep the 44dp field.
                        Icon(
                            painter = painterResource(GotestIcons.Linear.CloseCircle),
                            contentDescription = stringResource(R.string.home_search_cd_clear),
                            modifier =
                                Modifier
                                    .size(GotestTheme.sizes.iconMd)
                                    .clickable(role = Role.Button, onClick = onClear),
                            tint = GotestTheme.colors.iconDefault,
                        )
                    }
                } else {
                    null
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
    }
}

/** Web SearchPanel: the heading, "Mencari…" while waiting, "Tidak ada hasil yang cocok.", or the groups. */
@Composable
private fun SearchResults(
    results: SearchUiState.Results,
    onOpen: (SearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item(key = "heading") { SearchHeading(query = results.query) }
        val note =
            when {
                results.isLoading && results.groups.isEmpty() -> R.string.home_search_loading
                !results.isLoading && results.groups.isEmpty() -> R.string.home_search_empty
                else -> null
            }
        note?.let {
            item(key = "note") {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
        items(results.groups, key = { it.key }) { group -> SearchGroupCard(group = group, onOpen = onOpen) }
    }
}

/** "Hasil pencarian untuk “utbk”" with the query emphasised. */
@Composable
private fun SearchHeading(
    query: String,
    modifier: Modifier = Modifier,
) {
    val quoted = stringResource(R.string.home_search_query_quoted, query)
    val full = stringResource(R.string.home_search_results_for, quoted)
    val start = full.indexOf(quoted)
    val text =
        buildAnnotatedString {
            if (start < 0) {
                append(full)
            } else {
                append(full.substring(0, start))
                withStyle(SpanStyle(color = GotestTheme.colors.textPrimary, fontWeight = FontWeight.Medium)) {
                    append(quoted)
                }
                append(full.substring(start + quoted.length))
            }
        }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = GotestTheme.colors.textSecondary,
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenResultsPreview() {
    GotestTheme {
        SearchScreen(
            query = "utbk",
            uiState = SearchUiState.Results(query = "utbk", groups = previewGroups),
            onEvent = {},
            onBack = {},
            onOpen = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenEmptyPreview() {
    GotestTheme {
        SearchScreen(
            query = "zzz",
            uiState = SearchUiState.Results(query = "zzz"),
            onEvent = {},
            onBack = {},
            onOpen = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenIdlePreview() {
    GotestTheme {
        SearchScreen(query = "", uiState = SearchUiState.Idle, onEvent = {}, onBack = {}, onOpen = {})
    }
}
