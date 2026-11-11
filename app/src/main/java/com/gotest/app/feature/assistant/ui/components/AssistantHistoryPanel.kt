package com.gotest.app.feature.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.assistant.ui.assistant.AssistantPreviewData
import com.gotest.app.feature.assistant.ui.assistant.ConversationHistoryUiState
import com.gotest.app.feature.assistant.ui.assistant.HistoryGroup
import com.gotest.app.feature.assistant.ui.assistant.HistoryItem
import com.gotest.app.feature.assistant.ui.assistant.HistoryPeriod
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian: Locale = Locale.forLanguageTag("id-ID")

/** "09.12", as the design writes times in the history. */
private val historyTime = DateTimeFormatter.ofPattern("HH.mm", indonesian)

/** "Sen, 26 Okt". */
private val historyDay = DateTimeFormatter.ofPattern("EEE, d MMM", indonesian)

/**
 * "AI Chat / Menu Riwayat Percakapan" (frame 21622): a panel sliding in from the right over a
 * 40% scrim. The scrim, the close button and Back (handled by the screen) close it.
 */
@Composable
internal fun AssistantHistoryPanel(
    state: ConversationHistoryUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onNewConversation: () -> Unit,
    onConversationClick: (conversationId: String) -> Unit,
    onClearClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.isOpen,
        modifier = modifier,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                        .background(GotestTheme.colors.scrim)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClickLabel = stringResource(R.string.assistant_history_close),
                            onClick = onClose,
                        ),
            )
            HistoryPanelContent(
                state = state,
                query = query,
                onQueryChange = onQueryChange,
                onNewConversation = onNewConversation,
                onConversationClick = onConversationClick,
                onClearClick = onClearClick,
                onClose = onClose,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .animateEnterExit(
                            enter = slideInHorizontally { width -> width },
                            exit = slideOutHorizontally { width -> width },
                        ),
            )
        }
    }
}

/** The white panel itself, without the scrim and animation, so previews can render it. */
@Composable
internal fun HistoryPanelContent(
    state: ConversationHistoryUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onNewConversation: () -> Unit,
    onConversationClick: (conversationId: String) -> Unit,
    onClearClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    // A Surface swallows touches, so taps on the panel never reach the scrim.
    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .widthIn(max = AssistantDimens.historyPanelWidth)
                .fillMaxWidth(),
        color = GotestTheme.colors.bgApp,
    ) {
        Column(
            modifier =
                Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                ),
        ) {
            HistoryHeader(onClose = onClose)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.screenHorizontal)
                        .padding(top = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                GotestButton(
                    text = stringResource(R.string.assistant_history_new),
                    onClick = onNewConversation,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = GotestIcons.Linear.Add,
                )
                HistorySearchField(query = query, onQueryChange = onQueryChange)
                HistoryList(
                    state = state,
                    onConversationClick = onConversationClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.hasHistory) {
                ClearHistoryRow(
                    onClick = onClearClick,
                    modifier =
                        Modifier.padding(
                            start = spacing.screenHorizontal,
                            top = spacing.sm,
                            end = spacing.screenHorizontal,
                            bottom = spacing.xl,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.assistant_history_title),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        GotestIconButton(
            icon = GotestIcons.Linear.CloseCircle,
            contentDescription = stringResource(R.string.assistant_history_close),
            onClick = onClose,
        )
    }
}

/** "Cari percakapan…": a grey, borderless field (unlike the bordered GotestTextField). */
@Composable
private fun HistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.borderFocus),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                        .heightIn(min = AssistantDimens.historySearchHeight)
                        .padding(start = GotestTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(GotestIcons.Linear.SearchNormal),
                    contentDescription = null,
                    modifier = Modifier.size(AssistantDimens.smallIcon),
                    tint = colors.iconDefault,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.assistant_history_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPlaceholder,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        painter = painterResource(GotestIcons.Linear.CloseCircle),
                        contentDescription = stringResource(R.string.assistant_history_search_clear),
                        modifier =
                            Modifier
                                .clip(GotestTheme.shapes.full)
                                .clickable(role = Role.Button, onClick = { onQueryChange("") })
                                .minimumInteractiveComponentSize()
                                .size(AssistantDimens.smallIcon),
                        tint = colors.iconDefault,
                    )
                }
            }
        },
    )
}

