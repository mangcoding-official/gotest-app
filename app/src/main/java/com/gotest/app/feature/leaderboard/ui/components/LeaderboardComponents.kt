package com.gotest.app.feature.leaderboard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.ui.GotestFormat
import kotlin.math.absoluteValue

/** Leaderboard sizes the shared tokens do not cover (measured from frames 3616 and 9653 unless noted). */
internal object LeaderboardDimens {
    /** Frame holding the laurel and the avatar ("Frame 360"). */
    val laurelFrameWidth = 104.dp
    val laurelFrameHeight = 100.dp
    val laurelWidth = 96.dp
    val laurelHeight = 88.dp
    val podiumAvatar = 48.dp

    /** Avatar offset from the top of the laurel frame. */
    val podiumAvatarTop = 20.dp
    val podiumBlockFirst = 100.dp
    val podiumBlockSecond = 72.dp
    val podiumBlockThird = 56.dp

    /** Fixed-width rank column of "Mobile/Leaderboard Row". */
    val rankColumn = 24.dp
    val trophyWidth = 117.dp
    val trophyHeight = 76.dp

    /** White icon tile of the "Tentang Leaderboard" items (frame 21007). */
    val infoTile = 36.dp
    val infoTileRadius = 10.dp

    /** Fixed-width rank column and initials avatar of the participant search rows (frame 21556). */
    val searchRankColumn = 28.dp
    val searchAvatar = 36.dp
}

/** What the second line of a leaderboard row shows. */
enum class LeaderboardMetric {
    /** Global: "141x attempt". */
    Attempts,

    /** Per package: "Selesai dalam 2:32:41". */
    FinishTime,
}

@Immutable
private data class PodiumStyle(
    @param:DrawableRes val laurel: Int,
    val blockColor: Color,
    val numberColor: Color,
)

@Composable
private fun podiumStyle(rank: Int): PodiumStyle {
    val colors = GotestTheme.colors
    return when (rank) {
        1 -> {
            PodiumStyle(R.drawable.laurel_gold, colors.bgYellowSoft, colors.bgYellowDark)
        }

        2 -> {
            PodiumStyle(R.drawable.laurel_silver, colors.bgGrey, colors.textPrimary)
        }

        else -> {
            PodiumStyle(R.drawable.laurel_bronze, colors.podiumBronze, colors.onPodiumBronze)
        }
    }
}

/** "Selesai dalam 2:32:41" or "141x attempt". */
@Composable
internal fun entryCaption(
    entry: LeaderboardEntry,
    metric: LeaderboardMetric,
): String =
    when (metric) {
        LeaderboardMetric.Attempts -> {
            stringResource(R.string.leaderboard_attempts, GotestFormat.number(entry.attempts ?: 0))
        }

        LeaderboardMetric.FinishTime -> {
            stringResource(R.string.leaderboard_finished_in, GotestFormat.clock(entry.finishSeconds ?: 0))
        }
    }

/** The name, with "(Kamu)" on the student's own entry. */
@Composable
internal fun displayName(entry: LeaderboardEntry): String =
    if (entry.isCurrentUser) stringResource(R.string.leaderboard_you, entry.name) else entry.name

/**
 * Top three in 2-1-3 order: laurel with the avatar, name, points (plus finish time per
 * package) and a coloured block with the rank.
 */
@Composable
fun LeaderboardPodium(
    entries: List<LeaderboardEntry>,
    metric: LeaderboardMetric,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(GotestTheme.spacing.lg),
) {
    val byRank = entries.associateBy { it.rank }
    GotestCard(
        modifier = modifier,
        shape = shape,
        contentPadding = contentPadding,
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            podiumOrder.forEach { rank ->
                PodiumSlot(entry = byRank[rank], rank = rank, metric = metric)
            }
        }
    }
}

private val podiumOrder = listOf(2, 1, 3)

@Composable
private fun RowScope.PodiumSlot(
    entry: LeaderboardEntry?,
    rank: Int,
    metric: LeaderboardMetric,
) {
    if (entry == null) {
        Spacer(modifier = Modifier.weight(1f))
    } else {
        PodiumColumn(entry = entry, rank = rank, metric = metric, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumColumn(
    entry: LeaderboardEntry,
    rank: Int,
    metric: LeaderboardMetric,
    modifier: Modifier = Modifier,
) {
    val style = podiumStyle(rank)
    val colors = GotestTheme.colors
    val points = GotestFormat.number(entry.points)
    val caption =
        when (metric) {
            LeaderboardMetric.Attempts -> {
                stringResource(R.string.leaderboard_points, points)
            }

            LeaderboardMetric.FinishTime -> {
                stringResource(R.string.leaderboard_points_time, points, GotestFormat.clock(entry.finishSeconds ?: 0))
            }
        }
    val blockHeight =
        when (rank) {
            1 -> LeaderboardDimens.podiumBlockFirst
            2 -> LeaderboardDimens.podiumBlockSecond
            else -> LeaderboardDimens.podiumBlockThird
        }
    val blockShape = MaterialTheme.shapes.medium.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)
    val rankDescription = stringResource(R.string.leaderboard_cd_podium_rank, rank)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LaurelAvatar(laurel = style.laurel, avatarUrl = entry.avatarUrl)
        Text(
            text = displayName(entry),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(blockHeight)
                    .background(style.blockColor, blockShape)
                    .clearAndSetSemantics { contentDescription = rankDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = rank.toString(), style = MaterialTheme.typography.headlineMedium, color = style.numberColor)
        }
    }
}

/** Avatar sitting inside the gold/silver/bronze laurel. */
@Composable
private fun LaurelAvatar(
    @DrawableRes laurel: Int,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(LeaderboardDimens.laurelFrameWidth, LeaderboardDimens.laurelFrameHeight),
    ) {
        Image(
            painter = painterResource(laurel),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(LeaderboardDimens.laurelWidth, LeaderboardDimens.laurelHeight),
            contentScale = ContentScale.Fit,
        )
        GotestAvatar(
            model = avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = LeaderboardDimens.podiumAvatarTop),
            size = LeaderboardDimens.podiumAvatar,
        )
    }
}

