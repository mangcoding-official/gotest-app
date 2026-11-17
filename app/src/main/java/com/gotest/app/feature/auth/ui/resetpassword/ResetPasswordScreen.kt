package com.gotest.app.feature.auth.ui.resetpassword

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
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard

@Composable
fun ResetPasswordRoute(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ResetPasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
fun ResetPasswordScreen(
    uiState: ResetPasswordUiState,
    onEvent: (ResetPasswordUiEvent) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormCard(
        title = stringResource(R.string.reset_title),
        subtitle = stringResource(R.string.reset_subtitle),
        onBack = onBackToLogin,
        modifier = modifier,
    ) {
        if (uiState.isDone) {
            Text(
                text = stringResource(R.string.reset_done),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.statusSuccess,
            )
        } else {
            val emailError = uiState.emailMessage ?: uiState.emailErrorRes?.let { stringResource(it) }
            GotestTextField(
                value = uiState.email,
                onValueChange = { onEvent(ResetPasswordUiEvent.EmailChanged(it)) },
                label = stringResource(R.string.reset_email_label),
                leadingIcon = GotestIcons.Linear.DirectNotification,
                readOnly = uiState.emailLocked,
                supportingText = emailError,
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            GotestPasswordField(
                value = uiState.password,
                onValueChange = { onEvent(ResetPasswordUiEvent.PasswordChanged(it)) },
                label = stringResource(R.string.reset_password_label),
                placeholder = stringResource(R.string.reset_password_hint),
                supportingText = uiState.passwordError?.let { stringResource(it) },
                isError = uiState.passwordError != null,
            )
            GotestPasswordField(
                value = uiState.confirmation,
                onValueChange = { onEvent(ResetPasswordUiEvent.ConfirmationChanged(it)) },
                label = stringResource(R.string.reset_confirm_label),
                placeholder = stringResource(R.string.reset_confirm_hint),
                supportingText = uiState.confirmationError?.let { stringResource(it) },
                isError = uiState.confirmationError != null,
                keyboardActions = KeyboardActions(onDone = { onEvent(ResetPasswordUiEvent.Submit) }),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            if (!uiState.isDone) {
                GotestButton(
                    text =
                        stringResource(
                            if (uiState.isSubmitting) R.string.auth_processing else R.string.reset_submit,
                        ),
                    onClick = { onEvent(ResetPasswordUiEvent.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmitting,
                )
            }
            GotestButton(
                text = stringResource(R.string.reset_back_to_login),
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth(),
                variant = if (uiState.isDone) GotestButtonVariant.Filled else GotestButtonVariant.Plain,
            )
        }
        AuthCopyright()
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ResetPasswordScreenPreview() {
    GotestTheme {
        ResetPasswordScreen(ResetPasswordUiState(email = "budi@contoh.id", emailLocked = true), onEvent = {
        }, onBackToLogin = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ResetPasswordScreenErrorPreview() {
    GotestTheme {
        ResetPasswordScreen(
            ResetPasswordUiState(
                email = "budi@contoh.id",
                emailLocked = true,
                emailMessage = "Tautan atur ulang kata sandi tidak valid atau sudah kedaluwarsa.",
                passwordError = R.string.auth_error_password_number,
            ),
            onEvent = {},
            onBackToLogin = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ResetPasswordScreenSubmittingPreview() {
    GotestTheme {
        ResetPasswordScreen(ResetPasswordUiState(email = "budi@contoh.id", isSubmitting = true), onEvent = {
        }, onBackToLogin = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ResetPasswordScreenDonePreview() {
    GotestTheme {
        ResetPasswordScreen(
            ResetPasswordUiState(email = "budi@contoh.id", isDone = true),
            onEvent = {},
            onBackToLogin = {},
        )
    }
}
