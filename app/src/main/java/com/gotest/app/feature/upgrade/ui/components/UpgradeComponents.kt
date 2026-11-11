package com.gotest.app.feature.upgrade.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * The white "bottom-bar" frame of the upgrade screens: optional hairline on top, the primary
 * action(s) and a caption, padded above the system navigation bar.
 */
@Composable
internal fun UpgradeBottomBar(
    modifier: Modifier = Modifier,
    showTopBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(GotestTheme.colors.bgApp)) {
        if (showTopBorder) GotestDivider()
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content,
        )
    }
}

/** Centred grey caption under a bottom-bar button. */
@Composable
internal fun UpgradeCaption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = GotestTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

/** Medium 14 heading above a card ("Metode Pembayaran", "Cara Membayar", …). */
@Composable
internal fun UpgradeSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = GotestTheme.colors.textPrimary,
    )
}

/** Grey label on the left, value on the right; the rows of the account and summary cards. */
@Composable
internal fun UpgradeInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * "GoTest Premium" row: coloured award tile, product name and period, plus an optional
 * trailing slot (the "Aktif" chip on the success screen).
 */
@Composable
internal fun UpgradeProductRow(
    title: String,
    subtitle: String,
    iconContainerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(UpgradeDimens.productIconBox)
                    .background(iconContainerColor, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.Award),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = iconTint,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradeComponentsPreview() {
    GotestTheme {
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
            Column(
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            ) {
                UpgradeSectionTitle(text = "Ringkasan Pesanan")
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    UpgradeProductRow(
                        title = stringResource(R.string.upgrade_product_premium),
                        subtitle = stringResource(R.string.upgrade_product_yearly_detail),
                        iconContainerColor = GotestTheme.colors.bgYellow,
                        iconTint = GotestTheme.colors.bgYellowSoft,
                        trailing = { GotestChip(text = "Aktif", tone = GotestChipTone.Green) },
                    )
                    UpgradeInfoRow(label = "Biaya Tambahan", value = "Rp10.000")
                }
            }
            Spacer(modifier = Modifier.size(GotestTheme.spacing.xl))
            UpgradeBottomBar {
                GotestButton(text = "Bayar Sekarang", onClick = {}, modifier = Modifier.fillMaxWidth())
                UpgradeCaption(text = "Bisa dibatalkan kapan saja sebelum perpanjangan.")
            }
        }
    }
}
