package com.gotest.app.feature.auth.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.auth.domain.CredentialError

/** Logo mark plus the "Gotest.id" wordmark. */
@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = GotestTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconLg),
            tint = Color.Unspecified,
        )
        Text(
            text = stringResource(R.string.auth_logo_text),
            style = MaterialTheme.typography.headlineSmall,
            color = GotestTheme.colors.textBrandDark,
        )
    }
}

@Composable
fun AuthCopyright(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.auth_copyright),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = GotestTheme.colors.textPlaceholder,
        textAlign = TextAlign.Center,
    )
}

/** Card heading: title plus a muted subtitle. */
@Composable
fun AuthHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textPlaceholder)
    }
}

/** "Belum punya akun? Daftar"-style prompt with a brand-coloured link. */
@Composable
fun AuthSwitchPrompt(
    prompt: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = prompt, style = MaterialTheme.typography.bodyMedium, color = GotestTheme.colors.textSecondary)
        TextButton(onClick = onLinkClick) {
            Text(text = linkText, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textBrandDark)
        }
    }
}

@StringRes
fun CredentialError.messageRes(): Int =
    when (this) {
        CredentialError.Required -> R.string.auth_error_required
        CredentialError.InvalidEmail -> R.string.auth_error_email
        CredentialError.PasswordTooShort -> R.string.auth_error_password_length
        CredentialError.PasswordMismatch -> R.string.auth_error_password_mismatch
    }

@Preview(showBackground = true)
@Composable
private fun AuthComponentsPreview() {
    GotestTheme {
        Column(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
            AuthLogo()
            AuthHeading(title = "Masuk dan lanjutkan progresmu", subtitle = "Latihan terarah dan simulasi try out.")
            AuthSwitchPrompt(prompt = "Belum punya akun?", linkText = "Daftar", onLinkClick = {})
            AuthCopyright()
        }
    }
}
