package com.gotest.app.feature.auth.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthHeading
import com.gotest.app.feature.auth.ui.components.AuthLogo
import com.gotest.app.feature.auth.ui.components.AuthSwitchPrompt

/** The design's door illustration is 301dp wide in a 400dp column. */
private const val LOGIN_ILLUSTRATION_WIDTH_FRACTION = 0.75f

@Composable
fun LoginRoute(
    onSignInSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            LoginEffect.SignedIn -> onSignInSuccess()
        }
    }
    LoginScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onForgotPassword = onForgotPassword,
        onRegister = onRegister,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthLogo(modifier = Modifier.align(Alignment.Start))
        Image(
            painter = painterResource(R.drawable.illus_login),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(LOGIN_ILLUSTRATION_WIDTH_FRACTION),
        )
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.xl) {
            AuthHeading(
                title = stringResource(R.string.login_title),
                subtitle = stringResource(R.string.login_subtitle),
            )
            LoginForm(uiState = uiState, onEvent = onEvent, onForgotPassword = onForgotPassword)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.login_submit),
                    onClick = { onEvent(LoginUiEvent.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmitting,
                )
                GotestButton(
                    text = stringResource(R.string.login_google),
                    onClick = { onEvent(LoginUiEvent.GoogleSignIn) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                    enabled = !uiState.isSubmitting,
                )
                AuthSwitchPrompt(
                    prompt = stringResource(R.string.login_no_account),
                    linkText = stringResource(R.string.login_register_link),
                    onLinkClick = onRegister,
                )
            }
            AuthCopyright()
        }
    }
}

@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        GotestTextField(
            value = uiState.email,
            onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.login_email_label),
            placeholder = stringResource(R.string.login_email_hint),
            leadingIcon = GotestIcons.Linear.User,
            supportingText = uiState.emailError?.let { stringResource(it) },
            isError = uiState.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        val passwordError = uiState.passwordError ?: uiState.formError
        GotestPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.login_password_label),
            supportingText = passwordError?.let { stringResource(it) },
            isError = passwordError != null,
            keyboardActions = KeyboardActions(onDone = { onEvent(LoginUiEvent.Submit) }),
        )
        Text(
            text = stringResource(R.string.login_forgot_password),
            modifier =
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onForgotPassword),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textBrandDark,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LoginScreenPreview() {
    GotestTheme {
        LoginScreen(
            uiState = LoginUiState(email = "gilangpermana@mail.com"),
            onEvent = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LoginScreenErrorPreview() {
    GotestTheme {
        LoginScreen(
            uiState = LoginUiState(email = "gilang", emailError = R.string.auth_error_email),
            onEvent = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}
