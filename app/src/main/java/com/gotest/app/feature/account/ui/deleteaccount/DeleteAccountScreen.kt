package com.gotest.app.feature.account.ui.deleteaccount

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.domain.AccountValidator
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountTileSize
import kotlinx.coroutines.launch

private val consequences =
    listOf(
        R.string.account_delete_consequence_history,
        R.string.account_delete_consequence_ranking,
        R.string.account_delete_consequence_subscription,
    )

@Composable
fun DeleteAccountRoute(
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            DeleteAccountEffect.AccountDeleted -> {
                onDeleteSuccess()
            }

            is DeleteAccountEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    DeleteAccountScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    onEvent: (DeleteAccountUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    AccountDetailScaffold(
        title = stringResource(R.string.account_delete_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            AccountBottomActions {
                GotestButton(
                    text = stringResource(R.string.account_delete_submit),
                    onClick = { onEvent(DeleteAccountUiEvent.Delete) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canDelete && !uiState.isDeleting,
                    containerColor = colors.statusDanger,
                    contentColor = colors.textOnBrand,
                )
                GotestButton(
                    text = stringResource(R.string.account_cancel),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                    enabled = !uiState.isDeleting,
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        ) {
            GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.lg + spacing.xs) {
                AccountIconTile(
                    icon = GotestIcons.Bold.UserMinus,
                    tint = colors.statusDanger,
                    containerColor = colors.statusDangerSoft,
                    size = AccountTileSize.Large,
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = stringResource(R.string.account_delete_heading),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.account_delete_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                ConsequenceList()
                GotestTextField(
                    value = uiState.confirmation,
                    onValueChange = { onEvent(DeleteAccountUiEvent.ConfirmationChange(it)) },
                    label = stringResource(R.string.account_delete_confirm_label, AccountValidator.DELETE_CONFIRMATION),
                    placeholder = AccountValidator.DELETE_CONFIRMATION,
                    enabled = !uiState.isDeleting,
                    keyboardOptions =
                        KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                )
            }
        }
    }
}

/** "dampak": what the student loses, on a red tint. */
@Composable
private fun ConsequenceList(modifier: Modifier = Modifier) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.statusDangerSoft, MaterialTheme.shapes.large)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm + spacing.xxs),
    ) {
        consequences.forEach { textRes -> ConsequenceRow(textRes = textRes) }
    }
}

@Composable
private fun ConsequenceRow(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
) {
    val danger = GotestTheme.colors.statusDanger
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.CloseCircle),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = danger,
        )
        Text(text = stringResource(textRes), style = MaterialTheme.typography.labelSmall, color = danger)
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DeleteAccountScreenPreview() {
    GotestTheme {
        DeleteAccountScreen(uiState = DeleteAccountUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DeleteAccountScreenConfirmedPreview() {
    GotestTheme {
        DeleteAccountScreen(
            uiState = DeleteAccountUiState(confirmation = "gotest.id", canDelete = true),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DeleteAccountScreenDeletingPreview() {
    GotestTheme {
        DeleteAccountScreen(
            uiState = DeleteAccountUiState(confirmation = "gotest.id", canDelete = true, isDeleting = true),
            onEvent = {},
            onBack = {},
        )
    }
}