@Composable
private fun HistoryList(
    state: ConversationHistoryUiState,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            GotestLoadingView(modifier = modifier.fillMaxWidth())
        }

        !state.hasHistory -> {
            GotestEmptyView(
                icon = GotestIcons.Linear.Message,
                title = stringResource(R.string.assistant_history_empty_title),
                message = stringResource(R.string.assistant_history_empty_message),
                modifier = modifier.fillMaxWidth(),
            )
        }

        // Something is stored, so an empty list means the search matched nothing.
        state.groups.isEmpty() -> {
            GotestEmptyView(
                icon = GotestIcons.Linear.SearchNormal,
                title = stringResource(R.string.assistant_history_no_match_title),
                message = stringResource(R.string.assistant_history_no_match_message),
                modifier = modifier.fillMaxWidth(),
            )
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = GotestTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
            ) {
                items(items = state.groups, key = { it.period }) { group ->
                    HistorySection(group = group, onConversationClick = onConversationClick)
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    group: HistoryGroup,
    onConversationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = group.period.label(),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        group.items.forEach { item ->
            HistoryRow(item = item, period = group.period, onClick = { onConversationClick(item.id) })
        }
    }
}

@Composable
private fun HistoryPeriod.label(): String =
    stringResource(
        when (this) {
            HistoryPeriod.Today -> R.string.assistant_today
            HistoryPeriod.Yesterday -> R.string.assistant_yesterday
            HistoryPeriod.LastSevenDays -> R.string.assistant_history_last_week
            HistoryPeriod.Older -> R.string.assistant_history_older
        },
    )

/** A conversation; the open one sits on brand-soft with "Sedang dibuka". */
@Composable
private fun HistoryRow(
    item: HistoryItem,
    period: HistoryPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = RoundedCornerShape(AssistantDimens.historyRowRadius)
    val time = lastMessageLabel(item.lastMessageAt, period)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (item.isOpen) colors.bgBrandSoft else Color.Transparent)
                .semantics { selected = item.isOpen }
                .clickable(onClick = onClick)
                .padding(horizontal = GotestTheme.spacing.md, vertical = AssistantDimens.historyRowSpacing),
        horizontalArrangement = Arrangement.spacedBy(AssistantDimens.historyRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.Message),
            contentDescription = null,
            modifier = Modifier.size(AssistantDimens.smallIcon),
            tint = colors.iconDefault,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.isOpen) stringResource(R.string.assistant_history_open, time) else time,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/** "09.12" today and yesterday, "Sen, 26 Okt" this week, "12 Okt 2025" before that. */
private fun lastMessageLabel(
    lastMessageAt: LocalDateTime,
    period: HistoryPeriod,
): String =
    when (period) {
        HistoryPeriod.Today, HistoryPeriod.Yesterday -> historyTime.format(lastMessageAt)
        HistoryPeriod.LastSevenDays -> historyDay.format(lastMessageAt)
        HistoryPeriod.Older -> GotestFormat.mediumDate(lastMessageAt.toLocalDate())
    }

@Composable
private fun ClearHistoryRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val danger = GotestTheme.colors.statusDanger
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AssistantDimens.historyRowRadius))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AssistantDimens.historyRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.Trash),
            contentDescription = null,
            modifier = Modifier.size(AssistantDimens.smallIcon),
            tint = danger,
        )
        Text(
            text = stringResource(R.string.assistant_history_clear),
            style = MaterialTheme.typography.labelLarge,
            color = danger,
        )
    }
}

@Composable
private fun HistoryPanelPreview(
    state: ConversationHistoryUiState,
    query: String = "",
) {
    GotestTheme {
        Box(modifier = Modifier.fillMaxSize().background(GotestTheme.colors.scrim)) {
            HistoryPanelContent(
                state = state,
                query = query,
                onQueryChange = {},
                onNewConversation = {},
                onConversationClick = {},
                onClearClick = {},
                onClose = {},
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantHistoryPanelPreview() {
    HistoryPanelPreview(state = AssistantPreviewData.history)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun AssistantHistoryPanelNoMatchPreview() {
    HistoryPanelPreview(
        state = AssistantPreviewData.history.copy(groups = emptyList()),
        query = "kimia",
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantHistoryPanelEmptyPreview() {
    HistoryPanelPreview(state = ConversationHistoryUiState(isOpen = true, isLoading = false))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantHistoryPanelLoadingPreview() {
    HistoryPanelPreview(state = ConversationHistoryUiState(isOpen = true))
}
