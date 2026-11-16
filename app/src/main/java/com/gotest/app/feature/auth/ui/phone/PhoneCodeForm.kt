package com.gotest.app.feature.auth.ui.phone

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
import com.gotest.app.core.designsystem.theme.GotestTheme

/** The phone step and the code step of signing in with WhatsApp (web: PhoneOtpForm). */
@Composable
fun PhoneCodeForm(
    uiState: PhoneSignInUiState,
    onEvent: (PhoneSignInUiEvent) -> Unit,
    submitText: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        if (!uiState.isCodeStep) {
            val error = uiState.phoneMessage ?: uiState.phoneErrorRes?.let { stringResource(it) }
            GotestTextField(
                value = uiState.phone,
                onValueChange = { onEvent(PhoneSignInUiEvent.PhoneChanged(it)) },
                label = stringResource(R.string.phone_label),
                placeholder = stringResource(R.string.phone_hint),
                supportingText = error ?: stringResource(R.string.phone_helper),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onEvent(PhoneSignInUiEvent.SendCode) }),
            )
            GotestButton(
                text = stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.phone_send),
                onClick = { onEvent(PhoneSignInUiEvent.SendCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
        } else {
            Text(
                text = stringResource(R.string.phone_code_sent, uiState.sentTo),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
            GotestTextField(
                value = uiState.code,
                onValueChange = { onEvent(PhoneSignInUiEvent.CodeChanged(it)) },
                label = stringResource(R.string.phone_code_label),
                placeholder = stringResource(R.string.code_hint),
                supportingText = uiState.codeMessage,
                isError = uiState.codeMessage != null,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { onEvent(PhoneSignInUiEvent.Submit) }),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestButton(
                    text = if (uiState.isLoading) stringResource(R.string.auth_processing) else submitText,
                    onClick = { onEvent(PhoneSignInUiEvent.Submit) },
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
                    onClick = { onEvent(PhoneSignInUiEvent.Resend) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Text,
                    enabled = uiState.resendInSeconds == 0 && !uiState.isLoading,
                )
                GotestButton(
                    text = stringResource(R.string.phone_change),
                    onClick = { onEvent(PhoneSignInUiEvent.ChangeNumber) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Text,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneCodeFormPhonePreview() {
    GotestTheme {
        PhoneCodeForm(PhoneSignInUiState(isAvailable = true, phone = "0812 3456"), onEvent = {}, submitText = "Masuk")
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneCodeFormInvalidPreview() {
    GotestTheme {
        PhoneCodeForm(
            PhoneSignInUiState(
                isAvailable = true,
                phone = "123",
                phoneErrorRes = R.string.phone_invalid,
            ),
            onEvent = {
            },
            submitText = "Masuk",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneCodeFormCodePreview() {
    GotestTheme {
        PhoneCodeForm(
            PhoneSignInUiState(
                isAvailable = true,
                isCodeStep = true,
                sentTo = "0812-3456-7890",
                code = "12",
                resendInSeconds = 42,
            ),
            onEvent = {},
            submitText = "Masuk",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneCodeFormWrongCodePreview() {
    GotestTheme {
        PhoneCodeForm(
            PhoneSignInUiState(
                isAvailable = true,
                isCodeStep = true,
                sentTo = "0812-3456-7890",
                code = "111111",
                codeMessage = "Kode salah. Sisa 4 kali percobaan.",
            ),
            onEvent = {},
            submitText = "Lanjut",
        )
    }
}
