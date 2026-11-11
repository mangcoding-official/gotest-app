package com.gotest.app.feature.leaderboard.ui.packagesearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.leaderboard.ui.components.ParticipantResultList

@Composable
fun PackageLeaderboardSearchRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackageLeaderboardSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    PackageLeaderboardSearchScreen(
        uiState = uiState,
        query = query,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

/** "Cari Peserta" (frame 21556): back button and search field, then the matching participants. */
@Composable
fun PackageLeaderboardSearchScreen(
    uiState: PackageLeaderboardSearchUiState,
    query: String,
    onEvent: (PackageLeaderboardSearchUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keyboard and navigation bar push the results up; the search bar pads the status bar.
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle)
                .windowInsetsPadding(insets),
    ) {
        SearchBar(
            query = query,
            onQueryChange = { onEvent(PackageLeaderboardSearchUiEvent.QueryChange(it)) },
            onClear = { onEvent(PackageLeaderboardSearchUiEvent.ClearQuery) },
            onBack = onBack,
        )
        val fill = Modifier.fillMaxWidth().weight(1f)
        when (uiState) {
            PackageLeaderboardSearchUiState.Loading -> {
                GotestLoadingView(modifier = fill)
            }

            PackageLeaderboardSearchUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(PackageLeaderboardSearchUiEvent.Retry) }, modifier = fill)
            }

            PackageLeaderboardSearchUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.Leaderboard,
                    title = stringResource(R.string.leaderboard_package_empty_title),
                    message = stringResource(R.string.leaderboard_package_empty_message),
                    modifier = fill,
                )
            }

            PackageLeaderboardSearchUiState.Idle -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.SearchNormal,
                    title = stringResource(R.string.leaderboard_search_idle_title),
                    message = stringResource(R.string.leaderboard_search_note),
                    modifier = fill,
                )
            }

            is PackageLeaderboardSearchUiState.Success -> {
                if (uiState.results.isEmpty()) {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.SearchNormal,
                        title = stringResource(R.string.leaderboard_search_no_results_title),
                        message = stringResource(R.string.leaderboard_search_note),
                        modifier = fill,
                    )
                } else {
                    SearchResults(state = uiState, modifier = fill)
                }
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
    // Open the keyboard on first entry only, not after a configuration change.
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
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestBackButton(onClick = onBack)
        GotestTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = stringResource(R.string.leaderboard_search_placeholder),
            leadingIcon = GotestIcons.Linear.SearchNormal,
            trailingContent =
                if (query.isNotEmpty()) {
                    {
                        // Same compact 20dp target as the password eye, to keep the 44dp field.
                        Icon(
                            painter = painterResource(GotestIcons.Bold.CloseCircle),
                            contentDescription = stringResource(R.string.leaderboard_search_cd_clear),
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

/** "3 peserta ditemukan di …", the result card and the hint below it. */
@Composable
private fun SearchResults(
    state: PackageLeaderboardSearchUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val captionStyle = MaterialTheme.typography.labelSmall
    val captionColor = GotestTheme.colors.textSecondary
    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.sm,
                bottom = spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "count") {
            Text(
                text =
                    stringResource(
                        R.string.leaderboard_search_count,
                        GotestFormat.number(state.results.size),
                        state.packageTitle,
                    ),
                style = captionStyle,
                color = captionColor,
            )
        }
        item(key = "results") {
            ParticipantResultList(entries = state.results, modifier = Modifier.fillMaxWidth())
        }
        item(key = "note") {
            Text(text = stringResource(R.string.leaderboard_search_note), style = captionStyle, color = captionColor)
        }
    }
}

private const val PREVIEW_TITLE = "Tryout UTBK-SNBT 2026 (Sesi 1)"

@Composable
private fun SearchPreviewHost(
    uiState: PackageLeaderboardSearchUiState,
    query: String = "",
) {
    GotestTheme {
        PackageLeaderboardSearchScreen(uiState = uiState, query = query, onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchSuccessPreview() {
    SearchPreviewHost(
        uiState =
            PackageLeaderboardSearchUiState.Success(
                packageTitle = PREVIEW_TITLE,
                results =
                    listOf(
                        LeaderboardEntry(12, "Gilang P.", null, 104, null, 10_280, 1, true),
                        LeaderboardEntry(58, "Gilbert Andreas", null, 88, null, 10_683, 0, false),
                        LeaderboardEntry(143, "Gita Saraswati", null, 61, null, 11_147, 0, false),
                    ),
            ),
        query = "Gil",
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchNoResultsPreview() {
    SearchPreviewHost(
        uiState = PackageLeaderboardSearchUiState.Success(packageTitle = PREVIEW_TITLE, results = emptyList()),
        query = "Zaki",
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchIdlePreview() {
    SearchPreviewHost(uiState = PackageLeaderboardSearchUiState.Idle)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchLoadingPreview() {
    SearchPreviewHost(uiState = PackageLeaderboardSearchUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchEmptyPreview() {
    SearchPreviewHost(uiState = PackageLeaderboardSearchUiState.Empty)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSearchErrorPreview() {
    SearchPreviewHost(uiState = PackageLeaderboardSearchUiState.Error)
}
