package com.gotest.app.feature.auth.ui.phoneregister

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard
import com.gotest.app.feature.auth.ui.components.EducationLevelPicker
import com.gotest.app.feature.auth.ui.components.GuardianFields
import com.gotest.app.feature.auth.ui.components.GuardianFormState

@Composable
fun PhoneRegisterRoute(
    onRegisterSuccess: () -> Unit,
    onChangeNumber: () -> Unit,
    onAwaitConsent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhoneRegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            PhoneRegisterEffect.Registered -> onRegisterSuccess()
            PhoneRegisterEffect.AwaitingConsent -> onAwaitConsent()
        }
    }
    PhoneRegisterScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onChangeNumber = onChangeNumber,
        modifier = modifier,
    )
}

@Composable
fun PhoneRegisterScreen(
    uiState: PhoneRegisterUiState,
    onEvent: (PhoneRegisterUiEvent) -> Unit,
    onChangeNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFormCard(
        title = stringResource(R.string.phone_register_title),
        subtitle = stringResource(R.string.phone_register_subtitle, uiState.phoneDisplay),
        onBack = onChangeNumber,
        modifier = modifier,
    ) {
        val nameError = uiState.nameMessage ?: uiState.nameErrorRes?.let { stringResource(it) }
        GotestTextField(
            value = uiState.fullName,
            onValueChange = { onEvent(PhoneRegisterUiEvent.NameChanged(it)) },
            label = stringResource(R.string.phone_register_name_label),
            placeholder = stringResource(R.string.phone_register_name_hint),
            supportingText = nameError,
            isError = nameError != null,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
        )
        EducationLevelPicker(
            selected = uiState.level,
            onSelect = { onEvent(PhoneRegisterUiEvent.LevelPicked(it)) },
            error = uiState.levelErrorRes?.let { stringResource(it) },
        )
        val question = uiState.guardianQuestion
        if (question != null && uiState.asksGuardian) {
            GuardianFields(question = question, state = uiState.guardian, onChange = {
                onEvent(PhoneRegisterUiEvent.GuardianChanged(it))
            })
        }
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text =
                    stringResource(
                        if (uiState.isSubmitting) R.string.auth_processing else R.string.phone_register_submit,
                    ),
                onClick = { onEvent(PhoneRegisterUiEvent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
            )
            GotestButton(
                text = stringResource(R.string.phone_change),
                onClick = onChangeNumber,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
            )
        }
        AuthCopyright()
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhoneRegisterScreenPreview() {
    GotestTheme {
        PhoneRegisterScreen(PhoneRegisterUiState(phoneDisplay = "0812-9999-0000"), onEvent = {}, onChangeNumber = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhoneRegisterScreenErrorPreview() {
    GotestTheme {
        PhoneRegisterScreen(
            PhoneRegisterUiState(
                phoneDisplay = "0812-9999-0000",
                nameErrorRes = R.string.phone_register_name_required,
                levelErrorRes = R.string.education_required,
            ),
            onEvent = {},
            onChangeNumber = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhoneRegisterScreenExpiredPreview() {
    GotestTheme {
        PhoneRegisterScreen(
            PhoneRegisterUiState(
                phoneDisplay = "0812-9999-0000",
                fullName = "Budi Santoso",
                level = EducationLevel.SmaSmk,
                nameMessage = "Waktu pendaftaran habis. Minta kode baru dengan nomor HP kamu.",
            ),
            onEvent = {},
            onChangeNumber = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 1100)
@Composable
private fun PhoneRegisterScreenGuardianPreview() {
    GotestTheme {
        PhoneRegisterScreen(
            PhoneRegisterUiState(
                phoneDisplay = "0812-9999-0000",
                fullName = "Raka Pratama",
                level = EducationLevel.Smp,
                guardianQuestion =
                    GuardianQuestion(
                        age = 18,
                        levels = setOf(EducationLevel.Sd, EducationLevel.Smp, EducationLevel.SmaSmk),
                        channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email),
                    ),
                guardian =
                    GuardianFormState(
                        adult = false,
                        contact = "0812 9999 0000",
                        contactMessage = "Tulis nomor orang tua atau wali, bukan nomormu sendiri.",
                    ),
            ),
            onEvent = {},
            onChangeNumber = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhoneRegisterScreenSubmittingPreview() {
    GotestTheme {
        PhoneRegisterScreen(
            PhoneRegisterUiState(
                phoneDisplay = "0812-9999-0000",
                fullName = "Budi Santoso",
                level = EducationLevel.Smp,
                isSubmitting = true,
            ),
            onEvent = {},
            onChangeNumber = {},
        )
    }
}
