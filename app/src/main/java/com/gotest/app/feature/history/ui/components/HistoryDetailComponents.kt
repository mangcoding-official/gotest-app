package com.gotest.app.feature.history.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.SectionResult
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.ui.detail.HistoryRankingRow
import java.time.LocalDate

/** Sizes of the ranking card that the shared tokens do not cover. */
private object HistoryDetailDimens {
    /** Square rank badge ("Button" 32x32 in the design). */
    val rankBadge = 32.dp

    /** Outer glow around the gold/silver/bronze badges (drop shadow with 2dp spread). */
    val medalRing = 2.dp

    /** Dot between the date and the duration. */
    val metaDot = 4.dp
}

/** Midpoint used to derive medal tones from existing tokens until dedicated ones exist. */
private const val BRONZE_RANK = 3

@Immutable
private data class MedalColors(
    val fill: Color,
    val edge: Color,
)

/** Gold, silver and bronze badge tones; null below the podium. */
@Composable
private fun medalColors(rank: Int): MedalColors? {
    val colors = GotestTheme.colors
    return when (rank) {
        1 -> MedalColors(fill = colors.bgYellow, edge = colors.illustrationYellowSoft)
        2 -> MedalColors(fill = colors.medalSilver, edge = colors.bgApp)
        BRONZE_RANK -> MedalColors(fill = colors.medalBronze, edge = colors.bgYellowSoft)
        else -> null
    }
}

/** Title, description and the "Hasil per section" table ("ringkasan" in the design). */
@Composable
fun HistorySummaryCard(
    title: String,
    description: String,
    sections: List<SectionResult>,
    score: Int,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier, verticalSpacing = GotestTheme.spacing.lg) {
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        SectionResultTable(sections = sections, score = score, modifier = Modifier.fillMaxWidth())
    }
}

/** Bordered table: header, one row per section with Benar/Salah/Kosong, and the final score. */
@Composable
fun SectionResultTable(
    sections: List<SectionResult>,
    score: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.medium
    val cellPadding = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)
    Column(
        modifier =
            modifier
                .clip(shape)
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape),
    ) {
        Text(
            text = stringResource(R.string.history_detail_sections_title),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle)
                    .then(cellPadding),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        sections.forEach { section ->
            GotestDivider()
            Column(modifier = cellPadding, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(text = section.name, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    LabeledCount(R.string.history_detail_correct, section.correct, colors.statusSuccess)
                    LabeledCount(R.string.history_detail_wrong, section.wrong, colors.statusDanger)
                    LabeledCount(R.string.history_detail_empty, section.empty, colors.textPrimary)
                }
            }
        }
        GotestDivider()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle)
                    .then(cellPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_detail_final_score),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
            )
            LabeledCount(R.string.history_detail_score, score, colors.statusSuccess)
        }
    }
}

/** "Benar 20": muted label followed by a coloured number. */
@Composable
private fun LabeledCount(
    @StringRes labelRes: Int,
    value: Int,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(text = GotestFormat.number(value), style = MaterialTheme.typography.labelMedium, color = valueColor)
    }
}

/** "Peringkat" card: note, top participants with medal badges and "Lihat Selengkapnya". */
@Composable
fun HistoryRankingCard(
    rows: List<HistoryRankingRow>,
    onSeeMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier, containerColor = colors.bgSubtle, verticalSpacing = spacing.lg) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.history_detail_ranking_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.history_detail_ranking_note),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.bgApp,
            border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
        ) {
            Column(modifier = Modifier.padding(horizontal = spacing.sm)) {
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.history_detail_ranking_empty),
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.lg),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                rows.forEachIndexed { index, row ->
                    if (index > 0) GotestDivider()
                    HistoryRankingRowItem(row = row)
                }
            }
        }
        GotestButton(
            text = stringResource(R.string.history_detail_see_more),
            onClick = onSeeMore,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Medium,
        )
    }
}

@Composable
private fun HistoryRankingRowItem(
    row: HistoryRankingRow,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.padding(horizontal = spacing.sm, vertical = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank = row.rank)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (row.isCurrentUser) stringResource(R.string.history_detail_you, row.name) else row.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                RankingMeta(completedOn = row.completedOn, durationSeconds = row.durationSeconds)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.history_detail_score),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            Text(
                text = GotestFormat.number(row.score),
                style = MaterialTheme.typography.labelMedium,
                color = if (row.rank <= BRONZE_RANK) colors.statusWarning else colors.textPrimary,
            )
        }
    }
}

/** "19 Sep 2026 • 20 Menit 32 Detik"; either part is skipped when unknown. */
@Composable
private fun RankingMeta(
    completedOn: LocalDate?,
    durationSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (completedOn != null) {
            Text(
                text = GotestFormat.mediumDate(completedOn),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        if (completedOn != null && durationSeconds != null) {
            Box(
                modifier =
                    Modifier
                        .size(HistoryDetailDimens.metaDot)
                        .background(colors.bgGrey, GotestTheme.shapes.full),
            )
        }
        if (durationSeconds != null) {
            Text(
                text = longDurationText(durationSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Gold/silver/bronze medal for the podium, a grey numbered square below it. */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val medal = medalColors(rank)
    val description = stringResource(R.string.history_detail_cd_rank, rank)
    val badgeModifier =
        modifier
            .clearAndSetSemantics { contentDescription = description }
            .size(HistoryDetailDimens.rankBadge)
    if (medal != null) {
        Box(
            modifier =
                badgeModifier
                    .outerRing(medal.fill, HistoryDetailDimens.medalRing, shape)
                    .background(medal.fill, shape)
                    .border(GotestTheme.sizes.strokeThin, medal.edge, shape),
            contentAlignment = Alignment.Center,
        ) {
            // TODO(design): the design uses a crown glyph that GotestIcons does not have yet.
            Icon(
                painter = painterResource(GotestIcons.Bold.Award),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = GotestTheme.colors.bgYellowSoft,
            )
        }
    } else {
        Box(
            modifier = badgeModifier.background(GotestTheme.colors.bgGrey, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = GotestFormat.number(rank),
                style = GotestTheme.typography.buttonLarge,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

/** Solid ring drawn outside the bounds, like the design's 2dp-spread drop shadow. */
private fun Modifier.outerRing(
    color: Color,
    width: Dp,
    shape: Shape,
): Modifier =
    drawBehind {
        val ring = width.toPx()
        val outline = shape.createOutline(Size(size.width + ring * 2, size.height + ring * 2), layoutDirection, this)
        translate(-ring, -ring) { drawOutline(outline, color) }
    }

@Preview(showBackground = true)
@Composable
private fun HistoryDetailComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            HistorySummaryCard(
                title = "Simulasi UTBK-SNBT September Session 1",
                description = "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit.",
                sections =
                    listOf(
                        SectionResult("Penalaran Umum", 20, 20, 5),
                        SectionResult("Penalaran Matematika", 20, 20, 5),
                    ),
                score = 140,
            )
            HistoryRankingCard(
                rows =
                    listOf(
                        HistoryRankingRow(1, "John Stuwart", null, 7_805, 424, false),
                        HistoryRankingRow(2, "Michael T.", null, 8_621, 324, false),
                        HistoryRankingRow(3, "Emily Carter", null, 9_612, 172, false),
                        HistoryRankingRow(4, "Alex Thompson", null, 9_161, 124, false),
                        HistoryRankingRow(12, "Gilang P.", LocalDate.of(2026, 9, 19), 1_232, 104, true),
                    ),
                onSeeMore = {},
            )
        }
    }
}
