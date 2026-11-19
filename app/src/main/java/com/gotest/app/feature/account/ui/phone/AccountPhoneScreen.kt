package com.gotest.app.feature.account.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import kotlinx.coroutines.launch

@Composable
fun AccountPhoneRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountPhoneViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is AccountPhoneEffect.ShowMessage -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.message ?: resources.getString(R.string.state_error_message))
                }
            }

            AccountPhoneEffect.Close -> {
                onBack()
            }
        }
    }
    AccountPhoneScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun AccountPhoneScreen(
    uiState: AccountPhoneUiState,
    onEvent: (AccountPhoneUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = GotestTheme.spacing
    AccountDetailScaffold(
        title = stringResource(R.string.account_phone_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            uiState.loadFailed -> {
                GotestErrorView(
                    onRetry = { onEvent(AccountPhoneUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
                ) {
                    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
                        // The web's lede, in the card: the app bar keeps one line only.
                        Text(
                            text = stringResource(R.string.account_phone_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GotestTheme.colors.textSecondary,
                        )
                        when (uiState.step) {
                            PhoneStep.View -> CurrentNumber(uiState = uiState, onEvent = onEvent, onClose = onBack)
                            PhoneStep.Phone -> NumberForm(uiState = uiState, onEvent = onEvent, onClose = onBack)
                            PhoneStep.Code -> CodeForm(uiState = uiState, onEvent = onEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentNumber(
    uiState: AccountPhoneUiState,
    onEvent: (AccountPhoneUiEvent) -> Unit,
    onClose: () -> Unit,
) {
    GotestTextField(value = uiState.current.orEmpty(), onValueChange = {}, readOnly = true)
    Actions {
        if (uiState.canAdd) {
            GotestButton(
                text = stringResource(R.string.account_phone_change),
                onClick = { onEvent(AccountPhoneUiEvent.ChangeNumber) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (uiState.canRemove) {
            GotestButton(
                text = stringResource(R.string.account_phone_remove),
                onClick = { onEvent(AccountPhoneUiEvent.Remove) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = !uiState.isSubmitting,
                contentColor = GotestTheme.colors.statusDanger,
            )
        }
        GotestButton(
            text = stringResource(R.string.account_phone_close),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Text,
        )
    }
}

@Composable
private fun NumberForm(
    uiState: AccountPhoneUiState,
    onEvent: (AccountPhoneUiEvent) -> Unit,
    onClose: () -> Unit,
) {
    GotestTextField(
        value = uiState.phone,
        onValueChange = { onEvent(AccountPhoneUiEvent.PhoneChanged(it)) },
        label = stringResource(R.string.account_phone_label),
        placeholder = stringResource(R.string.account_phone_hint),
        supportingText = uiState.phoneMessage,
        isError = uiState.phoneMessage != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onEvent(AccountPhoneUiEvent.SendCode) }),
    )
    Actions {
        GotestButton(
            text = stringResource(R.string.account_phone_send),
            onClick = { onEvent(AccountPhoneUiEvent.SendCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSend,
        )
        // With a saved number "Kembali" returns to it; without one the screen closes, as the web modal does.
        GotestButton(
            text =
                stringResource(
                    if (uiState.current !=
                        null
                    ) {
                        R.string.account_phone_back
                    } else {
                        R.string.account_phone_close
                    },
                ),
            onClick = if (uiState.current != null) ({ onEvent(AccountPhoneUiEvent.BackToCurrent) }) else onClose,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun CodeForm(
    uiState: AccountPhoneUiState,
    onEvent: (AccountPhoneUiEvent) -> Unit,
) {
    GotestTextField(
        value = uiState.code,
        onValueChange = { onEvent(AccountPhoneUiEvent.CodeChanged(it)) },
        label = stringResource(R.string.account_phone_code_label),
        placeholder = stringResource(R.string.account_phone_code_hint),
        supportingText = uiState.codeMessage ?: stringResource(R.string.account_phone_code_sent, uiState.sentTo),
        isError = uiState.codeMessage != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(AccountPhoneUiEvent.Save) }),
    )
    Actions {
        GotestButton(
            text = stringResource(R.string.account_phone_save),
            onClick = { onEvent(AccountPhoneUiEvent.Save) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSave,
        )
        GotestButton(
            text = stringResource(R.string.account_phone_change),
            onClick = { onEvent(AccountPhoneUiEvent.ChangeNumber) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun Actions(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) { content() }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 700)
@Composable
private fun AccountPhoneLoadingPreview() {
    GotestTheme { AccountPhoneScreen(AccountPhoneUiState(), onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 700)
@Composable
private fun AccountPhoneErrorPreview() {
    GotestTheme {
        AccountPhoneScreen(
            AccountPhoneUiState(isLoading = false, loadFailed = true),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 700)
@Composable
private fun AccountPhoneCurrentPreview() {
    GotestTheme {
        AccountPhoneScreen(
            AccountPhoneUiState(
                isLoading = false,
                current = "0812-3456-7890",
                canAdd = true,
                canRemove = true,
                step = PhoneStep.View,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 700)
@Composable
private fun AccountPhoneAddPreview() {
    GotestTheme {
        AccountPhoneScreen(
            AccountPhoneUiState(
                isLoading = false,
                canAdd = true,
                phone = "0812 1111 2222",
                phoneMessage = "Nomor ini sudah dipakai akun lain.",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 700)
@Composable
private fun AccountPhoneCodePreview() {
    GotestTheme {
        AccountPhoneScreen(
            AccountPhoneUiState(
                isLoading = false,
                canAdd = true,
                step = PhoneStep.Code,
                phone = "0812 9999 0000",
                sentTo = "0812-9999-0000",
                code = "111111",
                codeMessage = "Kode salah. Sisa 4 kali percobaan.",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
