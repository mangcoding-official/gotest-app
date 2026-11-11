package com.gotest.app.feature.studypackage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.CategoryAccent

/** One labelled value in the grey footer of topic and seasonal cards. */
@Immutable
internal data class CardStat(
    val label: String,
    val value: String,
)

/**
 * The Paket frames' list card: a white card inset 4dp in a grey frame, with a grey [footer]
 * strip under it (category, subject, topic and seasonal tryout cards).
 */
@Composable
internal fun NestedCard(
    onClick: () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GotestTheme.colors
    val border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.bgSubtle,
        border = border,
    ) {
        Column(modifier = Modifier.padding(GotestTheme.spacing.xs)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = colors.bgApp,
                border = border,
            ) {
                Column(content = content)
            }
            footer()
        }
    }
}

/** Small rounded colour bar on category and subject cards. */
@Composable
internal fun AccentBar(
    accent: CategoryAccent,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(StudyPackageDimens.accentBarWidth, StudyPackageDimens.accentBarHeight)
                .background(accent.color(), GotestTheme.shapes.full),
    )
}

/** "12 Sub Kategori • 12 Paket latihan" footer line. */
@Composable
internal fun CardMetaLine(
    start: String,
    end: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = start, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        Box(
            modifier =
                Modifier
                    .size(StudyPackageDimens.metaDot)
                    .background(GotestTheme.colors.bgGrey, GotestTheme.shapes.full),
        )
        Text(text = end, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

/** Two stats side by side with a hairline between them ("Dikerjakan | Dikerjakan Pada"). */
@Composable
internal fun CardStatsFooter(
    first: CardStat,
    second: CardStat,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        CardStatColumn(stat = first, modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .width(GotestTheme.sizes.strokeThin)
                    .fillMaxHeight()
                    .background(GotestTheme.colors.bgGrey),
        )
        CardStatColumn(stat = second, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CardStatColumn(
    stat: CardStat,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = stat.label, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textPrimary)
        Text(text = stat.value, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

/**
 * Topic and seasonal tryout card: title and subtitle, an action button with an optional green
 * score chip, and two stats in the footer.
 */
@Composable
internal fun StudyItemCard(
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    onClick: () -> Unit,
    stats: Pair<CardStat, CardStat>,
    modifier: Modifier = Modifier,
    scoreText: String? = null,
) {
    val spacing = GotestTheme.spacing
    NestedCard(
        onClick = onClick,
        footer = { CardStatsFooter(first = stats.first, second = stats.second) },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = actionText,
                onClick = onAction,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
            if (scoreText != null) {
                GotestChip(text = scoreText, tone = GotestChipTone.Green)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyCardsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            NestedCard(
                onClick = {},
                footer = { CardMetaLine(start = "12 Sub Kategori", end = "12 Paket latihan") },
            ) {
                Column(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
                    AccentBar(accent = CategoryAccent.Yellow)
                    Text(text = "Perguruan Tinggi", style = MaterialTheme.typography.titleLarge)
                }
            }
            StudyItemCard(
                title = "Dasar-Dasar Pemrograman",
                subtitle = "12 Topik",
                actionText = "Latihan Lagi",
                onAction = {},
                onClick = {},
                stats = CardStat("Dikerjakan", "4") to CardStat("Dikerjakan Pada", "12 Okt 2025"),
                scoreText = "Skor : 412",
            )
        }
    }
}
