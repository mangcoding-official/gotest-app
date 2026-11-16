package com.gotest.app.feature.auth.ui.childlogin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.domain.FamilyChild
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard

@Composable
fun ChildLoginRoute(
    onSignInSuccess: () -> Unit,
    onOtherSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChildLoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.signInSuccess) { onSignInSuccess() }
    ChildLoginScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOtherSignIn = onOtherSignIn,
        onBack = onBack,
        modifier = modifier,
    )
}

/** "Masuk dengan kode keluarga" (Aturan 11.25–11.29): code, then name, then PIN. */
@Composable
fun ChildLoginScreen(
    uiState: ChildLoginUiState,
    onEvent: (ChildLoginUiEvent) -> Unit,
    onOtherSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle =
        when (uiState.step) {
            ChildLoginStep.Code -> stringResource(R.string.child_subtitle_code)
            ChildLoginStep.Name -> stringResource(R.string.child_subtitle_name)
            ChildLoginStep.Pin -> stringResource(R.string.child_subtitle_pin, uiState.child?.name.orEmpty())
        }
    val error = uiState.errorMessage ?: uiState.errorRes?.let { stringResource(it) }
    AuthFormCard(
        title = stringResource(R.string.child_title),
        subtitle = subtitle,
        onBack = onBack,
        modifier = modifier,
    ) {
        when (uiState.step) {
            ChildLoginStep.Code -> {
                CodeStep(
                    uiState = uiState,
                    error = error,
                    onEvent = onEvent,
                    onOtherSignIn = onOtherSignIn,
                )
            }

            ChildLoginStep.Name -> {
                NameStep(children = uiState.children, onEvent = onEvent)
            }

            ChildLoginStep.Pin -> {
                PinStep(uiState = uiState, error = error, onEvent = onEvent)
            }
        }
        AuthCopyright()
    }
}

@Composable
private fun CodeStep(
    uiState: ChildLoginUiState,
    error: String?,
    onEvent: (ChildLoginUiEvent) -> Unit,
    onOtherSignIn: () -> Unit,
) {
    GotestTextField(
        value = uiState.code,
        onValueChange = { onEvent(ChildLoginUiEvent.CodeChanged(it)) },
        label = stringResource(R.string.child_code_label),
        placeholder = stringResource(R.string.child_code_hint),
        supportingText = error,
        isError = error != null,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
        keyboardActions = KeyboardActions(onGo = { onEvent(ChildLoginUiEvent.SubmitCode) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.child_next),
            onClick = { onEvent(ChildLoginUiEvent.SubmitCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        )
        GotestButton(
            text = stringResource(R.string.child_other_sign_in),
            onClick = onOtherSignIn,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun NameStep(
    children: List<FamilyChild>,
    onEvent: (ChildLoginUiEvent) -> Unit,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        children.forEach { child ->
            Surface(
                onClick = { onEvent(ChildLoginUiEvent.ChildPicked(child)) },
                modifier = Modifier.fillMaxWidth(),
                shape = GotestTheme.shapes.largeIncreased,
                color = colors.bgApp,
                border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
            ) {
                Row(
                    modifier = Modifier.padding(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    GotestTheme.sizes.iconXl,
                                ).background(colors.bgBrandSoft, GotestTheme.shapes.full),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = child.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textBrandDark,
                        )
                    }
                    Text(text = child.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                }
            }
        }
        GotestButton(
            text = stringResource(R.string.child_change_code),
            onClick = { onEvent(ChildLoginUiEvent.ChangeCode) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun PinStep(
    uiState: ChildLoginUiState,
    error: String?,
    onEvent: (ChildLoginUiEvent) -> Unit,
) {
    GotestTextField(
        value = uiState.pin,
        onValueChange = { onEvent(ChildLoginUiEvent.PinChanged(it)) },
        label = stringResource(R.string.child_pin_label),
        placeholder = stringResource(R.string.child_pin_hint),
        supportingText = error,
        isError = error != null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(ChildLoginUiEvent.SubmitPin) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_processing else R.string.child_submit),
            onClick = { onEvent(ChildLoginUiEvent.SubmitPin) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        )
        GotestButton(
            text = stringResource(R.string.child_not_me, uiState.child?.name.orEmpty()),
            onClick = { onEvent(ChildLoginUiEvent.NotMe) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

private val previewChildren = listOf(FamilyChild("child_raka", "Raka"), FamilyChild("child_sinta", "Sinta"))

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChildLoginCodePreview() {
    GotestTheme { ChildLoginScreen(ChildLoginUiState(code = "KLG12"), onEvent = {}, onOtherSignIn = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChildLoginCodeErrorPreview() {
    GotestTheme {
        ChildLoginScreen(
            ChildLoginUiState(code = "ABC999", errorMessage = "Kode keluarga tidak ditemukan. Periksa lagi kodenya."),
            onEvent = {},
            onOtherSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChildLoginNamePreview() {
    GotestTheme {
        ChildLoginScreen(
            ChildLoginUiState(step = ChildLoginStep.Name, code = "KLG123", children = previewChildren),
            onEvent = {},
            onOtherSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChildLoginPinLockedPreview() {
    GotestTheme {
        ChildLoginScreen(
            ChildLoginUiState(
                step = ChildLoginStep.Pin,
                code = "KLG123",
                children = previewChildren,
                child = previewChildren.first(),
                errorMessage =
                    "PIN terkunci karena terlalu banyak yang salah. Coba lagi dalam 15 menit, " +
                        "atau minta orang tua atau gurumu mengatur ulang PIN.",
            ),
            onEvent = {},
            onOtherSignIn = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChildLoginPinLoadingPreview() {
    GotestTheme {
        ChildLoginScreen(
            ChildLoginUiState(
                step = ChildLoginStep.Pin,
                code = "KLG123",
                child = previewChildren.first(),
                pin = "2468",
                isLoading = true,
            ),
            onEvent = {},
            onOtherSignIn = {},
            onBack = {},
        )
    }
}
