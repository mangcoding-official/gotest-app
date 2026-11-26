package com.gotest.app.feature.auth.ui.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.domain.EmailVerificationState
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard

/** What "Menunggu verifikasi email" can do. */
data class EmailVerificationActions(
    val onCode: (String) -> Unit,
    val onVerify: () -> Unit,
    val onResend: () -> Unit,
    val onSignOut: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun EmailVerificationRoute(
    onAccountOpen: () -> Unit,
    onSignOutSuccess: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            EmailVerificationEffect.Verified -> onAccountOpen()
            EmailVerificationEffect.SignedOut -> onSignOutSuccess()
        }
    }
    EmailVerificationScreen(
        uiState = uiState,
        actions =
            EmailVerificationActions(
                onCode = viewModel::onCode,
                onVerify = viewModel::verify,
                onResend = viewModel::resend,
                onSignOut = viewModel::signOut,
                onDeleteAccount = onDeleteAccount,
                onRetry = viewModel::load,
            ),
        modifier = modifier,
    )
}

/** Web Auth/EmailVerificationWaiting with its EmailCodeForm: the texts are the web's. */
@Composable
fun EmailVerificationScreen(
    uiState: EmailVerificationUiState,
    actions: EmailVerificationActions,
    modifier: Modifier = Modifier,
) {
    AuthFormCard(
        title = stringResource(R.string.email_verify_heading),
        subtitle = stringResource(R.string.email_verify_lede),
        onBack = null,
        modifier = modifier,
    ) {
        val page = uiState.page
        when {
            page != null -> CodeForm(page = page, uiState = uiState, actions = actions)
            uiState.loadFailed -> GotestErrorView(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth())
            else -> GotestLoadingView(modifier = Modifier.fillMaxWidth())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GotestButton(
                text = stringResource(R.string.consent_sign_out),
                onClick = actions.onSignOut,
                variant = GotestButtonVariant.Text,
                enabled = !uiState.isSigningOut,
            )
            GotestButton(
                text = stringResource(R.string.consent_delete_account),
                onClick = actions.onDeleteAccount,
                variant = GotestButtonVariant.Text,
            )
        }
        AuthCopyright()
    }
}

@Composable
private fun CodeForm(
    page: EmailVerificationState,
    uiState: EmailVerificationUiState,
    actions: EmailVerificationActions,
) {
    val colors = GotestTheme.colors
    val email = page.email.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.email_verify_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        if (page.sendFailed) R.string.email_verify_body_failed else R.string.email_verify_body,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Text(
            // The web shows the account's email in full here, so a typo shows.
            text =
                if (page.sendFailed) {
                    stringResource(R.string.email_verify_notice_failed, email)
                } else {
                    stringResource(R.string.register_code_sent, email)
                },
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        GotestTextField(
            value = uiState.code,
            onValueChange = actions.onCode,
            label = stringResource(R.string.register_code_label),
            placeholder = stringResource(R.string.code_hint),
            supportingText = uiState.problem?.let { problemText(it) },
            isError = uiState.problem != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onVerify() }),
        )
        GotestButton(
            text =
                if (uiState.resendIn > 0) {
                    stringResource(R.string.code_resend_in, uiState.resendIn)
                } else {
                    stringResource(R.string.code_resend)
                },
            onClick = actions.onResend,
            variant = GotestButtonVariant.Text,
            enabled = uiState.resendIn == 0 && !uiState.isResending,
        )
        GotestButton(
            text = stringResource(if (uiState.isVerifying) R.string.auth_processing else R.string.email_verify_submit),
            onClick = actions.onVerify,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSubmit,
        )
    }
}

@Composable
private fun problemText(problem: CodeProblem): String =
    when (problem) {
        is CodeProblem.Server -> problem.message
        CodeProblem.Offline -> stringResource(R.string.email_verify_offline)
    }

private val previewActions = EmailVerificationActions({}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun EmailVerificationFailedPreview() {
    GotestTheme {
        EmailVerificationScreen(
            uiState =
                EmailVerificationUiState(
                    page = EmailVerificationState(email = "gilangpermana@mail.com", sendFailed = true),
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailVerificationSentPreview() {
    GotestTheme {
        EmailVerificationScreen(
            uiState =
                EmailVerificationUiState(
                    page = EmailVerificationState(email = "gilangpermana@mail.com", resendIn = 42),
                    code = "12345",
                    resendIn = 42,
                ),
            actions = previewActions,
        )
    }
}
