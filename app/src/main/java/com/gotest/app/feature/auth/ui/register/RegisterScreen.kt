package com.gotest.app.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.domain.JoinNotice
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard
import com.gotest.app.feature.auth.ui.components.AuthMethodTabs
import com.gotest.app.feature.auth.ui.components.AuthSwitchPrompt
import com.gotest.app.feature.auth.ui.components.EducationLevelPicker
import com.gotest.app.feature.auth.ui.components.GuardianFields
import com.gotest.app.feature.auth.ui.components.GuardianFormState
import com.gotest.app.feature.auth.ui.phone.PhoneCodeForm
import com.gotest.app.feature.auth.ui.phone.PhoneSignInEffect
import com.gotest.app.feature.auth.ui.phone.PhoneSignInUiEvent
import com.gotest.app.feature.auth.ui.phone.PhoneSignInUiState
import com.gotest.app.feature.auth.ui.phone.PhoneSignInViewModel

@Composable
fun RegisterRoute(
    onRegisterSuccess: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    onPhoneRegister: (phoneDisplay: String) -> Unit,
    onAwaitConsent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
    phoneViewModel: PhoneSignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val phoneState by phoneViewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(phoneViewModel.effects) { effect ->
        when (effect) {
            // A number that already has an account signs in directly, as on the web.
            PhoneSignInEffect.SignedIn -> onRegisterSuccess()

            is PhoneSignInEffect.NeedsRegistration -> onPhoneRegister(effect.phoneDisplay)
        }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            RegisterEffect.Registered -> onRegisterSuccess()
            RegisterEffect.AwaitingConsent -> onAwaitConsent()
        }
    }
    RegisterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onSignIn = onSignIn,
        onBack = onBack,
        modifier = modifier,
        phoneState = phoneState,
        onPhoneEvent = phoneViewModel::onEvent,
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    phoneState: PhoneSignInUiState = PhoneSignInUiState(),
    onPhoneEvent: (PhoneSignInUiEvent) -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    val codeStep = uiState.codeStep
    AuthFormCard(
        title = stringResource(if (codeStep != null) R.string.register_code_title else R.string.register_title),
        subtitle =
            stringResource(
                if (codeStep !=
                    null
                ) {
                    R.string.register_code_subtitle
                } else {
                    R.string.register_subtitle
                },
            ),
        onBack = if (codeStep != null) ({ onEvent(RegisterUiEvent.ChangeEmail) }) else onBack,
        modifier = modifier,
    ) {
        uiState.joinNotice?.let { JoinNoticeBox(it, onDrop = { onEvent(RegisterUiEvent.DropJoinCode) }) }
        // Nomor HP first while WhatsApp codes are available, as on the web.
        var phoneTab by rememberSaveable { mutableStateOf(true) }
        val showPhone = phoneState.isAvailable && phoneTab && codeStep == null
        if (phoneState.isAvailable &&
            codeStep == null
        ) {
            AuthMethodTabs(phoneSelected = phoneTab, onSelectPhone = { phoneTab = it })
        }
        when {
            codeStep != null -> {
                RegisterCodeForm(step = codeStep, isSubmitting = uiState.isSubmitting, onEvent = onEvent)
            }

            showPhone -> {
                PhoneCodeForm(
                    uiState = phoneState,
                    onEvent = onPhoneEvent,
                    submitText = stringResource(R.string.phone_submit_register),
                )
            }

            else -> {
                RegisterFields(uiState = uiState, onEvent = onEvent)
            }
        }
        if (codeStep == null) {
            RegisterActions(
                showSubmit = !showPhone,
                showGoogle = uiState.showGoogle,
                isSubmitting = uiState.isSubmitting,
                onEvent = onEvent,
                onSignIn = onSignIn,
            )
        }
        TermsNotice()
        AuthCopyright()
    }
}

@Composable
private fun RegisterActions(
    showSubmit: Boolean,
    showGoogle: Boolean,
    isSubmitting: Boolean,
    onEvent: (RegisterUiEvent) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        if (showSubmit) {
            GotestButton(
                text = stringResource(if (isSubmitting) R.string.auth_processing else R.string.register_submit),
                onClick = { onEvent(RegisterUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            )
        }
        if (showGoogle) {
            GotestButton(
                text = stringResource(R.string.register_google),
                onClick = { onEvent(RegisterUiEvent.GoogleSignUp) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = !isSubmitting,
            )
        }
        AuthSwitchPrompt(
            prompt = stringResource(R.string.register_has_account),
            linkText = stringResource(R.string.register_login_link),
            onLinkClick = onSignIn,
        )
    }
}

@Composable
private fun RegisterFields(
    uiState: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = uiState.messages
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val nameError = messages["name"] ?: uiState.fullNameError?.let { stringResource(it) }
        GotestTextField(
            value = uiState.fullName,
            onValueChange = { onEvent(RegisterUiEvent.FullNameChanged(it)) },
            label = stringResource(R.string.register_name_label),
            placeholder = stringResource(R.string.register_name_hint),
            leadingIcon = GotestIcons.Linear.User,
            supportingText = nameError,
            isError = nameError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        val emailError = messages["email"] ?: uiState.emailError?.let { stringResource(it) }
        GotestTextField(
            value = uiState.email,
            onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.register_email_label),
            placeholder = stringResource(R.string.register_email_hint),
            leadingIcon = GotestIcons.Linear.DirectNotification,
            supportingText = emailError,
            isError = emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        val passwordError = messages["password"] ?: uiState.passwordError?.let { stringResource(it) }
        GotestPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.register_password_label),
            placeholder = stringResource(R.string.register_password_hint),
            supportingText = passwordError,
            isError = passwordError != null,
        )
        GotestPasswordField(
            value = uiState.confirmation,
            onValueChange = { onEvent(RegisterUiEvent.ConfirmationChanged(it)) },
            label = stringResource(R.string.register_confirm_label),
            placeholder = stringResource(R.string.register_confirm_hint),
            supportingText = uiState.confirmationError?.let { stringResource(it) },
            isError = uiState.confirmationError != null,
        )
        EducationLevelPicker(
            selected = uiState.level,
            onSelect = { onEvent(RegisterUiEvent.LevelPicked(it)) },
            error = messages["education_level"] ?: uiState.levelError?.let { stringResource(it) },
        )
        val question = uiState.guardianQuestion
        if (question != null && uiState.asksGuardian) {
            GuardianFields(question = question, state = uiState.guardian, onChange = {
                onEvent(RegisterUiEvent.GuardianChanged(it))
            })
        }
        if (uiState.emailCode) NoteText(stringResource(R.string.register_email_code_note))
        messages["form"]?.let { ErrorNote(it) }
    }
}

