package com.gotest.app.feature.account.ui.downloaddata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.domain.AccountDataCategory
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountCheckboxIndicator
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountHint
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.AccountTileSize
import kotlinx.coroutines.launch

@Composable
fun DownloadDataRoute(
    onBack: () -> Unit,
    onRequestSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            DownloadDataEffect.Requested -> {
                onRequestSuccess()
            }

            is DownloadDataEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    DownloadDataScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** "Akun / Pengaturan – Unduh Data Akun" (frame 23277). */
@Composable
fun DownloadDataScreen(
    uiState: DownloadDataUiState,
    onEvent: (DownloadDataUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_download_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (uiState is DownloadDataUiState.Success) {
                AccountBottomActions {
                    GotestButton(
                        text = stringResource(R.string.account_download_submit),
                        onClick = { onEvent(DownloadDataUiEvent.Submit) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.canSubmit,
                    )
                }
            }
        },
    ) { padding ->
        when (uiState) {
            DownloadDataUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            DownloadDataUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(DownloadDataUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is DownloadDataUiState.Success -> {
                DownloadDataContent(
                    state = uiState,
                    onToggle = { onEvent(DownloadDataUiEvent.Toggle(it)) },
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
private fun DownloadDataContent(
    state: DownloadDataUiState.Success,
    onToggle: (AccountDataCategory) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        IntroCard()
        AccountSectionTitle(title = stringResource(R.string.account_download_select))
        AccountListCard {
            AccountDataCategory.entries.forEach { category ->
                CategoryRow(
                    title = stringResource(category.titleRes()),
                    subtitle = category.subtitle(attemptCount = state.attemptCount),
                    checked = category in state.selected,
                    onCheckedChange = { onToggle(category) },
                    enabled = !state.isSending,
                )
            }
        }
        AccountHint(
            text = stringResource(R.string.account_download_note, state.email),
            icon = GotestIcons.Linear.DirectNotification,
        )
    }
}

/** "intro": grey card explaining what happens. */
@Composable
private fun IntroCard(modifier: Modifier = Modifier) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgGrey, MaterialTheme.shapes.large)
                .padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AccountIconTile(
            icon = GotestIcons.Linear.FolderCloud,
            tint = colors.iconDefault,
            containerColor = colors.bgApp,
            size = AccountTileSize.Choice,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.account_download_intro_title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.account_download_intro_message),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

/** "row": title, subtitle and a checkbox; the whole row toggles. */
@Composable
private fun CategoryRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        }
        AccountCheckboxIndicator(checked = checked)
    }
}

private fun AccountDataCategory.titleRes(): Int =
    when (this) {
        AccountDataCategory.ProfileAndAccount -> R.string.account_download_profile
        AccountDataCategory.PracticeHistory -> R.string.account_download_history
        AccountDataCategory.AssistantChats -> R.string.account_download_chats
        AccountDataCategory.Transactions -> R.string.account_download_transactions
    }

@Composable
private fun AccountDataCategory.subtitle(attemptCount: Int): String =
    when (this) {
        AccountDataCategory.ProfileAndAccount -> {
            stringResource(R.string.account_download_profile_subtitle)
        }

        AccountDataCategory.PracticeHistory -> {
            pluralStringResource(R.plurals.account_download_history_subtitle, attemptCount, attemptCount)
        }

        AccountDataCategory.AssistantChats -> {
            stringResource(R.string.account_download_chats_subtitle)
        }

        AccountDataCategory.Transactions -> {
            stringResource(R.string.account_download_transactions_subtitle)
        }
    }

private val previewSuccess = DownloadDataUiState.Success(email = "gilang@gmail.com", attemptCount = 24)

@Composable
private fun DownloadDataPreviewScreen(uiState: DownloadDataUiState) {
    DownloadDataScreen(uiState = uiState, onEvent = {}, onBack = {})
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DownloadDataScreenPreview() {
    GotestTheme { DownloadDataPreviewScreen(previewSuccess) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun DownloadDataScreenNothingSelectedPreview() {
    GotestTheme { DownloadDataPreviewScreen(previewSuccess.copy(selected = emptySet())) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DownloadDataScreenSendingPreview() {
    GotestTheme { DownloadDataPreviewScreen(previewSuccess.copy(isSending = true)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DownloadDataScreenLoadingPreview() {
    GotestTheme { DownloadDataPreviewScreen(DownloadDataUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DownloadDataScreenErrorPreview() {
    GotestTheme { DownloadDataPreviewScreen(DownloadDataUiState.Error) }
}
