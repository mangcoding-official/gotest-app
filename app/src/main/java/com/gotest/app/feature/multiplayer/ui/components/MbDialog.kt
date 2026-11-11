package com.gotest.app.feature.multiplayer.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Colour of the round status badge used by Main Bareng dialogs. */
enum class MbBadgeTone { Yellow, Blue, Red, Green }

private data class BadgeColors(
    val container: Color,
    val ring: Color,
    val icon: Color,
)

private fun GotestColors.badgeColors(tone: MbBadgeTone): BadgeColors =
    when (tone) {
        MbBadgeTone.Yellow -> BadgeColors(bgYellow, illustrationYellowSoft, illustrationYellowSoft)
        MbBadgeTone.Blue -> BadgeColors(bgBlue, illustrationBlueSoft, bgBlueSoft)
        MbBadgeTone.Red -> BadgeColors(statusDanger, dangerRing, statusDangerSoft)
        MbBadgeTone.Green -> BadgeColors(bgGreen, illustrationGreenSoft, illustrationGreenSoft)
    }

/** 72dp solid circle with a soft 8dp halo and a 36dp glyph. */
@Composable
fun MbStatusBadge(
    @DrawableRes icon: Int,
    tone: MbBadgeTone,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors.badgeColors(tone)
    val shape = GotestTheme.shapes.full
    Box(
        modifier =
            modifier
                .size(MultiplayerDimens.statusBadge)
                .outerRing(colors.ring, GotestTheme.spacing.sm, shape)
                .background(colors.container, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(MultiplayerDimens.statusBadgeIcon),
            tint = colors.icon,
        )
    }
}

/** Centred Main Bareng dialog: status badge, title, message and one or two medium buttons. */
@Composable
fun MbDialog(
    @DrawableRes icon: Int,
    tone: MbBadgeTone,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        MbDialogContent(
            icon = icon,
            tone = tone,
            title = title,
            message = message,
            confirmText = confirmText,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
            dismissText = dismissText,
        )
    }
}

/** The dialog card without its window, for previews. */
@Composable
fun MbDialogContent(
    @DrawableRes icon: Int,
    tone: MbBadgeTone,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MbStatusBadge(icon = icon, tone = tone)
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                if (dismissText != null) {
                    GotestButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = GotestButtonVariant.Plain,
                        size = GotestButtonSize.Medium,
                    )
                }
                GotestButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    size = GotestButtonSize.Medium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MbDialogContentPreview() {
    GotestTheme {
        MbDialogContent(
            icon = GotestIcons.Bold.Award,
            tone = MbBadgeTone.Yellow,
            title = "Buat permainan khusus Premium",
            message = "Akun gratis tetap bisa ikut main bareng lewat kode dari temanmu.",
            confirmText = "Tingkatkan Akun",
            onConfirm = {},
            onDismiss = {},
            dismissText = "Gabung dengan Kode",
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}
