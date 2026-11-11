package com.gotest.app.feature.upgrade.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PaymentMethodType

/** Copy and icon of each payment method radio card. */
private data class PaymentMethodCopy(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
)

private fun PaymentMethodType.methodCopy(): PaymentMethodCopy =
    when (this) {
        PaymentMethodType.VirtualAccount -> {
            PaymentMethodCopy(
                R.string.upgrade_method_virtual_account,
                R.string.upgrade_method_virtual_account_description,
                GotestIcons.Linear.Bank,
            )
        }

        PaymentMethodType.Qris -> {
            PaymentMethodCopy(
                R.string.upgrade_method_qris,
                R.string.upgrade_method_qris_description,
                GotestIcons.Linear.Gallery,
            )
        }

        PaymentMethodType.EWallet -> {
            PaymentMethodCopy(
                R.string.upgrade_method_ewallet,
                R.string.upgrade_method_ewallet_description,
                GotestIcons.Linear.Directbox,
            )
        }
    }

/** "Metode Pembayaran": one radio card per [PaymentMethodType]. */
@Composable
internal fun PaymentMethodList(
    selected: PaymentMethodType,
    onSelect: (PaymentMethodType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        PaymentMethodType.entries.forEach { method ->
            PaymentMethodCard(
                method = method,
                selected = method == selected,
                onClick = { onSelect(method) },
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val copy = method.methodCopy()
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.RadioButton },
        shape = MaterialTheme.shapes.large,
        color =
            if (selected) {
                colors.bgBrandSoft.copy(alpha = UpgradeDimens.SELECTED_METHOD_CONTAINER_ALPHA)
            } else {
                colors.bgApp
            },
        border =
            if (selected) {
                BorderStroke(UpgradeDimens.selectedMethodBorder, colors.borderFocus)
            } else {
                BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
            },
    ) {
        Row(
            modifier = Modifier.padding(UpgradeDimens.methodCardPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(UpgradeDimens.methodIconBox)
                        .background(if (selected) colors.bgBrandSoft else colors.bgSubtle, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(copy.icon),
                    contentDescription = null,
                    modifier = Modifier.size(UpgradeDimens.methodIcon),
                    tint = if (selected) colors.iconBrand else colors.iconDefault,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(copy.title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(copy.description),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            RadioIndicator(selected = selected)
        }
    }
}

/** The design's 20dp radio: a thick dark ring when selected, a hairline circle otherwise. */
@Composable
internal fun RadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = GotestTheme.shapes.full
    Box(
        modifier =
            modifier
                .size(UpgradeDimens.radio)
                .background(colors.bgApp, shape)
                .border(
                    width = if (selected) UpgradeDimens.radioSelectedStroke else UpgradeDimens.radioStroke,
                    color = if (selected) colors.bgBrandDark else colors.borderSubtle,
                    shape = shape,
                ),
    )
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodListPreview() {
    GotestTheme {
        PaymentMethodList(
            selected = PaymentMethodType.VirtualAccount,
            onSelect = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}
