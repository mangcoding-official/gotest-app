package com.gotest.app.feature.auth.ui.emailcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * The email step and the code step of "Masuk dengan kode email", then "Masuk dengan kata sandi"
 * (web: Auth/Login.tsx).
 */
@Composable
fun EmailCodeSignInForm(
    uiState: EmailCodeSignInUiState,
    onEvent: (EmailCodeSignInUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        if (uiState.isCodeStep) CodeStep(uiState, onEvent) else EmailStep(uiState, onEvent)
        GotestButton(
            text = stringResource(R.string.login_password_link),
            onClick = { onEvent(EmailCodeSignInUiEvent.UseCode(false)) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Text,
        )
    }
}

@Composable
private fun EmailStep(
    uiState: EmailCodeSignInUiState,
    onEvent: (EmailCodeSignInUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
        GotestTextField(
            value = uiState.email,
            onValueChange = { onEvent(EmailCodeSignInUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.login_code_email_label),
            placeholder = stringResource(R.string.login_code_email_hint),
            leadingIcon = GotestIcons.Linear.User,
            supportingText = uiState.emailMessage ?: stringResource(R.string.login_code_note),
            isError = uiState.emailMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onEvent(EmailCodeSignInUiEvent.SendCode) }),
        )
        GotestButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.login_code_send),
            onClick = { onEvent(EmailCodeSignInUiEvent.SendCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        )
    }
}

@Composable
private fun CodeStep(
    uiState: EmailCodeSignInUiState,
    onEvent: (EmailCodeSignInUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Text(
            text = stringResource(R.string.login_code_sent, uiState.sentTo),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        GotestTextField(
            value = uiState.code,
            onValueChange = { onEvent(EmailCodeSignInUiEvent.CodeChanged(it)) },
            label = stringResource(R.string.register_code_label),
            placeholder = stringResource(R.string.code_hint),
            supportingText = uiState.codeMessage,
            isError = uiState.codeMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(EmailCodeSignInUiEvent.Submit) }),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.login_submit),
                onClick = { onEvent(EmailCodeSignInUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmitCode,
            )
            GotestButton(
                text =
                    if (uiState.resendInSeconds > 0) {
                        stringResource(R.string.code_resend_in, uiState.resendInSeconds)
                    } else {
                        stringResource(R.string.code_resend)
                    },
                onClick = { onEvent(EmailCodeSignInUiEvent.Resend) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Text,
                enabled = uiState.resendInSeconds == 0 && !uiState.isLoading,
            )
            GotestButton(
                text = stringResource(R.string.register_code_change_email),
                onClick = { onEvent(EmailCodeSignInUiEvent.ChangeEmail) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Text,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailCodeSignInEmailPreview() {
    GotestTheme {
        EmailCodeSignInForm(EmailCodeSignInUiState(isAvailable = true, isCodeMode = true), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailCodeSignInEmailErrorPreview() {
    GotestTheme {
        EmailCodeSignInForm(
            EmailCodeSignInUiState(
                isAvailable = true,
                isCodeMode = true,
                email = "gilang@mail",
                emailMessage = "Tulis email yang benar, misalnya nama@contoh.id.",
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailCodeSignInCodePreview() {
    GotestTheme {
        EmailCodeSignInForm(
            EmailCodeSignInUiState(
                isAvailable = true,
                isCodeMode = true,
                isCodeStep = true,
                sentTo = "g***@mail.com",
                resendInSeconds = 42,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailCodeSignInWrongCodePreview() {
    GotestTheme {
        EmailCodeSignInForm(
            EmailCodeSignInUiState(
                isAvailable = true,
                isCodeMode = true,
                isCodeStep = true,
                sentTo = "a***@gotest.id",
                codeMessage =
                    "Akun ini tidak dapat masuk dengan kode email. " +
                        "Silakan masuk dengan email dan kata sandi.",
            ),
            onEvent = {},
        )
    }
}