/** White card with one "Mobile/Leaderboard Row" per entry. */
@Composable
fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    metric: LeaderboardMetric,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        contentPadding = PaddingValues(GotestTheme.spacing.none),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        entries.forEach { entry -> LeaderboardRow(entry = entry, metric = metric) }
    }
}

/** Rank, avatar, name with caption, points and the change badge; the student's row is grey. */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    metric: LeaderboardMetric,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(if (entry.isCurrentUser) colors.bgSubtle else colors.bgApp)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = GotestFormat.number(entry.rank),
            modifier = Modifier.width(LeaderboardDimens.rankColumn),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        GotestAvatar(model = entry.avatarUrl, contentDescription = null, size = GotestTheme.sizes.avatarXs)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = displayName(entry),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entryCaption(entry, metric),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.leaderboard_points, GotestFormat.number(entry.points)),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 1,
        )
        RankChangeBadge(change = entry.rankChange, emphasized = entry.isCurrentUser)
    }
}

@Immutable
private data class BadgeColors(
    val container: Color,
    val content: Color,
)

/**
 * "2 ↑" / "4 ↓" / "0 —" next to each row. [emphasized] (the student's own row) uses a
 * solid fill instead of the soft one.
 */
@Composable
fun RankChangeBadge(
    change: Int,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = GotestTheme.colors
    val badge =
        when {
            change > 0 && emphasized -> BadgeColors(colors.bgGreen, colors.statusSuccessSoft)
            change > 0 -> BadgeColors(colors.bgGreenSoft, colors.bgGreen)
            change < 0 && emphasized -> BadgeColors(colors.statusDanger, colors.statusDangerSoft)
            change < 0 -> BadgeColors(colors.statusDangerSoft, colors.illustrationRed)
            else -> BadgeColors(colors.bgGrey, colors.textPrimary)
        }
    val description = rankChangeDescription(change)
    Row(
        modifier =
            modifier
                .background(badge.container, MaterialTheme.shapes.small)
                .padding(horizontal = GotestTheme.spacing.sm, vertical = GotestTheme.spacing.xxs)
                .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = change.absoluteValue.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = badge.content,
        )
        Icon(
            painter = painterResource(rankChangeIcon(change)),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = badge.content,
        )
    }
}

/** "1 ↑" pill next to "#12" on the "Peringkatmu saat ini" card. */
@Composable
fun RankChangePill(
    change: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val badge =
        when {
            change > 0 -> BadgeColors(colors.bgGreen, colors.textOnBrand)
            change < 0 -> BadgeColors(colors.statusDanger, colors.statusDangerSoft)
            else -> BadgeColors(colors.bgGrey, colors.textPrimary)
        }
    val description = rankChangeDescription(change)
    Row(
        modifier =
            modifier
                .background(badge.container, GotestTheme.shapes.full)
                .padding(horizontal = GotestTheme.spacing.sm, vertical = GotestTheme.spacing.xxs)
                .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = change.absoluteValue.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = badge.content,
        )
        Icon(
            painter = painterResource(rankChangeIcon(change)),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconXs),
            tint = badge.content,
        )
    }
}

@DrawableRes
internal fun rankChangeIcon(change: Int): Int =
    when {
        change > 0 -> GotestIcons.Linear.ArrowUp
        change < 0 -> GotestIcons.Linear.ArrowDown
        else -> GotestIcons.Linear.Minus
    }

@Composable
internal fun rankChangeDescription(change: Int): String =
    when {
        change > 0 -> stringResource(R.string.leaderboard_cd_change_up, change)
        change < 0 -> stringResource(R.string.leaderboard_cd_change_down, -change)
        else -> stringResource(R.string.leaderboard_cd_change_none)
    }

private val previewEntries =
    listOf(
        LeaderboardEntry(1, "John S.", null, 424, 152, 7_805, 0, false),
        LeaderboardEntry(2, "Michael T.", null, 324, 148, 8_621, 0, false),
        LeaderboardEntry(3, "Emily C.", null, 172, 145, 9_612, 0, false),
        LeaderboardEntry(4, "Alex Thompson", null, 124, 141, 9_161, 2, false),
        LeaderboardEntry(6, "Ethan Williams", null, 118, 130, 9_482, -4, false),
        LeaderboardEntry(12, "Gilang P.", null, 104, 98, 10_280, 1, true),
        LeaderboardEntry(13, "Sophia Carter", null, 101, 96, 10_400, 0, false),
    )

@Preview(showBackground = true)
@Composable
private fun LeaderboardPodiumPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            LeaderboardPodium(entries = previewEntries.take(3), metric = LeaderboardMetric.Attempts)
            LeaderboardPodium(
                entries = previewEntries.take(3),
                metric = LeaderboardMetric.FinishTime,
                shape = GotestTheme.shapes.largeIncreased,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardListPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            LeaderboardList(entries = previewEntries.drop(3), metric = LeaderboardMetric.Attempts)
            LeaderboardList(entries = previewEntries.drop(3), metric = LeaderboardMetric.FinishTime)
            RankChangePill(change = 1)
        }
    }
}
