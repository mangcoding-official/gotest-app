package com.gotest.app.feature.leaderboard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageRanking
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.examChipTone

/** "Peringkatmu saat ini #12 (1↑) dari 12.512 pengguna" with the trophy illustration. */
@Composable
fun MyRankCard(
    rank: Int,
    rankChange: Int,
    totalUsers: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = GotestTheme.shapes.largeIncreased
    Row(
        modifier =
            modifier
                .outerRing(colors.focusRing, spacing.xs, shape)
                .background(colors.bgApp, shape)
                .border(GotestTheme.sizes.strokeThin, colors.bgBrand, shape)
                .padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.leaderboard_my_rank_label),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_rank_value, GotestFormat.number(rank)),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                RankChangePill(change = rankChange)
            }
            Text(
                text = stringResource(R.string.leaderboard_total_users, GotestFormat.number(totalUsers)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Image(
            painter = painterResource(R.drawable.illus_trophy),
            contentDescription = null,
            modifier = Modifier.size(LeaderboardDimens.trophyWidth, LeaderboardDimens.trophyHeight),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Per Paket card: exam chip, size, title, rank/points/accuracy tiles, movement and action. */
@Composable
fun PackageRankingCard(
    ranking: PackageRanking,
    onViewLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier, verticalSpacing = spacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestChip(text = ranking.examLabel, tone = examChipTone(ranking.examLabel))
            Text(
                text =
                    stringResource(
                        R.string.leaderboard_package_meta,
                        ranking.questionCount,
                        ranking.durationMinutes,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Text(
            text = ranking.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            RankingStatTile(
                value = stringResource(R.string.leaderboard_rank_value, GotestFormat.number(ranking.rank)),
                label =
                    stringResource(
                        R.string.leaderboard_package_participants,
                        GotestFormat.number(ranking.participants),
                    ),
                modifier = Modifier.weight(1f),
            )
            RankingStatTile(
                value = GotestFormat.number(ranking.points),
                label = stringResource(R.string.leaderboard_package_points),
                modifier = Modifier.weight(1f),
            )
            RankingStatTile(
                value = stringResource(R.string.percent_value, ranking.accuracyPercent),
                label = stringResource(R.string.leaderboard_package_accuracy),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankMovementPill(change = ranking.rankChange)
            GotestButton(
                text = stringResource(R.string.leaderboard_view_leaderboard),
                onClick = onViewLeaderboard,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
    }
}

/** Grey tile with a bold value over a caption ("#12 / dari 12.412"). */
@Composable
private fun RankingStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.small)
                .padding(spacing.sm + spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "↑ Naik 2 posisi" / "↓ Turun 3 posisi" / "— Posisi tetap". */
@Composable
fun RankMovementPill(
    change: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val (container, content) =
        when {
            change > 0 -> colors.statusSuccessSoft to colors.statusSuccess
            change < 0 -> colors.statusDangerSoft to colors.statusDanger
            else -> colors.bgGrey to colors.textPrimary
        }
    val text =
        when {
            change > 0 -> stringResource(R.string.leaderboard_movement_up, change)
            change < 0 -> stringResource(R.string.leaderboard_movement_down, -change)
            else -> stringResource(R.string.leaderboard_movement_none)
        }
    Row(
        modifier =
            modifier
                .background(container, GotestTheme.shapes.full)
                .padding(horizontal = GotestTheme.spacing.sm, vertical = GotestTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(rankChangeIcon(change)),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconXs),
            tint = content,
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

/** Solid ring drawn outside the bounds, like the design's spread drop shadow. */
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
private fun LeaderboardCardsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            MyRankCard(rank = 12, rankChange = 1, totalUsers = 12_512, modifier = Modifier.fillMaxWidth())
            PackageRankingCard(
                ranking =
                    PackageRanking(
                        packageId = "p1",
                        title = "Tryout UTBK-SNBT 2026 (Sesi 1)",
                        examLabel = "UTBK-SNBT",
                        questionCount = 120,
                        durationMinutes = 120,
                        rank = 12,
                        participants = 12_412,
                        points = 680,
                        accuracyPercent = 76,
                        rankChange = 2,
                    ),
                onViewLeaderboard = {},
            )
            RankMovementPill(change = -3)
        }
    }
}
