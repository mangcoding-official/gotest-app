package com.gotest.app.feature.account.ui.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordRoute(
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            ChangePasswordEffect.PasswordChanged -> {
                onChangeSuccess()
            }

            is ChangePasswordEffect.ShowMessage -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message ?: resources.getString(R.string.state_error_message))
                }
            }
        }
    }
    ChangePasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ChangePasswordScreen(
    uiState: ChangePasswordUiState,
    onEvent: (ChangePasswordUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val ready = !uiState.isLoading && !uiState.loadFailed
    AccountDetailScaffold(
        title = stringResource(R.string.account_password_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (ready) {
                AccountBottomActions {
                    GotestButton(
                        text = stringResource(R.string.account_password_submit),
                        onClick = { onEvent(ChangePasswordUiEvent.Submit) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSubmitting,
                    )
                }
            }
        },
    ) { padding ->
        when {
            uiState.loadFailed -> {
                GotestErrorView(
                    onRetry = { onEvent(ChangePasswordUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            else -> {
                PasswordForm(uiState = uiState, onEvent = onEvent, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun PasswordForm(
    uiState: ChangePasswordUiState,
    onEvent: (ChangePasswordUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.account_password_description),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        PasswordLinkBox(uiState = uiState, onSend = { onEvent(ChangePasswordUiEvent.SendLink) })
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
            GotestPasswordField(
                value = uiState.current,
                onValueChange = { onEvent(ChangePasswordUiEvent.CurrentChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.account_password_current_label),
                supportingText = uiState.errors["current_password"],
                isError = "current_password" in uiState.errors,
            )
            GotestPasswordField(
                value = uiState.password,
                onValueChange = { onEvent(ChangePasswordUiEvent.PasswordChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.account_password_new_label),
                supportingText = uiState.errors["password"],
                isError = "password" in uiState.errors,
            )
            GotestPasswordField(
                value = uiState.confirmation,
                onValueChange = { onEvent(ChangePasswordUiEvent.ConfirmationChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.account_password_confirm_label),
            )
        }
    }
}

/** "Lupa password saat ini? Kirim tautan …", then the sent note in place of the button. */
@Composable
private fun PasswordLinkBox(
    uiState: ChangePasswordUiState,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val channelRes =
        if (uiState.linkByWhatsApp) R.string.account_password_link_whatsapp else R.string.account_password_link_email
    val boxRes =
        if (uiState.googleLinked) R.string.account_password_link_google else R.string.account_password_link_forgot
    val channel = stringResource(channelRes)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgBrandSoft, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(boxRes, channel),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
        )
        if (uiState.linkSent) {
            Text(
                text =
                    if (uiState.linkByWhatsApp) {
                        stringResource(R.string.account_password_link_sent_whatsapp)
                    } else {
                        stringResource(R.string.account_password_link_sent_email, uiState.email)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = colors.statusSuccess,
            )
        } else {
            GotestButton(
                text = stringResource(linkButtonText(uiState)),
                onClick = onSend,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
                enabled = !uiState.linkSending,
            )
            uiState.linkError?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.statusDanger)
            }
        }
    }
}

private fun linkButtonText(uiState: ChangePasswordUiState): Int =
    when {
        uiState.linkSending -> R.string.account_password_link_sending
        uiState.linkByWhatsApp -> R.string.account_password_link_send_whatsapp
        else -> R.string.account_password_link_send_email
    }

@Preview(showBackground = true, widthDp = 440, heightDp = 860)
@Composable
private fun ChangePasswordScreenPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState =
                ChangePasswordUiState(
                    isLoading = false,
                    email = "gilangpermana@mail.com",
                    current = "rahasia",
                    errors = mapOf("current_password" to "Kata sandi salah."),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 860)
@Composable
private fun ChangePasswordLinkSentPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState =
                ChangePasswordUiState(
                    isLoading = false,
                    googleLinked = true,
                    linkByWhatsApp = true,
                    linkSent = true,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 600)
@Composable
private fun ChangePasswordErrorPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState = ChangePasswordUiState(isLoading = false, loadFailed = true),
            onEvent = {},
            onBack = {},
        )
    }
}
