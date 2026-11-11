package com.gotest.app.feature.multiplayer.ui.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.question.GotestQuestionCell
import com.gotest.app.core.designsystem.component.question.QuestionCellState
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.PlayerStanding
import com.gotest.app.feature.multiplayer.domain.QuestionOutcome
import com.gotest.app.feature.multiplayer.ui.components.DotChipTone
import com.gotest.app.feature.multiplayer.ui.components.MbAwardChip
import com.gotest.app.feature.multiplayer.ui.components.MbDotChip
import com.gotest.app.feature.multiplayer.ui.components.MbLegendItem
import com.gotest.app.feature.multiplayer.ui.components.MbRankHeader
import com.gotest.app.feature.multiplayer.ui.components.MbRankRow
import com.gotest.app.feature.multiplayer.ui.components.MbStatTile
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.RankRowKind
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.gameTypeLabel
import com.gotest.app.feature.multiplayer.ui.components.outerRing
import com.gotest.app.feature.multiplayer.ui.components.playedAtLabel
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel
import com.gotest.app.feature.multiplayer.ui.components.shortName

/** "Selesai" chip, "Hasil Room • Penalaran Matematika" and the game's facts. */
@Composable
fun ResultTitleCard(
    result: GameResult,
    modifier: Modifier = Modifier,
) {
    val settings = result.settings
    val count = questionCountLabel(settings.questionCount)
    val duration = durationLabel(settings.durationMinutes)
    val playedAt = playedAtLabel(result.playedAt)
    val meta =
        if (settings.type == GameType.Room) {
            stringResource(
                R.string.multiplayer_meta_four,
                count,
                duration,
                pluralStringResource(R.plurals.multiplayer_player_count, result.standings.size, result.standings.size),
                playedAt,
            )
        } else {
            stringResource(R.string.multiplayer_meta_three, count, duration, playedAt)
        }
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = GotestTheme.spacing.xl, vertical = GotestTheme.spacing.lg),
        verticalSpacing = GotestTheme.spacing.sm,
    ) {
        MbDotChip(text = stringResource(R.string.multiplayer_status_finished), tone = DotChipTone.Green)
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(
                text =
                    stringResource(
                        R.string.multiplayer_result_heading,
                        gameTypeLabel(settings.type),
                        settings.subtest,
                    ),
                style = MaterialTheme.typography.headlineSmall,
                color = GotestTheme.colors.textPrimary,
            )
            Text(text = meta, style = MaterialTheme.typography.bodyMedium, color = GotestTheme.colors.textSecondary)
        }
    }
}

/** Room winner with the "Juara 1" chip. */
@Composable
fun ChampionCard(
    champion: PlayerStanding,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp, MaterialTheme.shapes.large)
                .padding(spacing.lg)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = champion.avatarUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(MultiplayerDimens.championAvatar)
                    .clip(GotestTheme.shapes.full)
                    .background(GotestTheme.colors.bgYellow),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = playerName(champion.name, champion.isCurrentUser),
                style = MaterialTheme.typography.headlineMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        R.string.multiplayer_result_champion_meta,
                        champion.correct,
                        GotestFormat.clock(champion.totalSeconds),
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textPrimary,
            )
        }
        MbAwardChip(text = stringResource(R.string.multiplayer_result_champion))
    }
}

