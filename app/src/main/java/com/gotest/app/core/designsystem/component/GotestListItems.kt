package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.theme.GotestTheme

private const val PERCENT = 100f

/**
 * The design's "Mobile/Paket Card": exam chip, title, two-line description, progress with
 * percentage and an action button.
 */
@Composable
fun GotestPackageCard(
    chipText: String,
    title: String,
    description: String,
    progress: Float?,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    chipTone: GotestChipTone = GotestChipTone.Blue,
    progressColor: Color = GotestTheme.colors.bgYellow,
) {
    GotestCard(modifier = modifier, verticalSpacing = GotestTheme.spacing.lg) {
        GotestChip(text = chipText, tone = chipTone)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (progress != null) {
            GotestTrackedProgress(progress = progress, color = progressColor)
        }
        GotestButton(
            text = actionText,
            onClick = onAction,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

/** Progress bar inside a bordered track plus the percentage label ("progress" in the design). */
@Composable
fun GotestTrackedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = GotestTheme.colors.bgYellow,
) {
    val sizes = GotestTheme.sizes
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .background(GotestTheme.colors.bgSubtle, GotestTheme.shapes.full)
                    .border(sizes.strokeThin, GotestTheme.colors.borderSubtle, GotestTheme.shapes.full)
                    .padding(spacing.xxs),
        ) {
            GotestProgressBar(progress = progress, color = color, trackColor = Color.Transparent)
        }
        Text(
            text = stringResource(R.string.percent_value, (progress * PERCENT).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

/**
 * The design's "Mobile/Activity Row": chip, title and subtitle on the left, a small action
 * button on the right. Used by Aktivitas Terkini and Riwayat.
 */
@Composable
fun GotestActivityRow(
    chipText: String,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    chipTone: GotestChipTone = GotestChipTone.Blue,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            GotestChip(text = chipText, tone = chipTone)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GotestButton(
            text = actionText,
            onClick = onAction,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

/** Hairline between rows inside a card. */
@Composable
fun GotestDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(GotestTheme.sizes.strokeThin)
                .background(GotestTheme.colors.borderSubtle),
    )
}

@Preview(showBackground = true)
@Composable
private fun GotestListItemsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            GotestPackageCard(
                chipText = "UTBK-SNBT",
                title = "Latihan September Session 1",
                description = "Latihan persiapan UTBK-SNBT dengan 20 soal dan penilaian benar 1, salah 0.",
                progress = 0.75f,
                actionText = "Mulai Latihan",
                onAction = {},
            )
            GotestActivityRow(
                chipText = "UTBK-SNBT",
                title = "Latihan September Session 1",
                subtitle = "Diselesaikan 21 Oktober 2026",
                actionText = "Lihat Hasil",
                onAction = {},
            )
        }
    }
}
