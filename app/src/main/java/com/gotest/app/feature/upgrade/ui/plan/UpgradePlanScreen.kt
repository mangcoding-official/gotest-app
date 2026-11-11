package com.gotest.app.feature.upgrade.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PlanTier
import com.gotest.app.feature.upgrade.ui.components.UpgradeBottomBar
import com.gotest.app.feature.upgrade.ui.components.UpgradeCaption
import com.gotest.app.feature.upgrade.ui.components.UpgradePlanCard
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData

@Composable
fun UpgradePlanRoute(
    onClose: () -> Unit,
    onChoosePlan: (planId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpgradePlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UpgradePlanScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onChoosePlan = onChoosePlan,
        modifier = modifier,
    )
}

/** "Upgrade / Pilih Paket" (frame 11349). */
@Composable
fun UpgradePlanScreen(
    uiState: UpgradePlanUiState,
    onEvent: (UpgradePlanUiEvent) -> Unit,
    onClose: () -> Unit,
    onChoosePlan: (planId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.upgrade_plan_title),
                navigationIcon = {
                    GotestIconButton(
                        icon = GotestIcons.Linear.CloseCircle,
                        contentDescription = stringResource(R.string.upgrade_plan_close),
                        onClick = onClose,
                    )
                },
            )
        },
        bottomBar = {
            if (uiState is UpgradePlanUiState.Success) {
                PlanBottomBar(state = uiState, onChoosePlan = onChoosePlan)
            }
        },
        containerColor = GotestTheme.colors.bgApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState) {
            UpgradePlanUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            UpgradePlanUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(UpgradePlanUiEvent.Retry) }, modifier = contentModifier)
            }

            UpgradePlanUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.Award,
                    title = stringResource(R.string.upgrade_plan_empty_title),
                    message = stringResource(R.string.upgrade_plan_empty_message),
                    modifier = contentModifier,
                )
            }

            is UpgradePlanUiState.Success -> {
                PlanContent(state = uiState, onEvent = onEvent, modifier = contentModifier)
            }
        }
    }
}

@Composable
private fun PlanContent(
    state: UpgradePlanUiState.Success,
    onEvent: (UpgradePlanUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    top = spacing.sm,
                    end = spacing.screenHorizontal,
                    bottom = spacing.lg,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.upgrade_plan_headline).uppercase(),
                modifier = Modifier.semantics { heading() },
                style = GotestTheme.typography.displayExtraSmall,
                color = GotestTheme.colors.textBrandDark,
            )
            Text(
                text = stringResource(R.string.upgrade_plan_description),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            state.plans.forEach { plan ->
                UpgradePlanCard(
                    plan = plan,
                    selected = plan.id == state.selectedPlanId,
                    isCurrentPlan = plan.tier == state.currentTier,
                    onClick = { onEvent(UpgradePlanUiEvent.SelectPlan(plan.id)) },
                )
            }
        }
    }
}

@Composable
private fun PlanBottomBar(
    state: UpgradePlanUiState.Success,
    onChoosePlan: (planId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when {
            state.isPremium -> R.string.upgrade_plan_already_premium
            state.selectedPlan.tier == PlanTier.FreeTrial -> R.string.upgrade_plan_current
            else -> R.string.upgrade_plan_choose_yearly
        }
    UpgradeBottomBar(modifier = modifier) {
        GotestButton(
            text = stringResource(label),
            onClick = { onChoosePlan(state.selectedPlanId) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canCheckout,
        )
        UpgradeCaption(text = stringResource(R.string.upgrade_plan_cancel_note))
    }
}

private val previewSuccess =
    UpgradePlanUiState.Success(
        plans = listOf(UpgradePreviewData.premiumPlan, UpgradePreviewData.freePlan),
        selectedPlanId = UpgradePreviewData.premiumPlan.id,
        currentTier = PlanTier.FreeTrial,
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun UpgradePlanScreenPreview() {
    GotestTheme {
        UpgradePlanScreen(uiState = previewSuccess, onEvent = {}, onClose = {}, onChoosePlan = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun UpgradePlanScreenPremiumPreview() {
    GotestTheme {
        UpgradePlanScreen(
            uiState = previewSuccess.copy(currentTier = PlanTier.Premium),
            onEvent = {},
            onClose = {},
            onChoosePlan = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun UpgradePlanScreenLoadingPreview() {
    GotestTheme {
        UpgradePlanScreen(uiState = UpgradePlanUiState.Loading, onEvent = {}, onClose = {}, onChoosePlan = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun UpgradePlanScreenEmptyPreview() {
    GotestTheme {
        UpgradePlanScreen(uiState = UpgradePlanUiState.Empty, onEvent = {}, onClose = {}, onChoosePlan = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun UpgradePlanScreenErrorPreview() {
    GotestTheme {
        UpgradePlanScreen(uiState = UpgradePlanUiState.Error, onEvent = {}, onClose = {}, onChoosePlan = {})
    }
}
