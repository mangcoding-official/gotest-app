package com.gotest.app.feature.account.ui.mainlevel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.domain.MainLevel
import com.gotest.app.feature.account.ui.components.AccountBottomActions
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountHint
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountRadioIndicator
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.AccountTileSize
import kotlinx.coroutines.launch

@Composable
fun MainLevelRoute(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainLevelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            MainLevelEffect.Saved -> {
                onSaveSuccess()
            }

            is MainLevelEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    MainLevelScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** "Akun / Pengaturan – Jenjang Utama" (frame 23023). */
@Composable
fun MainLevelScreen(
    uiState: MainLevelUiState,
    onEvent: (MainLevelUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_main_level_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        bottomBar = {
            if (uiState is MainLevelUiState.Success) {
                AccountBottomActions {
                    GotestButton(
                        text = stringResource(R.string.account_main_level_save),
                        onClick = { onEvent(MainLevelUiEvent.Save) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.canSave,
                    )
                }
            }
        },
    ) { padding ->
        when (uiState) {
            MainLevelUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            MainLevelUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(MainLevelUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is MainLevelUiState.Success -> {
                MainLevelContent(
                    state = uiState,
                    onSelect = { onEvent(MainLevelUiEvent.Select(it)) },
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
private fun MainLevelContent(
    state: MainLevelUiState.Success,
    onSelect: (MainLevel) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AccountSectionTitle(
            title = stringResource(R.string.account_main_level_heading),
            subtitle = stringResource(R.string.account_main_level_description),
        )
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(spacing.sm),
            verticalSpacing = spacing.none,
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                MainLevel.entries.forEach { level ->
                    MainLevelOption(
                        level = level,
                        selected = level == state.selectedLevel,
                        onClick = { onSelect(level) },
                        enabled = !state.isSaving,
                    )
                }
            }
        }
        AccountHint(
            text = stringResource(R.string.account_main_level_info),
            icon = GotestIcons.Linear.InfoCircle,
        )
    }
}

/** "option": icon tile, title and subtitle, radio; the selected row sits on a grey tint. */
@Composable
private fun MainLevelOption(
    level: MainLevel,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) colors.bgSubtle else Color.Transparent)
                .border(GotestTheme.sizes.strokeThin, if (selected) colors.borderSubtle else Color.Transparent, shape)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountIconTile(
            icon = level.icon,
            tint = colors.iconDefault,
            containerColor = if (selected) colors.bgApp else colors.bgSubtle,
            size = AccountTileSize.Choice,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(level.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(level.subtitleRes),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        AccountRadioIndicator(selected = selected)
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MainLevelScreenPreview() {
    GotestTheme {
        MainLevelScreen(
            uiState = MainLevelUiState.Success(savedLevel = MainLevel.UtbkSnbt),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun MainLevelScreenChangedPreview() {
    GotestTheme {
        MainLevelScreen(
            uiState = MainLevelUiState.Success(savedLevel = MainLevel.UtbkSnbt, selectedLevel = MainLevel.Cpns),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MainLevelScreenLoadingPreview() {
    GotestTheme { MainLevelScreen(uiState = MainLevelUiState.Loading, onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MainLevelScreenErrorPreview() {
    GotestTheme { MainLevelScreen(uiState = MainLevelUiState.Error, onEvent = {}, onBack = {}) }
}
