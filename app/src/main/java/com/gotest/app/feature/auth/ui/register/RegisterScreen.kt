package com.gotest.app.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard
import com.gotest.app.feature.auth.ui.components.AuthSwitchPrompt

@Composable
fun RegisterRoute(
    onRegisterSuccess: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            RegisterEffect.Registered -> onRegisterSuccess()
        }
    }
    RegisterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    AuthFormCard(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBack = onBack,
        modifier = modifier,
    ) {
        RegisterFields(uiState = uiState, onEvent = onEvent)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.register_submit),
                onClick = { onEvent(RegisterUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
            )
            GotestButton(
                text = stringResource(R.string.register_google),
                onClick = { onEvent(RegisterUiEvent.GoogleSignUp) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = !uiState.isSubmitting,
            )
            AuthSwitchPrompt(
                prompt = stringResource(R.string.register_has_account),
                linkText = stringResource(R.string.register_login_link),
                onLinkClick = onSignIn,
            )
        }
        TermsNotice()
        AuthCopyright()
    }
}

@Composable
private fun RegisterFields(
    uiState: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        GotestTextField(
            value = uiState.fullName,
            onValueChange = { onEvent(RegisterUiEvent.FullNameChanged(it)) },
            label = stringResource(R.string.register_name_label),
            placeholder = stringResource(R.string.register_name_hint),
            leadingIcon = GotestIcons.Linear.User,
            supportingText = uiState.fullNameError?.let { stringResource(it) },
            isError = uiState.fullNameError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        GotestTextField(
            value = uiState.email,
            onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.register_email_label),
            placeholder = stringResource(R.string.register_email_hint),
            leadingIcon = GotestIcons.Linear.DirectNotification,
            supportingText = uiState.emailError?.let { stringResource(it) },
            isError = uiState.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        GotestPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.register_password_label),
            placeholder = stringResource(R.string.register_password_hint),
            supportingText = uiState.passwordError?.let { stringResource(it) },
            isError = uiState.passwordError != null,
        )
        GotestPasswordField(
            value = uiState.confirmation,
            onValueChange = { onEvent(RegisterUiEvent.ConfirmationChanged(it)) },
            label = stringResource(R.string.register_confirm_label),
            placeholder = stringResource(R.string.register_confirm_hint),
            supportingText = uiState.confirmationError?.let { stringResource(it) },
            isError = uiState.confirmationError != null,
        )
    }
}

@Composable
private fun TermsNotice(modifier: Modifier = Modifier) {
    val link = SpanStyle(color = GotestTheme.colors.textBrandDark)
    val text =
        buildAnnotatedString {
            append(stringResource(R.string.register_terms_prefix))
            append(' ')
            withStyle(link) { append(stringResource(R.string.register_terms_link)) }
            append(' ')
            append(stringResource(R.string.register_terms_and))
            append(' ')
            withStyle(link) { append(stringResource(R.string.register_privacy_link)) }
        }
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.textPlaceholder,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenPreview() {
    GotestTheme {
        RegisterScreen(uiState = RegisterUiState(), onEvent = {}, onSignIn = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenErrorPreview() {
    GotestTheme {
        RegisterScreen(
            uiState =
                RegisterUiState(
                    fullName = "Gilang Permana",
                    email = "gilang",
                    emailError = R.string.auth_error_email,
                    confirmationError = R.string.auth_error_password_mismatch,
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}
