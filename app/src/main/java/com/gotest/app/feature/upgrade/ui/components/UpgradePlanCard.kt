package com.gotest.app.feature.upgrade.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.ui.GotestFormat

/**
 * Selectable plan card ("plan/premium", "plan/free"). The selected card gets the 2dp brand
 * border; the plan the student is on shows the grey "Paket kamu saat ini" chip instead of its badge.
 */
@Composable
internal fun UpgradePlanCard(
    plan: Plan,
    selected: Boolean,
    isCurrentPlan: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val isPremium = plan.tier == PlanTier.Premium
    val border =
        if (selected) {
            BorderStroke(UpgradeDimens.selectedPlanBorder, colors.bgBrand)
        } else {
            BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
        }
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.RadioButton },
        shape = if (isPremium) GotestTheme.shapes.largeIncreased else MaterialTheme.shapes.large,
        color = colors.bgApp,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            PlanHeader(plan = plan, isPremium = isPremium, isCurrentPlan = isCurrentPlan)
            PlanPrice(plan = plan)
            Text(
                text = plan.description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            val tickColor = if (isPremium) colors.bgYellow else colors.statusSuccess
            plan.benefits.forEach { benefit -> PlanBenefit(text = benefit, tickColor = tickColor) }
        }
    }
}

@Composable
private fun PlanHeader(
    plan: Plan,
    isPremium: Boolean,
    isCurrentPlan: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPremium) {
            Icon(
                painter = painterResource(GotestIcons.Bold.Award),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = GotestTheme.colors.bgYellow,
            )
        }
        Text(
            text = plan.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        when {
            isCurrentPlan -> {
                GotestChip(text = stringResource(R.string.upgrade_plan_current), tone = GotestChipTone.Grey)
            }

            plan.badge != null -> {
                GotestChip(text = plan.badge, tone = GotestChipTone.Yellow)
            }
        }
    }
}

@Composable
private fun PlanPrice(
    plan: Plan,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    if (plan.priceRupiah == 0L) {
        Text(
            text = stringResource(R.string.upgrade_plan_free_price),
            modifier = modifier,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = GotestFormat.rupiah(plan.priceRupiah),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
            )
            plan.period?.let { period ->
                Text(
                    text = stringResource(R.string.upgrade_plan_per_period, period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun PlanBenefit(
    text: String,
    tickColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Bold.TickCircle),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = tickColor,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradePlanCardPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            UpgradePlanCard(plan = UpgradePreviewData.premiumPlan, selected = true, isCurrentPlan = false, onClick = {})
            UpgradePlanCard(plan = UpgradePreviewData.freePlan, selected = false, isCurrentPlan = true, onClick = {})
        }
    }
}
