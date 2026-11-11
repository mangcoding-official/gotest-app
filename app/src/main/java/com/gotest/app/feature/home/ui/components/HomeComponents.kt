package com.gotest.app.feature.home.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Sizes the home designs use that have no token. */
internal object HomeDimens {
    /** "Paket Untukmu" card; the next card peeks in from the edge. */
    val packageCardWidth = 268.dp

    /** Bar inside the "Lanjutkan" progress track. */
    val continueProgressHeight = 12.dp

    /** The "Belum ada Aktivitas" illustration. */
    val emptyActivityIllustrationWidth = 276.dp

    /** Pill behind the Main Bareng icon. */
    val multiplayerIconWidth = 56.dp
}

/**
 * The compact tab switcher inside Beranda's cards: a grey track holding small filled/text
 * buttons (the shared segmented control is the taller white variant).
 */
@Composable
internal fun HomeTabSwitcher(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.bgSubtle,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(GotestTheme.spacing.xs)
                    .selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                GotestButton(
                    text = option,
                    onClick = { onSelect(index) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { this.selected = selected },
                    variant = if (selected) GotestButtonVariant.Filled else GotestButtonVariant.Text,
                    size = GotestButtonSize.Small,
                )
            }
        }
    }
}

/** Card heading with a muted subtitle and a "Lihat Semua" text button. */
@Composable
internal fun HomeSectionHeader(
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        GotestButton(
            text = actionText,
            onClick = onAction,
            variant = GotestButtonVariant.Text,
            size = GotestButtonSize.Small,
        )
    }
}

/** 40dp tinted square holding a 20dp icon (stats, search rows, notifications). */
@Composable
internal fun HomeIconTile(
    @DrawableRes icon: Int,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    Box(
        modifier =
            modifier
                .size(GotestTheme.sizes.iconButton)
                .background(containerColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = iconColor,
        )
    }
}

/** The 4dp grey dot between metadata ("UTBK-SNBT • 20 Soal • 45 Menit"). */
@Composable
internal fun HomeMetaDot(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(GotestTheme.spacing.xs)
                .background(GotestTheme.colors.iconMuted, GotestTheme.shapes.full),
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeTabSwitcherPreview() {
    GotestTheme {
        HomeTabSwitcher(
            options = listOf("Paket Latihan", "Paket Tryout"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSectionHeaderPreview() {
    GotestTheme {
        HomeSectionHeader(
            title = "Paket Untukmu",
            subtitle = "Kerjakan latihan atau tryout di sini.",
            actionText = "Lihat Semua",
            onAction = {},
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeIconTilePreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIconTile(
                icon = GotestIcons.Bold.Award,
                containerColor = GotestTheme.colors.bgGreenSoft,
                iconColor = GotestTheme.colors.bgGreen,
            )
            HomeIconTile(
                icon = GotestIcons.Bold.Note2,
                containerColor = GotestTheme.colors.bgBlueSoft,
                iconColor = GotestTheme.colors.bgBlue,
            )
            HomeMetaDot()
        }
    }
}
