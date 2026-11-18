package com.gotest.app.feature.leaderboard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.leaderboard.domain.LeaderboardPackageCard
import com.gotest.app.feature.leaderboard.domain.LeaderboardRules
import kotlin.math.absoluteValue

/** One person on a board, from the global board or a package board. */
@Immutable
data class BoardRow(
    val rank: Int,
    val userId: Long,
    val name: String,
    val points: Int,
    /** Previous rank minus this rank; null shows "-". */
    val change: Int?,
    val isCurrentUser: Boolean = false,
    /** Try Out package boards: seconds used, shown on the podium. */
    val durationUsed: Int? = null,
)

internal object BoardDimens {
    val laurelFrameWidth = 104.dp
    val laurelFrameHeight = 100.dp
    val laurelWidth = 96.dp
    val laurelHeight = 88.dp
    val podiumAvatar = 48.dp
    val podiumAvatarTop = 20.dp
    val podiumBlockFirst = 100.dp
    val podiumBlockSecond = 72.dp
    val podiumBlockThird = 56.dp
    val rankColumn = 28.dp
    val pointsColumn = 56.dp
    val changeColumn = 72.dp
    val rowAvatar = 28.dp
    val trophyWidth = 117.dp
    val trophyHeight = 76.dp
}

private const val PODIUM_SIZE = 3

/** "Peringkatmu Saat Ini": the account's global rank, or "—" before it has one (web LeaderboardHero). */
@Composable
fun LeaderboardHero(
    current: BoardRow?,
    totalParticipants: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = colors.bgBrandSoft,
        showBorder = false,
        verticalSpacing = spacing.md,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(GotestIcons.Linear.Award),
                        contentDescription = null,
                        modifier = Modifier.size(GotestTheme.sizes.iconXs),
                        tint = colors.bgYellow,
                    )
                    Text(
                        text = stringResource(R.string.leaderboard_hero_kicker),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textBrandDark,
                    )
                }
                Text(
                    text = stringResource(R.string.leaderboard_hero_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textBrandDark,
                )
                Text(
                    text = stringResource(R.string.leaderboard_hero_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textBrandDark,
                )
            }
            Image(
                painter = painterResource(R.drawable.illus_trophy),
                contentDescription = null,
                modifier = Modifier.size(BoardDimens.trophyWidth, BoardDimens.trophyHeight),
                contentScale = ContentScale.Fit,
            )
        }
        MyRankCard(current = current, totalParticipants = totalParticipants)
    }
}

@Composable
private fun MyRankCard(
    current: BoardRow?,
    totalParticipants: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.xs) {
        Text(
            text = stringResource(R.string.leaderboard_hero_my_rank),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = current?.rank?.let(GotestFormat::number) ?: stringResource(R.string.leaderboard_hero_no_rank),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.textPrimary,
            )
            if (current != null) RankChange(change = current.change)
        }
        Text(
            text = stringResource(R.string.leaderboard_hero_total, GotestFormat.number(totalParticipants)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.leaderboard_hero_footer),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xs)
                    .background(colors.bgGrey, MaterialTheme.shapes.small)
                    .padding(spacing.sm),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

/**
 * Web RankChange. Unlabelled (hero, tables): null "-", up and down with arrows, 0 with a flat line.
 * [labelled] (package cards): "{n} Posisi", and "-" for both null and 0.
 */
