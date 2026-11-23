package com.gotest.app.feature.multiplayer.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.ResultRow
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbDuelResultBanner
import com.gotest.app.feature.multiplayer.ui.components.MbPlayerRow
import com.gotest.app.feature.multiplayer.ui.components.MbRankHeader
import com.gotest.app.feature.multiplayer.ui.components.MbRankRow
import com.gotest.app.feature.multiplayer.ui.components.MbSeatList
import com.gotest.app.feature.multiplayer.ui.components.MbStatTile
import com.gotest.app.feature.multiplayer.ui.components.PlayerRowState
import com.gotest.app.feature.multiplayer.ui.components.RankRowKind
import com.gotest.app.feature.multiplayer.ui.components.formatClock
import com.gotest.app.feature.multiplayer.ui.components.formatDateTime
import com.gotest.app.feature.multiplayer.ui.components.modeLabel
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.problemText
import com.gotest.app.feature.multiplayer.ui.components.timeAgo
import com.gotest.app.feature.multiplayer.ui.hub.Body

/** The viewer is done; nobody's score shows until the game ends (web WaitingScreen). */
@Composable
internal fun WaitingContent(
    game: GameState,
    secondsLeft: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val done = game.players.count { it.finished }
    GotestCard(modifier = modifier.fillMaxWidth()) {
        SentHeader(secondsLeft)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_sent_players),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            GotestChip(
                stringResource(R.string.multiplayer_sent_finished_count, done, game.players.size),
                tone = GotestChipTone.Green,
            )
        }
        MbSeatList {
            game.players.forEach { player ->
                MbPlayerRow(
                    name = playerName(player.name, player.isYou),
                    subtitle =
                        if (player.finished) {
                            stringResource(
                                R.string.multiplayer_finished_ago,
                                timeAgo(player.finishedAt, game.serverNow),
                            )
                        } else {
                            stringResource(R.string.multiplayer_seat_still_playing)
                        },
                    state = if (player.finished) PlayerRowState.Finished else PlayerRowState.Playing,
                )
            }
        }
        // Nobody has to stay: the result waits in the history (web audit MB-12).
        GotestButton(
            text = stringResource(R.string.multiplayer_back_to_hub),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
        Text(
            text = stringResource(R.string.multiplayer_sent_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** "Jawabanmu sudah terkirim" with the time left in the game. */
@Composable
private fun SentHeader(secondsLeft: Long?) {
    val colors = GotestTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.multiplayer_sent_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.multiplayer_sent_message),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier =
                Modifier
                    .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                    .padding(horizontal = GotestTheme.spacing.lg, vertical = GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Body(stringResource(R.string.multiplayer_sent_time_left))
            Text(
                text = formatClock(secondsLeft ?: 0),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
        }
    }
}

/** What the result can do. */
internal data class ResultActions(
    val onPlayAgain: () -> Unit,
    val onBack: () -> Unit,
)

/** Duel and room results; scores and ranks appear only now (web ResultScreen). */
@Composable
internal fun ResultContent(
    game: GameState,
    result: GameResult,
    busy: Boolean,
    error: GameProblem?,
    actions: ResultActions,
    modifier: Modifier = Modifier,
) {
    val isDuel = game.isDuel && result.rows.size == 2
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        error?.let {
            MbAlert(
                type = MbAlertType.Warning,
                title = stringResource(R.string.multiplayer_error_title),
                message = problemText(it),
            )
        }
        HeaderCard(game = game, result = result, isDuel = isDuel)
        if (isDuel) DuelSection(result) else RoomSection(result)
        result.you?.let { SummaryCard(you = it, result = result) }
        ActionsCard(game = game, busy = busy, actions = actions)
    }
}

@Composable
private fun HeaderCard(
    game: GameState,
    result: GameResult,
    isDuel: Boolean,
) {
    val meta =
        buildList {
            game.organization?.let { add(stringResource(R.string.multiplayer_space, it.name)) }
            add(stringResource(R.string.multiplayer_questions, game.questionCount))
            add(stringResource(R.string.multiplayer_minutes, game.durationMinutes))
            if (!isDuel) add(stringResource(R.string.multiplayer_players_count, result.rows.size))
            game.closedAt?.let { add(formatDateTime(it)) }
        }
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_result_heading, modeLabel(game.mode), game.subjectName),
                style = MaterialTheme.typography.titleLarge,
                color = GotestTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            GotestChip(stringResource(R.string.multiplayer_status_finished), tone = GotestChipTone.Green)
        }
        Body(meta.joinToString(" • "))
    }
}

@Composable
private fun DuelSection(result: GameResult) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val outcome =
            GameRules.duelOutcome(result.outcome) ?: com.gotest.app.feature.multiplayer.domain.DuelOutcome.Draw
        val mine = result.rows.firstOrNull { it.isYou } ?: result.rows[0]
        val theirs = result.rows.firstOrNull { !it.isYou } ?: result.rows[1]
        MbDuelResultBanner(outcome = outcome, sameCorrect = mine.correct == theirs.correct)
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            DuelPanel(row = mine, winner = result.outcome == GameResult.WIN)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(GotestTheme.sizes.avatarLg)
                        .background(GotestTheme.colors.bgBrandDark, GotestTheme.shapes.full),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.multiplayer_result_versus),
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textOnBrand,
                )
            }
            DuelPanel(row = theirs, winner = result.outcome == GameResult.LOSE)
        }
        Body(stringResource(R.string.multiplayer_result_duel_note))
    }
}

