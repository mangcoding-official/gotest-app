package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Icon badge colours shared by dialogs, sheets and empty states. */
data class GotestIconBadgeColors(
    val container: Color,
    val content: Color,
)

/**
 * Centred confirmation dialog from the design (icon badge, title, message, up to two
 * buttons side by side). Omit [dismissText] for a single full-width button.
 */
@Composable
fun GotestDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    iconColors: GotestIconBadgeColors =
        GotestIconBadgeColors(GotestTheme.colors.bgBrandSoft, GotestTheme.colors.iconBrand),
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GotestDialogContent(
            title = title,
            message = message,
            confirmText = confirmText,
            onConfirm = onConfirm,
            modifier = modifier,
            icon = icon,
            iconColors = iconColors,
            dismissText = dismissText,
            onDismiss = onDismiss,
        )
    }
}

/** The dialog card without the window, so previews and in-screen overlays can reuse it. */
@Composable
fun GotestDialogContent(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    iconColors: GotestIconBadgeColors =
        GotestIconBadgeColors(GotestTheme.colors.bgBrandSoft, GotestTheme.colors.iconBrand),
    dismissText: String? = null,
    onDismiss: () -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.bgApp,
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon?.let { GotestIconBadge(icon = it, colors = iconColors) }
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
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
                    )
                }
                GotestButton(text = confirmText, onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Round icon badge (56dp, 28dp glyph) at the top of dialogs and sheets. */
@Composable
fun GotestIconBadge(
    @DrawableRes icon: Int,
    colors: GotestIconBadgeColors,
    modifier: Modifier = Modifier,
) {
    val sizes = GotestTheme.sizes
    Box(
        modifier =
            modifier
                .size(sizes.dialogIcon)
                .background(colors.container, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(sizes.iconXl),
            tint = colors.content,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestDialogPreview() {
    GotestTheme {
        GotestDialogContent(
            title = "Selesaikan latihan?",
            message = "Cek semua soal sebelum menyelesaikan latihan. Kamu sudah menjawab 4 dari 20 soal.",
            confirmText = "Selesaikan",
            onConfirm = {},
            icon = GotestIcons.Bold.TaskSquare,
            dismissText = "Batal",
            modifier = Modifier.padding(GotestTheme.spacing.xl),
        )
    }
}