@Composable
fun RankChange(
    change: Int?,
    modifier: Modifier = Modifier,
    labelled: Boolean = false,
) {
    val colors = GotestTheme.colors
    val value = change ?: 0
    val (container, content) =
        when {
            change == null -> colors.bgGrey to colors.textSecondary
            value > 0 -> colors.statusSuccessSoft to colors.statusSuccess
            value < 0 -> colors.statusDangerSoft to colors.statusDanger
            else -> colors.bgGrey to colors.textSecondary
        }
    val plain = change == null || (labelled && value == 0)
    val text =
        when {
            plain -> stringResource(R.string.leaderboard_change_none)
            labelled -> stringResource(R.string.leaderboard_card_change, value.absoluteValue)
            else -> value.absoluteValue.toString()
        }
    val description = changeDescription(value)
    Row(
        modifier =
            modifier
                .background(container, GotestTheme.shapes.full)
                .padding(horizontal = GotestTheme.spacing.sm, vertical = GotestTheme.spacing.xxs)
                .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!plain) {
            Icon(
                painter = painterResource(changeIcon(value)),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconXs),
                tint = content,
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun changeDescription(change: Int): String =
    when {
        change > 0 -> stringResource(R.string.leaderboard_cd_change_up, change)
        change < 0 -> stringResource(R.string.leaderboard_cd_change_down, -change)
        else -> stringResource(R.string.leaderboard_cd_change_none)
    }

@DrawableRes
private fun changeIcon(change: Int): Int =
    when {
        change > 0 -> GotestIcons.Linear.ArrowUp
        change < 0 -> GotestIcons.Linear.ArrowDown
        else -> GotestIcons.Linear.Minus
    }

/**
 * The top three in 2-1-3 order; a missing place says "Belum ada" with 0 points, as on the web.
 * [showTime] adds the time used (Try Out package boards). [pointsLabel] is the server's word for the score
 * of a Latihan package board ("Nilai" or "Skor Latihan"), used in place of "pt" (A2e-6 = b).
 */
@Composable
fun BoardPodium(
    top: List<BoardRow>,
    showTime: Boolean,
    modifier: Modifier = Modifier,
    pointsLabel: String? = null,
) {
    val byRank = top.associateBy(BoardRow::rank)
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.none) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(2, 1, PODIUM_SIZE).forEach { rank ->
                PodiumColumn(
                    row = byRank[rank],
                    rank = rank,
                    showTime = showTime,
                    pointsLabel = pointsLabel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    row: BoardRow?,
    rank: Int,
    showTime: Boolean,
    pointsLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val points = GotestFormat.number(row?.points ?: 0)
    val caption =
        when {
            showTime -> {
                stringResource(R.string.leaderboard_time_points, LeaderboardRules.clock(row?.durationUsed), points)
            }

            pointsLabel != null -> {
                stringResource(R.string.leaderboard_points_labelled, points, pointsLabel)
            }

            else -> {
                stringResource(R.string.leaderboard_points, points)
            }
        }
    val (laurel, block, number) =
        when (rank) {
            1 -> Triple(R.drawable.laurel_gold, colors.bgYellowSoft, colors.bgYellowDark)
            2 -> Triple(R.drawable.laurel_silver, colors.bgGrey, colors.textPrimary)
            else -> Triple(R.drawable.laurel_bronze, colors.podiumBronze, colors.onPodiumBronze)
        }
    val height =
        when (rank) {
            1 -> BoardDimens.podiumBlockFirst
            2 -> BoardDimens.podiumBlockSecond
            else -> BoardDimens.podiumBlockThird
        }
    val rankDescription = stringResource(R.string.leaderboard_cd_podium_rank, rank)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(BoardDimens.laurelFrameWidth, BoardDimens.laurelFrameHeight)) {
            Image(
                painter = painterResource(laurel),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(BoardDimens.laurelWidth, BoardDimens.laurelHeight),
                contentScale = ContentScale.Fit,
            )
            InitialsAvatar(
                name = row?.name.orEmpty(),
                size = BoardDimens.podiumAvatar,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = BoardDimens.podiumAvatarTop),
            )
        }
        Text(
            text = row?.name ?: stringResource(R.string.leaderboard_podium_none),
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
                    .height(height)
                    .background(
                        block,
                        MaterialTheme.shapes.medium.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
                    ).clearAndSetSemantics { contentDescription = rankDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = rank.toString(), style = MaterialTheme.typography.headlineMedium, color = number)
        }
    }
}

/** "Belum ada data Leaderboard" with the board's own message. */
@Composable
fun BoardEmpty(
    message: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        GotestEmptyView(
            icon = GotestIcons.Linear.Leaderboard,
            title = stringResource(R.string.leaderboard_empty_title),
            message = message,
        )
    }
}

/**
 * Web RankTable at phone width: #, Nama, Point, Perubahan (the web hides the third column below 640 px).
 * The account's own row is tinted and says "(Kamu)".
 */