@Composable
private fun DuelPanel(
    row: ResultRow,
    winner: Boolean,
) {
    val colors = GotestTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (winner) colors.bgGreenSoft else colors.bgSubtle, MaterialTheme.shapes.large)
                .padding(GotestTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        if (winner) {
            GotestChip(
                stringResource(R.string.multiplayer_result_winner),
                tone = GotestChipTone.Green,
                leadingIcon = GotestIcons.Bold.Award,
            )
        }
        Text(
            text = playerName(row.name, row.isYou),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xl)) {
            Stat(value = row.correct.toString(), label = stringResource(R.string.multiplayer_result_correct))
            Stat(
                value = formatClock(row.durationSeconds.toLong()),
                label = stringResource(R.string.multiplayer_result_total_time),
            )
        }
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = GotestTheme.colors.textPrimary)
        Body(label)
    }
}

@Composable
private fun RoomSection(result: GameResult) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val colors = GotestTheme.colors
        val champion = result.rows.firstOrNull()
        if (champion != null) {
            // Players tied for first all share the top spot (Aturan 8.14, web audit MB-08).
            val champions = result.rows.filter { it.rank == champion.rank }
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgYellowSoft, showBorder = false) {
                GotestChip(
                    text =
                        stringResource(
                            if (champions.size >
                                1
                            ) {
                                R.string.multiplayer_result_champion_shared
                            } else {
                                R.string.multiplayer_result_champion
                            },
                        ),
                    tone = GotestChipTone.White,
                    leadingIcon = GotestIcons.Bold.Award,
                )
                Text(
                    text = champions.map { playerName(it.name, it.isYou) }.joinToString(", "),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
                Text(
                    text =
                        stringResource(
                            R.string.multiplayer_result_champion_meta,
                            champion.correct,
                            formatClock(champion.durationSeconds.toLong()),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
        }
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.multiplayer_result_ranking),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            MbRankHeader()
            result.rows.forEach { row ->
                MbRankRow(
                    position = row.rank,
                    name = playerName(row.name, row.isYou),
                    correctLabel = stringResource(R.string.multiplayer_result_correct_count, row.correct),
                    timeLabel = formatClock(row.durationSeconds.toLong()),
                    kind =
                        when {
                            row.rank == 1 -> RankRowKind.Champion
                            row.isYou -> RankRowKind.You
                            else -> RankRowKind.Default
                        },
                )
            }
            Body(stringResource(R.string.multiplayer_result_ranking_note))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(
    you: ResultRow,
    result: GameResult,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.multiplayer_result_summary),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            MbStatTile(
                value = you.correct.toString(),
                label = stringResource(R.string.multiplayer_result_correct),
                container = colors.bgGreenSoft,
                content = colors.statusSuccess,
                modifier = Modifier.weight(1f),
            )
            MbStatTile(
                value = you.wrong.toString(),
                label = stringResource(R.string.multiplayer_result_wrong),
                container = colors.statusDangerSoft,
                content = colors.statusDanger,
                modifier = Modifier.weight(1f),
            )
            MbStatTile(
                value = you.empty.toString(),
                label = stringResource(R.string.multiplayer_result_empty),
                container = colors.bgYellowSoft,
                content = colors.bgYellowDark,
                modifier = Modifier.weight(1f),
            )
        }
        Row {
            Body(stringResource(R.string.multiplayer_result_total_time))
            Text(
                text = formatClock(you.durationSeconds.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
        PerQuestion(result)
        Body(stringResource(R.string.multiplayer_result_no_review))
    }
}

/** "Benar atau salah per soal": one square per question, in this player's order, with the legend. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerQuestion(result: GameResult) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.multiplayer_result_per_question),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            result.items.forEach { item ->
                val (container, description) =
                    when (item.status) {
                        GameResult.CORRECT -> {
                            colors.bgGreen to
                                stringResource(R.string.multiplayer_cd_outcome_correct, item.order)
                        }

                        GameResult.WRONG -> {
                            colors.statusDanger to
                                stringResource(R.string.multiplayer_cd_outcome_wrong, item.order)
                        }

                        else -> {
                            colors.bgYellow to stringResource(R.string.multiplayer_cd_outcome_empty, item.order)
                        }
                    }
                Box(
                    modifier =
                        Modifier
                            .size(GotestTheme.sizes.questionCell)
                            .background(container, MaterialTheme.shapes.small)
                            .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.order.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textOnBrand,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            Legend(colors.bgGreen, stringResource(R.string.multiplayer_result_correct))
            Legend(colors.statusDanger, stringResource(R.string.multiplayer_result_wrong))
            Legend(colors.bgYellow, stringResource(R.string.multiplayer_result_empty))
        }
    }
}

@Composable
private fun Legend(
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Box(modifier = Modifier.size(GotestTheme.sizes.badgeDot).background(color))
        Body(label)
    }
}

@Composable
private fun ActionsCard(
    game: GameState,
    busy: Boolean,
    actions: ResultActions,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.multiplayer_back_to_hub),
                onClick = actions.onBack,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            if (game.youAreHost) {
                GotestButton(
                    text =
                        stringResource(
                            if (busy) R.string.multiplayer_result_preparing else R.string.multiplayer_result_play_again,
                        ),
                    onClick = actions.onPlayAgain,
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                )
            } else {
                GotestButton(
                    text =
                        stringResource(
                            if (busy) R.string.multiplayer_result_joining else R.string.multiplayer_result_join_again,
                        ),
                    onClick = actions.onPlayAgain,
                    modifier = Modifier.weight(1f),
                    enabled = !busy && game.nextGameCode != null,
                )
            }
        }
        if (!game.youAreHost && game.nextGameCode == null) {
            Body(stringResource(R.string.multiplayer_result_waiting_next, game.hostName))
        }
        Body(
            stringResource(
                if (game.youAreHost) {
                    R.string.multiplayer_result_footer_host
                } else {
                    R.string.multiplayer_result_footer_guest
                },
            ) + " " + stringResource(R.string.multiplayer_result_footer_tail),
        )
    }
}
