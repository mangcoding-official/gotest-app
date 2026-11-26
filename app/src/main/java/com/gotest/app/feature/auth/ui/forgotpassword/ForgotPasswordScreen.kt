package com.gotest.app.feature.auth.ui.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard

@Composable
fun ForgotPasswordRoute(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEvent: (ForgotPasswordUiEvent) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormCard(
        title = stringResource(R.string.forgot_title),
        subtitle =
            stringResource(if (uiState.byWhatsApp) R.string.forgot_subtitle_whatsapp else R.string.forgot_subtitle),
        onBack = onBackToLogin,
        modifier = modifier,
    ) {
        GotestTextField(
            value = uiState.email,
            onValueChange = { onEvent(ForgotPasswordUiEvent.EmailChanged(it)) },
            label =
                stringResource(
                    if (uiState.byWhatsApp) R.string.forgot_whatsapp_label else R.string.forgot_email_label,
                ),
            placeholder =
                stringResource(
                    if (uiState.byWhatsApp) R.string.forgot_whatsapp_hint else R.string.forgot_email_hint,
                ),
            leadingIcon = GotestIcons.Linear.DirectNotification,
            supportingText = uiState.emailMessage ?: uiState.emailError?.let { stringResource(it) },
            isError = uiState.emailMessage != null || uiState.emailError != null,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (uiState.byWhatsApp) KeyboardType.Text else KeyboardType.Email,
                    imeAction = ImeAction.Send,
                ),
            keyboardActions = KeyboardActions(onSend = { onEvent(ForgotPasswordUiEvent.Submit) }),
        )
        uiState.sentTo?.let { email ->
            Text(
                text = uiState.sentMessage ?: stringResource(R.string.forgot_sent, email),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.statusSuccess,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text =
                    stringResource(
                        if (uiState.byWhatsApp) R.string.forgot_submit_whatsapp else R.string.forgot_submit,
                    ),
                onClick = { onEvent(ForgotPasswordUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
            )
            GotestButton(
                text = stringResource(R.string.forgot_back_to_login),
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
            )
        }
        AuthCopyright()
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ForgotPasswordScreenPreview() {
    GotestTheme {
        ForgotPasswordScreen(uiState = ForgotPasswordUiState(), onEvent = {}, onBackToLogin = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ForgotPasswordScreenSentPreview() {
    GotestTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(email = "gilang@gmail.com", sentTo = "gilang@gmail.com"),
            onEvent = {},
            onBackToLogin = {},
        )
    }
}
