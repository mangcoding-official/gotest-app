package com.gotest.app.feature.account.ui.editprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Web EmailCodeForm in EditAccountModal: "Cek email kamu", the code, "Kirim ulang", "Batal". */
@Composable
internal fun EmailCodeForm(
    step: EmailCodeStep,
    onEvent: (EditProfileUiEvent) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
    ) {
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
            Text(
                text = stringResource(R.string.account_email_code_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.account_email_code_lede),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Text(text = sentToText(step.sentTo), style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
            GotestTextField(
                value = step.code,
                onValueChange = { onEvent(EditProfileUiEvent.CodeChange(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.account_email_code_label),
                placeholder = stringResource(R.string.account_email_code_hint),
                supportingText = step.codeError,
                isError = step.codeError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestButton(
                    text =
                        if (step.resendIn > 0) {
                            stringResource(R.string.account_email_code_resend_in, step.resendIn)
                        } else {
                            stringResource(R.string.account_email_code_resend)
                        },
                    onClick = { onEvent(EditProfileUiEvent.ResendCode) },
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Small,
                    enabled = step.resendIn == 0 && !step.isSaving,
                )
                GotestButton(
                    text = stringResource(R.string.account_email_code_cancel),
                    onClick = { onEvent(EditProfileUiEvent.CancelCode) },
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Small,
                    enabled = !step.isSaving,
                )
            }
        }
    }
}

/** "Kode 6 angka dikirim ke {sentTo}. …" with the address in bold, as on the web. */
@Composable
private fun sentToText(sentTo: String) =
    buildAnnotatedString {
        val full = stringResource(R.string.account_email_code_sent, sentTo)
        val start = full.indexOf(sentTo)
        if (start < 0) {
            append(full)
        } else {
            append(full.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(sentTo) }
            append(full.substring(start + sentTo.length))
        }
    }