/** "Cek email kamu": the code from the email makes the account (web: Components/Auth/EmailCodeForm.tsx). */
@Composable
private fun RegisterCodeForm(
    step: RegisterCodeStep,
    isSubmitting: Boolean,
    onEvent: (RegisterUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        NoteText(stringResource(R.string.register_code_sent, step.sentTo))
        GotestTextField(
            value = step.code,
            onValueChange = { onEvent(RegisterUiEvent.CodeChanged(it)) },
            label = stringResource(R.string.register_code_label),
            placeholder = stringResource(R.string.code_hint),
            supportingText = step.codeMessage,
            isError = step.codeMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(RegisterUiEvent.ConfirmCode) }),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(if (isSubmitting) R.string.auth_processing else R.string.register_code_submit),
                onClick = { onEvent(RegisterUiEvent.ConfirmCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && step.code.length == CODE_LENGTH,
            )
            GotestButton(
                text =
                    if (step.resendInSeconds > 0) {
                        stringResource(R.string.code_resend_in, step.resendInSeconds)
                    } else {
                        stringResource(R.string.code_resend)
                    },
                onClick = { onEvent(RegisterUiEvent.ResendCode) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Text,
                enabled = step.resendInSeconds == 0 && !isSubmitting,
            )
            GotestButton(
                text = stringResource(R.string.register_code_change_email),
                onClick = { onEvent(RegisterUiEvent.ChangeEmail) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Text,
            )
        }
    }
}

/** "Kamu akan bergabung ke …" when the sign-up came through a Mitra code (Aturan 2.18). */
@Composable
private fun JoinNoticeBox(
    notice: JoinNotice,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = GotestTheme.colors.bgSubtle,
        contentPadding = PaddingValues(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm),
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        NoteText(
            if (notice.isClass) {
                stringResource(R.string.register_join_class, notice.label, notice.organization)
            } else {
                stringResource(R.string.register_join_event, notice.organization, notice.label)
            },
        )
        GotestButton(
            text = stringResource(R.string.register_join_drop),
            onClick = onDrop,
            variant = GotestButtonVariant.Text,
        )
    }
}

@Composable
private fun NoteText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.textSecondary,
    )
}

@Composable
private fun ErrorNote(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.statusDanger,
    )
}

private const val CODE_LENGTH = 6

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

private val PreviewQuestion =
    GuardianQuestion(
        age = 18,
        levels = setOf(EducationLevel.Sd, EducationLevel.Smp, EducationLevel.SmaSmk),
        channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email),
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenPreview() {
    GotestTheme {
        RegisterScreen(
            uiState = RegisterUiState(emailCode = true, showGoogle = true),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
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
                    email = "gilangpermana@mail.com",
                    messages = mapOf("email" to "Email ini sudah terdaftar. Masuk dengan email kamu."),
                    confirmationError = R.string.auth_error_password_mismatch,
                    levelError = R.string.education_required,
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 1400)
@Composable
private fun RegisterScreenGuardianPreview() {
    GotestTheme {
        RegisterScreen(
            uiState =
                RegisterUiState(
                    fullName = "Raka Pratama",
                    email = "raka@contoh.id",
                    level = EducationLevel.Smp,
                    guardianQuestion = PreviewQuestion,
                    guardian =
                        GuardianFormState(
                            adult = false,
                            channel = GuardianChannel.WhatsApp,
                            contactErrorRes = R.string.guardian_phone_required,
                        ),
                    emailCode = true,
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenJoinPreview() {
    GotestTheme {
        RegisterScreen(
            uiState =
                RegisterUiState(
                    joinNotice = JoinNotice(isClass = true, organization = "SMA Harapan", label = "XII IPA 1"),
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenCodePreview() {
    GotestTheme {
        RegisterScreen(
            uiState =
                RegisterUiState(
                    codeStep = RegisterCodeStep(sentTo = "r***@contoh.id", code = "12", resendInSeconds = 42),
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun RegisterScreenWrongCodePreview() {
    GotestTheme {
        RegisterScreen(
            uiState =
                RegisterUiState(
                    codeStep =
                        RegisterCodeStep(
                            sentTo = "r***@contoh.id",
                            codeMessage = "Kode salah. Sisa 4 kali percobaan.",
                        ),
                ),
            onEvent = {},
            onSignIn = {},
            onBack = {},
        )
    }
}
