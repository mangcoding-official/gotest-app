package com.gotest.app.feature.account.ui.changepassword

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.domain.PasswordRule
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountTileSize
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordRoute(
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            ChangePasswordEffect.PasswordChanged -> {
                onChangeSuccess()
            }

            is ChangePasswordEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    ChangePasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ChangePasswordScreen(
    uiState: ChangePasswordUiState,
    onEvent: (ChangePasswordUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = GotestTheme.spacing
    AccountDetailScaffold(
        title = stringResource(R.string.account_password_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            AccountBottomActions {
                GotestButton(
                    text = stringResource(R.string.account_password_submit),
                    onClick = { onEvent(ChangePasswordUiEvent.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canSubmit && !uiState.isSubmitting,
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        ) {
            GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.lg + spacing.xs) {
                AccountIconTile(
                    icon = GotestIcons.Bold.Lock,
                    tint = GotestTheme.colors.iconBrand,
                    containerColor = GotestTheme.colors.bgBrandSoft,
                    size = AccountTileSize.Large,
                )
                Text(
                    text = stringResource(R.string.account_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GotestTheme.colors.textSecondary,
                )
                PasswordFields(uiState = uiState, onEvent = onEvent)
                PasswordChecklist(satisfiedRules = uiState.satisfiedRules)
            }
        }
    }
}

@Composable
private fun PasswordFields(
    uiState: ChangePasswordUiState,
    onEvent: (ChangePasswordUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
        GotestPasswordField(
            value = uiState.currentPassword,
            onValueChange = { onEvent(ChangePasswordUiEvent.CurrentPasswordChange(it)) },
            label = stringResource(R.string.account_password_current_label),
            placeholder = stringResource(R.string.account_password_current_hint),
        )
        GotestPasswordField(
            value = uiState.newPassword,
            onValueChange = { onEvent(ChangePasswordUiEvent.NewPasswordChange(it)) },
            label = stringResource(R.string.account_password_new_label),
            placeholder = stringResource(R.string.account_password_new_hint),
        )
        GotestPasswordField(
            value = uiState.confirmation,
            onValueChange = { onEvent(ChangePasswordUiEvent.ConfirmationChange(it)) },
            label = stringResource(R.string.account_password_confirm_label),
            placeholder = stringResource(R.string.account_password_confirm_hint),
            supportingText =
                if (uiState.confirmationMismatch) {
                    stringResource(
                        R.string.account_password_mismatch,
                    )
                } else {
                    null
                },
            isError = uiState.confirmationMismatch,
        )
    }
}

/** "syarat": green filled tick once a rule is met, grey outline otherwise. */
@Composable
private fun PasswordChecklist(
    satisfiedRules: Set<PasswordRule>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        PasswordRule.entries.forEach { rule ->
            PasswordRuleRow(textRes = rule.labelRes(), met = rule in satisfiedRules)
        }
    }
}

@Composable
private fun PasswordRuleRow(
    @StringRes textRes: Int,
    met: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (met) GotestIcons.Bold.TickCircle else GotestIcons.Linear.TickCircle),
            contentDescription =
                stringResource(if (met) R.string.account_password_rule_met else R.string.account_password_rule_unmet),
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = if (met) colors.statusSuccess else colors.iconMuted,
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (met) colors.bgGreenDark else colors.textSecondary,
        )
    }
}

@StringRes
private fun PasswordRule.labelRes(): Int =
    when (this) {
        PasswordRule.MinLength -> R.string.account_password_rule_length
        PasswordRule.LettersAndDigits -> R.string.account_password_rule_mix
        PasswordRule.DifferentFromCurrent -> R.string.account_password_rule_different
    }

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChangePasswordScreenEmptyPreview() {
    GotestTheme {
        ChangePasswordScreen(uiState = ChangePasswordUiState(), onEvent = {}, onBack = {})
    }
}

/** The design's state: two rules met, confirmation still empty. */
@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChangePasswordScreenPartialPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState =
                ChangePasswordUiState(
                    currentPassword = "",
                    newPassword = "gotest2026",
                    satisfiedRules = setOf(PasswordRule.MinLength, PasswordRule.LettersAndDigits),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChangePasswordScreenMismatchPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState =
                ChangePasswordUiState(
                    currentPassword = "rahasia12",
                    newPassword = "gotest2026",
                    confirmation = "gotest2025",
                    satisfiedRules = PasswordRule.entries.toSet(),
                    confirmationMismatch = true,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ChangePasswordScreenValidPreview() {
    GotestTheme {
        ChangePasswordScreen(
            uiState =
                ChangePasswordUiState(
                    currentPassword = "rahasia12",
                    newPassword = "gotest2026",
                    confirmation = "gotest2026",
                    satisfiedRules = PasswordRule.entries.toSet(),
                    canSubmit = true,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}
