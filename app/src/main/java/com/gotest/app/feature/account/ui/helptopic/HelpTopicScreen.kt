package com.gotest.app.feature.account.ui.helptopic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.HelpAccordionItem
import com.gotest.app.feature.account.ui.components.openHelpContact
import kotlinx.coroutines.launch

@Composable
fun HelpTopicRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HelpTopicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val noApp = stringResource(R.string.account_help_no_app)
    HelpTopicScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onContactClick = { contact ->
            if (!context.openHelpContact(contact)) scope.launch { snackbarHostState.showSnackbar(noApp) }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun HelpTopicScreen(
    uiState: HelpTopicUiState,
    onEvent: (HelpTopicUiEvent) -> Unit,
    onBack: () -> Unit,
    onContactClick: (HelpContact) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title =
        when (uiState) {
            is HelpTopicUiState.Success -> uiState.title
            is HelpTopicUiState.Empty -> uiState.title
            else -> stringResource(R.string.account_help_topic_fallback_title)
        }
    val subtitle =
        when (uiState) {
            is HelpTopicUiState.Success -> uiState.subtitle
            is HelpTopicUiState.Empty -> uiState.subtitle
            else -> null
        }
    AccountDetailScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        subtitle = subtitle,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when (uiState) {
            HelpTopicUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            HelpTopicUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(HelpTopicUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is HelpTopicUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.MessageQuestion,
                    title = stringResource(R.string.account_help_topic_empty_title),
                    message = stringResource(R.string.account_help_topic_empty_message),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    actionText = stringResource(R.string.account_help_topic_contact),
                    onAction = { onContactClick(uiState.supportContact) },
                )
            }

            is HelpTopicUiState.Success -> {
                HelpTopicContent(
                    state = uiState,
                    onEvent = onEvent,
                    onContactClick = { onContactClick(uiState.supportContact) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun HelpTopicContent(
    state: HelpTopicUiState.Success,
    onEvent: (HelpTopicUiEvent) -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (state.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                state.tags.forEach { tag ->
                    GotestFilterPill(
                        text = tag.label,
                        selected = tag.id == state.selectedTagId,
                        onClick = { onEvent(HelpTopicUiEvent.TagSelect(tag.id)) },
                    )
                }
            }
        }
        AccountListCard {
            state.articles.forEach { article ->
                HelpAccordionItem(
                    question = article.question,
                    answer = article.answer,
                    expanded = article.id in state.expandedArticleIds,
                    onToggle = { onEvent(HelpTopicUiEvent.ArticleToggle(article.id)) },
                )
            }
        }
        NeedHelpCard(onContactClick = onContactClick)
    }
}

/** "butuh-bantuan": purple card with the "Hubungi Kami" button. */
@Composable
private fun NeedHelpCard(
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.bgBrandSoft,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(
                text = stringResource(R.string.account_help_topic_need_help),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textBrandDark,
            )
            Text(
                text = stringResource(R.string.account_help_topic_need_help_message),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textBrandDark,
            )
            GotestButton(
                text = stringResource(R.string.account_help_topic_contact),
                onClick = onContactClick,
                modifier = Modifier.fillMaxWidth(),
                size = GotestButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun HelpTopicPreviewScreen(uiState: HelpTopicUiState) {
    HelpTopicScreen(uiState = uiState, onEvent = {}, onBack = {}, onContactClick = {})
}

private val previewTopic = AccountPreviewData.helpTopic

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpTopicScreenPreview() {
    GotestTheme {
        HelpTopicPreviewScreen(
            HelpTopicUiState.Success(
                title = previewTopic.title,
                subtitle = previewTopic.subtitle,
                tags = previewTopic.tags,
                selectedTagId = "akun",
                articles = previewTopic.articles,
                expandedArticleIds = setOf("akun_pengaturan", "akun_preferensi"),
                supportContact = previewTopic.supportContact,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpTopicScreenFilteredPreview() {
    GotestTheme {
        HelpTopicPreviewScreen(
            HelpTopicUiState.Success(
                title = previewTopic.title,
                subtitle = previewTopic.subtitle,
                tags = previewTopic.tags,
                selectedTagId = "password",
                articles = previewTopic.articles.filter { "password" in it.tagIds },
                expandedArticleIds = setOf("akun_password"),
                supportContact = previewTopic.supportContact,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpTopicScreenEmptyPreview() {
    GotestTheme {
        HelpTopicPreviewScreen(
            HelpTopicUiState.Empty(
                title = previewTopic.title,
                subtitle = previewTopic.subtitle,
                supportContact = previewTopic.supportContact,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpTopicScreenLoadingPreview() {
    GotestTheme { HelpTopicPreviewScreen(HelpTopicUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpTopicScreenErrorPreview() {
    GotestTheme { HelpTopicPreviewScreen(HelpTopicUiState.Error) }
}