/** "Peringkat" table of a room. */
@Composable
fun RankingCard(
    standings: List<PlayerStanding>,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.md) {
        Text(
            text = stringResource(R.string.multiplayer_result_ranking),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium)
                    .padding(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            MbRankHeader()
            standings.forEach { standing ->
                MbRankRow(
                    position = standing.rank,
                    name = playerName(shortName(standing.name), standing.isCurrentUser),
                    avatarUrl = standing.avatarUrl,
                    correctLabel = stringResource(R.string.multiplayer_result_correct_count, standing.correct),
                    timeLabel = GotestFormat.clock(standing.totalSeconds),
                    kind =
                        when {
                            standing.rank == 1 -> RankRowKind.Champion
                            standing.isCurrentUser -> RankRowKind.You
                            else -> RankRowKind.Default
                        },
                )
            }
        }
        Text(
            text = stringResource(R.string.multiplayer_result_ranking_note),
            modifier = Modifier.padding(top = GotestTheme.spacing.md),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

/** The two duelists side by side with "VS"; the winner's panel is highlighted. */
@Composable
fun VersusCard(
    me: PlayerStanding,
    opponent: PlayerStanding,
    winnerId: String?,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DuelistPanel(standing = me, isWinner = me.playerId == winnerId)
            Box(
                modifier =
                    Modifier
                        .background(GotestTheme.colors.bgBrandDark, GotestTheme.shapes.full)
                        .padding(horizontal = GotestTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.multiplayer_result_versus),
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.bgApp,
                )
            }
            DuelistPanel(standing = opponent, isWinner = opponent.playerId == winnerId)
        }
        Text(
            text = stringResource(R.string.multiplayer_result_duel_note),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun DuelistPanel(
    standing: PlayerStanding,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (isWinner) {
                        Modifier.outerRing(
                            colors.illustrationYellowSoft,
                            GotestTheme.spacing.xs,
                            shape,
                        )
                    } else {
                        Modifier
                    },
                ),
        shape = shape,
        color = if (isWinner) colors.bgApp else colors.bgSubtle,
        border = if (isWinner) BorderStroke(GotestTheme.sizes.strokeThin, colors.bgYellow) else null,
    ) {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GotestAvatar(model = standing.avatarUrl, contentDescription = null, size = GotestTheme.sizes.avatarLg)
            if (isWinner) MbAwardChip(text = stringResource(R.string.multiplayer_result_winner))
            Text(
                text = playerName(standing.name, standing.isCurrentUser),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
                DuelStat(standing.correct.toString(), stringResource(R.string.multiplayer_result_correct))
                DuelStat(
                    GotestFormat.clock(standing.totalSeconds),
                    stringResource(R.string.multiplayer_result_total_time),
                )
            }
        }
    }
}

@Composable
private fun DuelStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = GotestTheme.colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

/** "Ringkasan kamu": totals, total time and the right/wrong grid. */
@Composable
fun MySummaryCard(
    result: GameResult,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
        Text(
            text = stringResource(R.string.multiplayer_result_summary),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MbStatTile(
                    value = result.myCorrect.toString(),
                    label = stringResource(R.string.multiplayer_result_correct),
                    container = colors.bgGreenSoft,
                    content = colors.illustrationGreenDark,
                    modifier = Modifier.weight(1f),
                )
                MbStatTile(
                    value = result.myWrong.toString(),
                    label = stringResource(R.string.multiplayer_result_wrong),
                    container = colors.statusDangerSoft,
                    content = colors.illustrationRedDark,
                    modifier = Modifier.weight(1f),
                )
                MbStatTile(
                    value = result.myEmpty.toString(),
                    label = stringResource(R.string.multiplayer_result_empty),
                    container = colors.statusWarningSoft,
                    content = colors.illustrationYellowDark,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    text = stringResource(R.string.multiplayer_result_total_time),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = GotestFormat.clock(result.me?.totalSeconds ?: 0),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
            }
        }
        OutcomeGrid(outcomes = result.myOutcomes)
    }
}

@Composable
private fun OutcomeGrid(
    outcomes: List<QuestionOutcome>,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val columns = MultiplayerDimens.OUTCOME_GRID_COLUMNS
    val labels =
        mapOf(
            QuestionOutcome.Correct to stringResource(R.string.multiplayer_result_correct),
            QuestionOutcome.Wrong to stringResource(R.string.multiplayer_result_wrong),
            QuestionOutcome.Empty to stringResource(R.string.multiplayer_result_empty),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.multiplayer_result_per_question),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
        outcomes.withIndex().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                row.forEach { (index, outcome) ->
                    val description =
                        stringResource(R.string.multiplayer_cd_question_outcome, index + 1, labels.getValue(outcome))
                    GotestQuestionCell(
                        number = index + 1,
                        state =
                            when (outcome) {
                                QuestionOutcome.Correct -> QuestionCellState.Correct
                                QuestionOutcome.Wrong -> QuestionCellState.Wrong
                                QuestionOutcome.Empty -> QuestionCellState.Skipped
                            },
                        onClick = null,
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .semantics(mergeDescendants = true) { contentDescription = description },
                    )
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
            MbLegendItem(colors.statusSuccess, labels.getValue(QuestionOutcome.Correct))
            MbLegendItem(colors.statusDanger, labels.getValue(QuestionOutcome.Wrong))
            MbLegendItem(colors.statusWarning, labels.getValue(QuestionOutcome.Empty))
        }
        Text(
            text = stringResource(R.string.multiplayer_result_no_review),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}
