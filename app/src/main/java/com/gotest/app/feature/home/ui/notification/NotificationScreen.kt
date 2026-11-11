package com.gotest.app.feature.home.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents

@Composable
fun NotificationRoute(
    onBack: () -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onOpenPracticePackage: (packageId: String) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is NotificationEffect.OpenResult -> onOpenResult(effect.attemptId)
            is NotificationEffect.OpenPracticePackage -> onOpenPracticePackage(effect.packageId)
            NotificationEffect.OpenLeaderboard -> onOpenLeaderboard()
            NotificationEffect.OpenUpgrade -> onUpgrade()
        }
    }
    NotificationScreen(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

@Composable
fun NotificationScreen(
    uiState: NotificationUiState,
    onEvent: (NotificationUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle)
                .windowInsetsPadding(insets),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.home_notification_title),
            style = GotestTopAppBarStyle.Leading,
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                GotestButton(
                    text = stringResource(R.string.home_notification_mark_all_read),
                    onClick = { onEvent(NotificationUiEvent.MarkAllRead) },
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Medium,
                    enabled = uiState is NotificationUiState.Success || uiState is NotificationUiState.Empty,
                )
            },
        )
        NotificationBody(uiState = uiState, onEvent = onEvent)
    }
}

@Composable
private fun ColumnScope.NotificationBody(
    uiState: NotificationUiState,
    onEvent: (NotificationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = modifier.fillMaxWidth().weight(1f)
    when (uiState) {
        NotificationUiState.Loading -> {
            GotestLoadingView(modifier = fill)
        }

        NotificationUiState.Error -> {
            GotestErrorView(onRetry = { onEvent(NotificationUiEvent.Retry) }, modifier = fill)
        }

        is NotificationUiState.Empty -> {
            Column(modifier = fill) {
                NotificationFilterControl(filter = uiState.filter, unreadCount = uiState.unreadCount, onEvent = onEvent)
                // "Belum dibaca" with everything read is not designed; it reuses the empty layout.
                val unreadOnly = uiState.filter == NotificationFilter.Unread
                GotestEmptyView(
                    icon = GotestIcons.Bold.Notification,
                    title =
                        if (unreadOnly) {
                            stringResource(R.string.home_notification_unread_empty_title)
                        } else {
                            stringResource(R.string.home_notification_empty_title)
                        },
                    message =
                        if (unreadOnly) {
                            stringResource(R.string.home_notification_unread_empty_body)
                        } else {
                            stringResource(R.string.home_notification_empty_body)
                        },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        is NotificationUiState.Success -> {
            Column(modifier = fill) {
                NotificationFilterControl(filter = uiState.filter, unreadCount = uiState.unreadCount, onEvent = onEvent)
                NotificationList(groups = uiState.groups, onOpen = { onEvent(NotificationUiEvent.Open(it)) })
            }
        }
    }
}

@Composable
private fun NotificationFilterControl(
    filter: NotificationFilter,
    unreadCount: Int,
    onEvent: (NotificationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestSegmentedControl(
        options =
            listOf(
                stringResource(R.string.home_notification_filter_all),
                if (unreadCount > 0) {
                    stringResource(R.string.home_notification_filter_unread_count, unreadCount)
                } else {
                    stringResource(R.string.home_notification_filter_unread)
                },
            ),
        selectedIndex = filter.ordinal,
        onSelect = { onEvent(NotificationUiEvent.SelectFilter(NotificationFilter.entries[it])) },
        modifier = modifier.padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.sm),
    )
}

@Composable
private fun NotificationList(
    groups: List<NotificationGroup>,
    onOpen: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.lg,
                bottom = spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        items(groups, key = { it.day.key }) { group ->
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = group.day.label(),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textSecondary,
                )
                GotestCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(),
                    verticalSpacing = spacing.none,
                ) {
                    group.items.forEach { item ->
                        NotificationRow(item = item, onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDay.label(): String =
    when (this) {
        NotificationDay.Today -> stringResource(R.string.home_notification_today)
        NotificationDay.Yesterday -> stringResource(R.string.home_notification_yesterday)
        is NotificationDay.On -> GotestFormat.longDate(date)
    }

@Composable
private fun NotificationScreenPreview(uiState: NotificationUiState) {
    GotestTheme {
        NotificationScreen(uiState = uiState, onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun NotificationScreenSuccessPreview() {
    NotificationScreenPreview(uiState = NotificationPreviewData.success)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun NotificationScreenEmptyPreview() {
    NotificationScreenPreview(uiState = NotificationUiState.Empty(filter = NotificationFilter.All, unreadCount = 0))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun NotificationScreenUnreadEmptyPreview() {
    NotificationScreenPreview(uiState = NotificationUiState.Empty(filter = NotificationFilter.Unread, unreadCount = 0))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun NotificationScreenLoadingPreview() {
    NotificationScreenPreview(uiState = NotificationUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun NotificationScreenErrorPreview() {
    NotificationScreenPreview(uiState = NotificationUiState.Error)
}
