package com.gotest.app.feature.account.ui.help

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.account.domain.HelpCategory
import com.gotest.app.feature.account.domain.HelpCategoryKind
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpContactType
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountNote
import com.gotest.app.feature.account.ui.components.AccountOutlinedSurface
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.AccountRowArrow
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.AccountTileSize
import com.gotest.app.feature.account.ui.components.HelpAccordionItem
import com.gotest.app.feature.account.ui.components.openHelpContact
import kotlinx.coroutines.launch

@Composable
fun HelpRoute(
    onBack: () -> Unit,
    onOpenTopic: (topicId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HelpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val noApp = stringResource(R.string.account_help_no_app)
    HelpScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenTopic = onOpenTopic,
        onContactClick = { contact ->
            if (!context.openHelpContact(contact)) scope.launch { snackbarHostState.showSnackbar(noApp) }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun HelpScreen(
    uiState: HelpUiState,
    onEvent: (HelpUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenTopic: (topicId: String) -> Unit,
    onContactClick: (HelpContact) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_help_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when (uiState) {
            HelpUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            HelpUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(HelpUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is HelpUiState.Success -> {
                HelpContent(
                    state = uiState,
                    onEvent = onEvent,
                    onOpenTopic = onOpenTopic,
                    onContactClick = onContactClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun HelpContent(
    state: HelpUiState.Success,
    onEvent: (HelpUiEvent) -> Unit,
    onOpenTopic: (topicId: String) -> Unit,
    onContactClick: (HelpContact) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.account_help_description),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        GotestTextField(
            value = state.query,
            onValueChange = { onEvent(HelpUiEvent.QueryChange(it)) },
            placeholder = stringResource(R.string.account_help_search_hint),
            leadingIcon = GotestIcons.Linear.SearchNormal,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        HelpSection(title = stringResource(R.string.account_help_categories)) {
            AccountListCard {
                state.categories.forEach { category ->
                    CategoryRow(category = category, onClick = { onOpenTopic(category.topicId) })
                }
            }
        }
        HelpSection(title = stringResource(R.string.account_help_faq)) {
            FaqList(state = state, onToggle = { onEvent(HelpUiEvent.FaqToggle(it)) })
        }
        HelpSection(title = stringResource(R.string.account_help_contact)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                state.contacts.forEach { contact ->
                    ContactTile(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AccountNote(
                text = state.operatingHours,
                icon = GotestIcons.Linear.Calendar,
                containerColor = GotestTheme.colors.bgYellowSoft,
                iconColor = GotestTheme.colors.statusWarning,
                textColor = GotestTheme.colors.bgYellowDark,
            )
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        AccountSectionTitle(title = title)
        content()
    }
}

@Composable
private fun FaqList(
    state: HelpUiState.Success,
    onToggle: (faqId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountListCard(modifier = modifier) {
        if (state.faqs.isEmpty()) {
            GotestEmptyView(
                icon = GotestIcons.Linear.SearchNormal,
                title = stringResource(R.string.account_help_search_empty_title),
                message = stringResource(R.string.account_help_search_empty_message, state.query.trim()),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            state.faqs.forEach { faq ->
                HelpAccordionItem(
                    question = faq.question,
                    answer = faq.answer,
                    expanded = faq.id in state.expandedFaqIds,
                    onToggle = { onToggle(faq.id) },
                )
            }
        }
    }
}

@Immutable
private data class TileStyle(
    @param:DrawableRes val icon: Int,
    val tint: Color,
    val container: Color,
)

@Composable
private fun HelpCategoryKind.style(): TileStyle {
    val colors = GotestTheme.colors
    return when (this) {
        HelpCategoryKind.Account -> TileStyle(GotestIcons.Bold.User, colors.iconBrand, colors.bgBrandSoft)
        HelpCategoryKind.Practice -> TileStyle(GotestIcons.Bold.Note2, colors.bgBlue, colors.bgBlueSoft)
        HelpCategoryKind.Payment -> TileStyle(GotestIcons.Bold.Bank, colors.bgGreen, colors.bgGreenSoft)
    }
}

@Composable
private fun HelpContactType.style(): TileStyle {
    val colors = GotestTheme.colors
    return when (this) {
        HelpContactType.Email -> TileStyle(GotestIcons.Bold.Directbox, colors.iconBrand, colors.bgBrandSoft)
        HelpContactType.WhatsApp -> TileStyle(GotestIcons.Bold.Message, colors.bgGreen, colors.bgGreenSoft)
    }
}

@Composable
private fun CategoryRow(
    category: HelpCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val style = category.kind.style()
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = GotestTheme.colors.bgApp) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIconTile(
                icon = style.icon,
                tint = style.tint,
                containerColor = style.container,
                size = AccountTileSize.Medium,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
            AccountRowArrow()
        }
    }
}

@Composable
private fun ContactTile(
    contact: HelpContact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val style = contact.type.style()
    val label =
        stringResource(
            when (contact.type) {
                HelpContactType.Email -> R.string.account_help_contact_email
                HelpContactType.WhatsApp -> R.string.account_help_contact_whatsapp
            },
        )
    AccountOutlinedSurface(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(spacing.md + spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            AccountIconTile(
                icon = style.icon,
                tint = style.tint,
                containerColor = style.container,
                size = AccountTileSize.Compact,
            )
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
            Text(
                text = contact.displayValue,
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

private val previewSuccess =
    HelpUiState.Success(
        query = "",
        categories = AccountPreviewData.helpCategories,
        faqs = AccountPreviewData.helpFaqs,
        expandedFaqIds = setOf("faq_report"),
        contacts = AccountPreviewData.helpContacts,
        operatingHours = "Senin–Jumat, 08:00–17:00 (kecuali libur nasional)",
    )

@Composable
private fun HelpPreviewScreen(uiState: HelpUiState) {
    HelpScreen(uiState = uiState, onEvent = {}, onBack = {}, onOpenTopic = {}, onContactClick = {})
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpScreenPreview() {
    GotestTheme { HelpPreviewScreen(previewSuccess) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpScreenNoResultPreview() {
    GotestTheme { HelpPreviewScreen(previewSuccess.copy(query = "sertifikat", faqs = emptyList())) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpScreenLoadingPreview() {
    GotestTheme { HelpPreviewScreen(HelpUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HelpScreenErrorPreview() {
    GotestTheme { HelpPreviewScreen(HelpUiState.Error) }
}
