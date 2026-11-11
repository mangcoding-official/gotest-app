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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.gotest.app.core.model.PackageType

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onOpenArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        query = viewModel.query,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenPackage = onOpenPackage,
        onOpenResult = onOpenResult,
        onOpenArticle = onOpenArticle,
        modifier = modifier,
    )
}

@Composable
fun SearchScreen(
    query: String,
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onOpenArticle: (articleId: String) -> Unit,
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
        SearchBody(
            uiState = uiState,
            onEvent = onEvent,
            onOpenPackage = onOpenPackage,
            onOpenResult = onOpenResult,
            onOpenArticle = onOpenArticle,
        )
    }
}

@Composable
private fun ColumnScope.SearchBody(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onOpenArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = modifier.fillMaxWidth().weight(1f)
    when (uiState) {
        SearchUiState.Idle -> {
            GotestEmptyView(
                icon = GotestIcons.Linear.SearchNormal,
                title = stringResource(R.string.home_search_idle_title),
                message = stringResource(R.string.home_search_idle_body),
                modifier = fill,
            )
        }

        SearchUiState.Loading -> {
            GotestLoadingView(modifier = fill)
        }

        SearchUiState.Error -> {
            GotestErrorView(onRetry = { onEvent(SearchUiEvent.Retry) }, modifier = fill)
        }

        is SearchUiState.Empty -> {
            SearchResults(
                query = uiState.query,
                filter = uiState.filter,
                results = null,
                onSelectFilter = { onEvent(SearchUiEvent.SelectFilter(it)) },
                onOpenPackage = onOpenPackage,
                onOpenResult = onOpenResult,
                onOpenArticle = onOpenArticle,
                modifier = fill,
            )
        }

        is SearchUiState.Success -> {
            SearchResults(
                query = uiState.query,
                filter = uiState.filter,
                results = uiState,
                onSelectFilter = { onEvent(SearchUiEvent.SelectFilter(it)) },
                onOpenPackage = onOpenPackage,
                onOpenResult = onOpenResult,
                onOpenArticle = onOpenArticle,
                modifier = fill,
            )
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

/** Heading, filter pills and the result sections (or the no-result message when [results] is null). */
@Composable
private fun SearchResults(
    query: String,
    filter: SearchFilter,
    results: SearchUiState.Success?,
    onSelectFilter: (SearchFilter) -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onOpenArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item(key = "heading") { SearchHeading(query = query) }
        item(key = "filters") { SearchFilterRow(selected = filter, onSelect = onSelectFilter) }
        if (results == null) {
            item(key = "empty") {
                GotestEmptyView(
                    icon = GotestIcons.Linear.SearchNormal,
                    title = stringResource(R.string.home_search_empty_title),
                    message = stringResource(R.string.home_search_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            return@LazyColumn
        }
        if (results.tryouts.isNotEmpty()) {
            item(key = "tryouts") {
                SearchPackageSection(
                    title = stringResource(R.string.home_search_section_tryout),
                    packages = results.tryouts,
                    onOpenPackage = onOpenPackage,
                )
            }
        }
        if (results.practices.isNotEmpty()) {
            item(key = "practices") {
                SearchPackageSection(
                    title = stringResource(R.string.home_search_section_practice),
                    packages = results.practices,
                    onOpenPackage = onOpenPackage,
                )
            }
        }
        if (results.topics.isNotEmpty()) {
            item(key = "topics") { SearchTopicSection(topics = results.topics) }
        }
        if (results.attempts.isNotEmpty()) {
            item(key = "attempts") { SearchAttemptSection(attempts = results.attempts, onOpenResult = onOpenResult) }
        }
        if (results.articles.isNotEmpty()) {
            item(key = "articles") { SearchArticleSection(articles = results.articles, onOpenArticle = onOpenArticle) }
        }
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

@Composable
private fun SearchFilterRow(
    selected: SearchFilter,
    onSelect: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        SearchFilter.entries.forEach { filter ->
            GotestFilterPill(
                text = stringResource(filter.labelRes()),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

private fun SearchFilter.labelRes(): Int =
    when (this) {
        SearchFilter.All -> R.string.home_search_filter_all
        SearchFilter.Packages -> R.string.home_search_filter_packages
        SearchFilter.Topics -> R.string.home_search_filter_topics
        SearchFilter.Results -> R.string.home_search_filter_results
        SearchFilter.Articles -> R.string.home_search_filter_articles
    }

@Composable
private fun SearchScreenPreview(
    uiState: SearchUiState,
    query: String = "utbk",
) {
    GotestTheme {
        SearchScreen(
            query = query,
            uiState = uiState,
            onEvent = {},
            onBack = {},
            onOpenPackage = { _, _ -> },
            onOpenResult = {},
            onOpenArticle = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenSuccessPreview() {
    SearchScreenPreview(uiState = SearchPreviewData.success)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenIdlePreview() {
    SearchScreenPreview(uiState = SearchUiState.Idle, query = "")
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenEmptyPreview() {
    SearchScreenPreview(uiState = SearchUiState.Empty(query = "kimia", filter = SearchFilter.All), query = "kimia")
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenLoadingPreview() {
    SearchScreenPreview(uiState = SearchUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SearchScreenErrorPreview() {
    SearchScreenPreview(uiState = SearchUiState.Error)
}
