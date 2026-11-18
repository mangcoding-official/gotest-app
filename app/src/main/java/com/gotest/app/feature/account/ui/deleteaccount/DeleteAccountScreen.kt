package com.gotest.app.feature.account.ui.deleteaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import kotlinx.coroutines.launch

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

            DeleteAccountEffect.Failed -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.state_error_message)) }
            }
        }
    }
    DeleteAccountScreen(
        uiState = uiState,
        actions = DeleteAccountActions(onBack, viewModel::onConfirmationChange, viewModel::delete),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

data class DeleteAccountActions(
    val onBack: () -> Unit,
    val onConfirmationChange: (String) -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    actions: DeleteAccountActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val ready = !uiState.isLoading
    AccountDetailScaffold(
        title = stringResource(R.string.account_delete_title),
        onBack = actions.onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (ready) {
                AccountBottomActions {
                    // As on the web, the button does not wait for the right text; the server checks it.
                    GotestButton(
                        text = stringResource(R.string.account_delete_submit),
                        onClick = actions.onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isDeleting,
                        containerColor = GotestTheme.colors.statusDanger,
                        contentColor = GotestTheme.colors.textOnBrand,
                    )
                }
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            GotestLoadingView(modifier = Modifier.padding(padding))
        } else {
            DeleteForm(
                uiState = uiState,
                onConfirmationChange = actions.onConfirmationChange,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DeleteForm(
    uiState: DeleteAccountUiState,
    onConfirmationChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
    ) {
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
            Paragraph(stringResource(R.string.account_delete_message))
            if (uiState.soleManagerOf.isNotEmpty()) {
                Paragraph(stringResource(R.string.account_delete_sole_manager, joinNames(uiState.soleManagerOf)))
            }
            GotestTextField(
                value = uiState.confirmation,
                onValueChange = onConfirmationChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.account_delete_confirm_label),
                placeholder = stringResource(R.string.account_delete_confirm_hint),
                supportingText = uiState.error,
                isError = uiState.error != null,
            )
        }
    }
}

@Composable
private fun Paragraph(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = GotestTheme.colors.textPrimary,
    )
}

private val previewActions = DeleteAccountActions({}, {}, {})

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun DeleteAccountScreenPreview() {
    GotestTheme {
        DeleteAccountScreen(
            uiState =
                DeleteAccountUiState(
                    isLoading = false,
                    soleManagerOf = listOf("SMP 1 Bantargedung", "Bimbel Cerdas"),
                    confirmation = "gotest",
                    error = "Ketik ulang gotest.id untuk menghapus akun.",
                ),
            actions = previewActions,
        )
    }
}
