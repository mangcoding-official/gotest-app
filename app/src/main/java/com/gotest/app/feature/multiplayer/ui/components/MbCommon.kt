package com.gotest.app.feature.multiplayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Tone of [MbDotChip]. */
enum class DotChipTone { Yellow, Green }

/** Status chip with a leading dot ("Menunggu pemain", "Selesai"). */
@Composable
fun MbDotChip(
    text: String,
    tone: DotChipTone,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val (container, content) =
        when (tone) {
            DotChipTone.Yellow -> colors.illustrationYellowSoft to colors.illustrationYellowDark
            DotChipTone.Green -> colors.illustrationGreenSoft to colors.illustrationGreenDark
        }
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .background(container, GotestTheme.shapes.full)
                .padding(horizontal = spacing.md, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(MultiplayerDimens.statusDot)
                    .background(content, GotestTheme.shapes.full),
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/** Solid yellow chip with a halo: "Juara 1" and "Pemenang". */
@Composable
fun MbAwardChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = GotestTheme.shapes.full
    Row(
        modifier =
            modifier
                .outerRing(colors.illustrationYellowSoft, GotestTheme.sizes.focusRingWidth, shape)
                .background(colors.bgYellow, shape)
                .padding(horizontal = spacing.md, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Bold.Award),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = colors.bgApp,
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = colors.bgApp)
    }
}

/** Label over value tile ("Jumlah soal / 20 soal"); [filled] grey, otherwise outlined. */
@Composable
fun MbInfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            modifier
                .then(
                    if (filled) {
                        Modifier.background(colors.bgSubtle, shape)
                    } else {
                        Modifier.border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape)
                    },
                ).padding(GotestTheme.spacing.md)
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Big number with a caption on a tinted tile (Benar / Salah / Kosong, Dijawab / Kosong). */
@Composable
fun MbStatTile(
    value: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
) {
    val tileModifier =
        modifier
            .background(container, if (horizontal) MaterialTheme.shapes.medium else MaterialTheme.shapes.small)
            .padding(GotestTheme.spacing.md)
            .semantics(mergeDescendants = true) {}
    if (horizontal) {
        Row(
            modifier = tileModifier,
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = content)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = content)
        }
    } else {
        Column(modifier = tileModifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = content)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = content)
        }
    }
}

/** Rounded square holding a feature icon (hub cards, join sheet, finish sheet). */
@Composable
fun MbIconTile(
    @DrawableRes icon: Int,
    container: Color,
    tint: Color,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(container, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}

/** Small swatch and caption of a legend. */
@Composable
fun MbLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    swatchSize: Dp = GotestTheme.sizes.badgeDot,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs + GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(swatchSize)
                    .background(color, MaterialTheme.shapes.extraSmall),
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

/** White bar pinned to the bottom with a hairline on top; pads for the navigation bar. */
@Composable
fun MbBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().background(GotestTheme.colors.bgApp)) {
        GotestDivider()
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            content = content,
        )
    }
}

/** Preview only: dims [screen] like a modal scrim and draws [overlay] on top. */
@Composable
internal fun MbPreviewOverlay(
    screen: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlayAlignment: Alignment = Alignment.Center,
    overlay: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        screen()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(GotestTheme.colors.scrim),
            contentAlignment = overlayAlignment,
            content = overlay,
        )
    }
}

/** Preview only: the bottom-sheet surface with its handle, as `GotestBottomSheet` draws it. */
@Composable
internal fun MbPreviewSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp, GotestTheme.shapes.bottomSheet),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = GotestTheme.spacing.sm, bottom = GotestTheme.spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(GotestTheme.sizes.sheetHandleWidth, GotestTheme.sizes.sheetHandleHeight)
                        .background(GotestTheme.colors.bgGrey, GotestTheme.shapes.full),
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun MbCommonPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbDotChip(text = "Menunggu pemain", tone = DotChipTone.Yellow)
                MbDotChip(text = "Selesai", tone = DotChipTone.Green)
                MbAwardChip(text = "Juara 1")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbInfoTile(label = "Jumlah soal", value = "20 soal", modifier = Modifier.weight(1f))
                MbInfoTile(label = "Durasi", value = "15 menit", modifier = Modifier.weight(1f), filled = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbStatTile(
                    value = "14",
                    label = "Benar",
                    container = GotestTheme.colors.bgGreenSoft,
                    content = GotestTheme.colors.illustrationGreenDark,
                    modifier = Modifier.weight(1f),
                )
                MbStatTile(
                    value = "17",
                    label = "Dijawab",
                    container = GotestTheme.colors.bgGreenSoft,
                    content = GotestTheme.colors.bgGreenDark,
                    modifier = Modifier.weight(1f),
                    horizontal = true,
                )
            }
            MbLegendItem(color = GotestTheme.colors.statusSuccess, label = "Benar")
            MbBottomBar {
                GotestButton(text = "Mulai Permainan", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
