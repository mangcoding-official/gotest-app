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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.gotest.app.feature.auth.ui.components.AuthMethodTabs
import com.gotest.app.feature.auth.ui.components.AuthSwitchPrompt
import com.gotest.app.feature.auth.ui.emailcode.EmailCodeSignInEffect
import com.gotest.app.feature.auth.ui.emailcode.EmailCodeSignInForm
import com.gotest.app.feature.auth.ui.emailcode.EmailCodeSignInUiEvent
import com.gotest.app.feature.auth.ui.emailcode.EmailCodeSignInUiState
import com.gotest.app.feature.auth.ui.emailcode.EmailCodeSignInViewModel
import com.gotest.app.feature.auth.ui.phone.PhoneCodeForm
import com.gotest.app.feature.auth.ui.phone.PhoneSignInEffect
import com.gotest.app.feature.auth.ui.phone.PhoneSignInUiEvent
import com.gotest.app.feature.auth.ui.phone.PhoneSignInUiState
import com.gotest.app.feature.auth.ui.phone.PhoneSignInViewModel

/** The design's door illustration is 301dp wide in a 400dp column. */
private const val LOGIN_ILLUSTRATION_WIDTH_FRACTION = 0.75f

@Composable
fun LoginRoute(
    onSignInSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onChildSignIn: () -> Unit,
    onPhoneRegister: (phoneDisplay: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    phoneViewModel: PhoneSignInViewModel = hiltViewModel(),
    emailCodeViewModel: EmailCodeSignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val phoneState by phoneViewModel.uiState.collectAsStateWithLifecycle()
    val emailCodeState by emailCodeViewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(emailCodeViewModel.effects) { effect ->
        when (effect) {
            EmailCodeSignInEffect.SignedIn -> onSignInSuccess()
        }
    }
    ObserveAsEvents(phoneViewModel.effects) { effect ->
        when (effect) {
            PhoneSignInEffect.SignedIn -> onSignInSuccess()
            is PhoneSignInEffect.NeedsRegistration -> onPhoneRegister(effect.phoneDisplay)
        }
    }
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
        onChildSignIn = onChildSignIn,
        phoneState = phoneState,
        onPhoneEvent = phoneViewModel::onEvent,
        emailCodeState = emailCodeState,
        onEmailCodeEvent = emailCodeViewModel::onEvent,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    onChildSignIn: () -> Unit = {},
    phoneState: PhoneSignInUiState = PhoneSignInUiState(),
    onPhoneEvent: (PhoneSignInUiEvent) -> Unit = {},
    emailCodeState: EmailCodeSignInUiState = EmailCodeSignInUiState(),
    onEmailCodeEvent: (EmailCodeSignInUiEvent) -> Unit = {},
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
            // Nomor HP first while WhatsApp codes are available, as on the web.
            var phoneTab by rememberSaveable { mutableStateOf(true) }
            val showPhone = phoneState.isAvailable && phoneTab
            if (phoneState.isAvailable) AuthMethodTabs(phoneSelected = phoneTab, onSelectPhone = { phoneTab = it })
            // T-023: with "Kode lewat email" on, the Email tab may sign in with a code instead of the password.
            val codeMode = !showPhone && emailCodeState.isAvailable && emailCodeState.isCodeMode
            when {
                showPhone -> {
                    PhoneCodeForm(
                        uiState = phoneState,
                        onEvent = onPhoneEvent,
                        submitText = stringResource(R.string.phone_submit_login),
                    )
                }

                codeMode -> {
                    EmailCodeSignInForm(uiState = emailCodeState, onEvent = onEmailCodeEvent)
                }

                else -> {
                    LoginForm(uiState = uiState, onEvent = onEvent, onForgotPassword = onForgotPassword)
                }
            }
            LoginActions(
                passwordForm = !showPhone && !codeMode,
                codeAvailable = emailCodeState.isAvailable,
                codeMode = codeMode,
                showGoogle = uiState.showGoogle,
                isSubmitting = uiState.isSubmitting,
                onEvent = onEvent,
                onUseCode = { onEmailCodeEvent(EmailCodeSignInUiEvent.UseCode(true)) },
                onRegister = onRegister,
                onChildSignIn = onChildSignIn,
            )
            AuthCopyright()
        }
    }
}

/**
 * Masuk under the form. As on the web, the code form leaves out the password button, Google, and Daftar;
 * "Masuk dengan kode keluarga" always stays at the bottom (Aturan 11.25).
 */
@Composable
private fun LoginActions(
    passwordForm: Boolean,
    codeAvailable: Boolean,
    codeMode: Boolean,
    showGoogle: Boolean,
    isSubmitting: Boolean,
    onEvent: (LoginUiEvent) -> Unit,
    onUseCode: () -> Unit,
    onRegister: () -> Unit,
    onChildSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        if (passwordForm) {
            GotestButton(
                text = stringResource(R.string.login_submit),
                onClick = { onEvent(LoginUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            )
            if (codeAvailable) {
                GotestButton(
                    text = stringResource(R.string.login_email_code_link),
                    onClick = onUseCode,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Text,
                )
            }
        }
        if (!codeMode) {
            if (showGoogle) {
                GotestButton(
                    text = stringResource(R.string.login_google),
                    onClick = { onEvent(LoginUiEvent.GoogleSignIn) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                    enabled = !isSubmitting,
                )
            }
            AuthSwitchPrompt(
                prompt = stringResource(R.string.login_no_account),
                linkText = stringResource(R.string.login_register_link),
                onLinkClick = onRegister,
            )
        }
        GotestButton(
            text = stringResource(R.string.child_login_link),
            onClick = onChildSignIn,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Text,
        )
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
            supportingText = uiState.emailError?.let { stringResource(it) } ?: uiState.emailMessage,
            isError = uiState.emailError != null || uiState.emailMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        val passwordError =
            (uiState.passwordError ?: uiState.formError)?.let { stringResource(it) } ?: uiState.formMessage
        GotestPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.login_password_label),
            supportingText = passwordError,
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
            uiState = LoginUiState(email = "gilangpermana@mail.com", showGoogle = true),
            onEvent = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 1100)
@Composable
private fun LoginScreenEmailCodePreview() {
    GotestTheme {
        LoginScreen(
            uiState = LoginUiState(),
            onEvent = {},
            onForgotPassword = {},
            onRegister = {},
            emailCodeState =
                EmailCodeSignInUiState(
                    isAvailable = true,
                    isCodeMode = true,
                    isCodeStep = true,
                    sentTo = "g***@mail.com",
                ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LoginScreenErrorPreview() {
    GotestTheme {
        LoginScreen(
            uiState = LoginUiState(email = "gilang", emailError = R.string.auth_error_email, showGoogle = true),
            onEvent = {},
            onForgotPassword = {},
            onRegister = {},
        )
    }
}