@Composable
fun BoardTable(
    rows: List<BoardRow>,
    modifier: Modifier = Modifier,
    /** The server's word for the score of a Latihan package board, in place of "Point" (A2e-6 = b). */
    pointsLabel: String? = null,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.none),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        TableLine(
            rank = {
                Text(
                    stringResource(R.string.leaderboard_col_rank),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            name = { Text(stringResource(R.string.leaderboard_col_name), style = MaterialTheme.typography.titleSmall) },
            points = {
                Text(
                    pointsLabel ?: stringResource(R.string.leaderboard_col_points),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            change = {
                Text(
                    stringResource(R.string.leaderboard_col_change),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            background = colors.bgSubtle,
        )
        rows.forEach { row ->
            GotestDivider()
            BoardTableRow(row)
        }
    }
}

@Composable
private fun BoardTableRow(
    row: BoardRow,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val text = if (row.isCurrentUser) colors.textBrandDark else colors.textPrimary
    TableLine(
        modifier = modifier,
        background = if (row.isCurrentUser) colors.bgBrandSoft else colors.bgApp,
        rank = { Text(row.rank.toString(), style = MaterialTheme.typography.labelMedium, color = text) },
        name = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(name = row.name, size = BoardDimens.rowAvatar)
                Text(
                    text = if (row.isCurrentUser) stringResource(R.string.leaderboard_you, row.name) else row.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        points = { Text(GotestFormat.number(row.points), style = MaterialTheme.typography.labelMedium, color = text) },
        change = { RankChange(change = row.change) },
    )
}

@Composable
private fun TableLine(
    rank: @Composable () -> Unit,
    name: @Composable () -> Unit,
    points: @Composable () -> Unit,
    change: @Composable () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().background(background).padding(GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(BoardDimens.rankColumn), contentAlignment = Alignment.Center) { rank() }
        Box(modifier = Modifier.weight(1f)) { name() }
        Box(modifier = Modifier.width(BoardDimens.pointsColumn)) { points() }
        Box(modifier = Modifier.width(BoardDimens.changeColumn)) { change() }
    }
}

@Immutable
private data class AvatarTone(
    val container: Color,
    val content: Color,
)

/** Web Avatar: up to two initials, "—" without a name, the tone from the sum of the name's characters. */
@Composable
fun InitialsAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val tones =
        listOf(
            AvatarTone(colors.illustrationGreenSoft, colors.illustrationGreenDark),
            AvatarTone(colors.illustrationBlueSoft, colors.illustrationBlueDark),
            AvatarTone(colors.illustrationBrandSoft, colors.illustrationBrandDark),
            AvatarTone(colors.illustrationYellowSoft, colors.illustrationYellowDark),
            AvatarTone(colors.illustrationRedSoft, colors.illustrationRedDark),
        )
    val tone = tones[name.sumOf(Char::code) % tones.size]
    val initials =
        name
            .split(' ')
            .filter(String::isNotEmpty)
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "—" }
    Box(
        modifier = modifier.size(size).background(tone.container, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = MaterialTheme.typography.labelSmall, color = tone.content, maxLines = 1)
    }
}

/** Web PackageCardGrid card: level, name, size and time, rank / points / accuracy, change and "Lihat Leaderboard". */
@Composable
fun BoardPackageCard(
    card: LeaderboardPackageCard,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val none = stringResource(R.string.leaderboard_card_none)
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        GotestChip(text = card.categoryName, tone = categoryTone(card.categoryName))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = card.name,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        R.string.leaderboard_card_meta,
                        card.questionCount,
                        card.timeLabel ?: stringResource(R.string.leaderboard_minutes, card.durationMinutes),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            StatTile(
                value =
                    card.rank?.let { stringResource(R.string.leaderboard_card_rank, GotestFormat.number(it)) } ?: none,
                label = stringResource(R.string.leaderboard_card_of, GotestFormat.number(card.totalParticipants)),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = card.points?.let(GotestFormat::number) ?: none,
                label = card.pointsLabel ?: stringResource(R.string.leaderboard_card_points),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = card.accuracy?.let { stringResource(R.string.leaderboard_card_percent, it) } ?: none,
                label = stringResource(R.string.leaderboard_card_accuracy),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankChange(change = card.change, labelled = true)
            GotestButton(
                text = stringResource(R.string.leaderboard_card_open),
                onClick = onOpen,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
                trailingIcon = GotestIcons.Linear.ArrowRight,
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.small)
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.small)
                .padding(spacing.sm),
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

/** Web `categoryBadgeClass`: SMA green, UTBK/SNBT blue, others violet. */
internal fun categoryTone(name: String): GotestChipTone {
    val value = name.lowercase()
    return when {
        "sma" in value -> GotestChipTone.Green
        "utbk" in value || "snbt" in value -> GotestChipTone.Blue
        else -> GotestChipTone.Violet
    }
}

private val previewRows =
    listOf(
        BoardRow(1, 1, "Nadia Putri", 912, 0, durationUsed = 5_400),
        BoardRow(2, 2, "Rafi Akbar", 880, 2, durationUsed = 5_712),
        BoardRow(4, 4, "Alya Rahma", 840, -1),
        BoardRow(5, 5, "Gilang Ramadhan", 820, null, isCurrentUser = true),
    )

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun BoardComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            LeaderboardHero(current = previewRows.last(), totalParticipants = 1_234)
            BoardPodium(top = previewRows.take(2), showTime = true)
            BoardTable(rows = previewRows.drop(2))
            BoardPackageCard(
                card =
                    LeaderboardPackageCard(
                        id = 11,
                        name = "Try Out UTBK-SNBT 3",
                        categoryName = "UTBK-SNBT",
                        questionCount = 160,
                        durationMinutes = 195,
                        rank = 42,
                        totalParticipants = 137,
                        points = 612,
                        accuracy = 71,
                        change = 3,
                    ),
                onOpen = {},
            )
        }
    }
}
