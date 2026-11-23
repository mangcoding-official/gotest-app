package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.ui.MainBarengUiState
import com.gotest.app.feature.multiplayer.ui.components.MbCodeInput
import com.gotest.app.feature.multiplayer.ui.components.MbInfoTile
import com.gotest.app.feature.multiplayer.ui.components.formatDateTime
import com.gotest.app.feature.multiplayer.ui.components.gameTitle
import com.gotest.app.feature.multiplayer.ui.components.modeLabel

private const val HISTORY_PREVIEW = 5

/** The hub of a space (web Hub.tsx): what Main Bareng is, create, join, the game the viewer is in, the history. */
@Composable
internal fun HubContent(
    uiState: MainBarengUiState,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
    onResume: () -> Unit,
    onViewResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        IntroCard(organizationName = uiState.organizationName)
        CreateCard(onCreate = onCreate)
        JoinCard(onJoin = onJoin)
        uiState.active?.let { ActiveCard(game = it, onResume = onResume) }
        ModeInfo(
            label = stringResource(R.string.multiplayer_rules_duel_heading),
            title = stringResource(R.string.multiplayer_rules_duel_title),
            rows =
                listOf(
                    R.string.multiplayer_rules_players to R.string.multiplayer_rules_duel_players,
                    R.string.multiplayer_rules_start to R.string.multiplayer_rules_duel_start,
                    R.string.multiplayer_rules_winner to R.string.multiplayer_rules_duel_winner,
                ),
        )
        ModeInfo(
            label = stringResource(R.string.multiplayer_rules_room_heading),
            title = stringResource(R.string.multiplayer_rules_room_title),
            rows =
                listOf(
                    R.string.multiplayer_rules_players to R.string.multiplayer_rules_room_players,
                    R.string.multiplayer_rules_start to R.string.multiplayer_rules_room_start,
                    R.string.multiplayer_rules_ranking to R.string.multiplayer_rules_room_ranking,
                ),
        )
        HistoryCard(
            history = uiState.history,
            organizationName = uiState.organizationName,
            onViewResult = onViewResult,
        )
    }
}

@Composable
private fun IntroCard(organizationName: String?) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        GotestChip(
            text =
                organizationName?.let { stringResource(R.string.multiplayer_intro_pill_space, it) }
                    ?: stringResource(R.string.multiplayer_title),
            tone = GotestChipTone.Violet,
            leadingIcon = GotestIcons.Bold.People,
        )
        Text(
            text = stringResource(R.string.multiplayer_intro_title),
            style = MaterialTheme.typography.headlineSmall,
            color = GotestTheme.colors.textPrimary,
        )
        val body = stringResource(R.string.multiplayer_intro_body)
        Body(organizationName?.let { stringResource(R.string.multiplayer_intro_space, it) + " " + body } ?: body)
    }
}

@Composable
private fun CreateCard(onCreate: () -> Unit) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_create_title),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            GotestChip(
                text = stringResource(R.string.multiplayer_premium),
                tone = GotestChipTone.Yellow,
                leadingIcon = GotestIcons.Bold.Award,
            )
        }
        Body(stringResource(R.string.multiplayer_create_body))
        GotestButton(
            text = stringResource(R.string.multiplayer_create_title),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JoinCard(onJoin: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.multiplayer_join_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Body(stringResource(R.string.multiplayer_join_body))
        MbCodeInput(value = code, onValueChange = { code = GameRules.normalizeCode(it) }, onDone = { onJoin(code) })
        GotestButton(
            text = stringResource(R.string.multiplayer_join_action),
            onClick = { onJoin(code) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            enabled = code.isEmpty() || code.length == GameRules.CODE_LENGTH,
        )
    }
}

@Composable
private fun ActiveCard(
    game: GameState,
    onResume: () -> Unit,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgBrandSoft) {
        val title =
            gameTitle(game.mode, game.subjectName) +
                (game.organization?.let { " • " + stringResource(R.string.multiplayer_space, it.name) } ?: "")
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        GotestChip(
            text =
                stringResource(
                    if (game.running) R.string.multiplayer_status_running else R.string.multiplayer_status_waiting,
                ),
            tone = if (game.running) GotestChipTone.Green else GotestChipTone.Yellow,
        )
        Body(stringResource(R.string.multiplayer_active_body, game.code))
        GotestButton(
            text = stringResource(R.string.multiplayer_back_to_game),
            onClick = onResume,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModeInfo(
    label: String,
    title: String,
    rows: List<Pair<Int, Int>>,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textBrandDark)
        GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgSubtle, showBorder = false) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            rows.forEach { (term, value) ->
                Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                    Text(
                        text = stringResource(term),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.width(GotestTheme.sizes.avatarLg * 2),
                    )
                    Text(
                        text = stringResource(value),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    history: List<GameHistoryItem>,
    organizationName: String?,
    onViewResult: (String) -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    var detailCode by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_history_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (history.size > HISTORY_PREVIEW) {
                GotestButton(
                    text =
                        stringResource(
                            if (showAll) {
                                R.string.multiplayer_history_show_less
                            } else {
                                R.string.multiplayer_history_see_all
                            },
                        ),
                    onClick = { showAll = !showAll },
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Small,
                )
            }
        }
        val body = stringResource(R.string.multiplayer_history_body)
        Body(organizationName?.let { stringResource(R.string.multiplayer_history_space, it) + " " + body } ?: body)
        if (history.isEmpty()) {
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgSubtle, showBorder = false) {
                Text(
                    text = stringResource(R.string.multiplayer_history_empty_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                Body(stringResource(R.string.multiplayer_history_empty_message))
            }
        } else {
            (if (showAll) history else history.take(HISTORY_PREVIEW)).forEach { item ->
                HistoryRow(
                    item = item,
                    onOpen = { if (item.finished) onViewResult(item.code) else detailCode = item.code },
                )
            }
        }
    }
    history.firstOrNull { it.code == detailCode }?.let { item ->
        CancelledDetail(item = item, onClose = { detailCode = null })
    }
}

@Composable
private fun HistoryRow(
    item: GameHistoryItem,
    onOpen: () -> Unit,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.xs) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModeChip(item.mode)
            Column(modifier = Modifier.weight(1f).padding(start = GotestTheme.spacing.sm)) {
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = historyMeta(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GotestButton(
                text =
                    stringResource(
                        if (item.finished) {
                            R.string.multiplayer_history_see_result
                        } else {
                            R.string.multiplayer_history_detail
                        },
                    ),
                onClick = onOpen,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutcomeChip(item)
            Text(
                text = formatDateTime(item.closedAt),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
internal fun ModeChip(mode: String) {
    GotestChip(
        text = modeLabel(mode),
        tone = if (mode == GameState.MODE_DUEL) GotestChipTone.Violet else GotestChipTone.Blue,
    )
}

@Composable
private fun OutcomeChip(item: GameHistoryItem) {
    when {
        !item.finished -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_cancelled), tone = GotestChipTone.Red)
        }

        item.outcome == GameResult.WIN -> {
            GotestChip(
                stringResource(R.string.multiplayer_outcome_won),
                tone = GotestChipTone.Green,
                leadingIcon = GotestIcons.Bold.Award,
            )
        }

        item.outcome == GameResult.DRAW -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_draw), tone = GotestChipTone.Yellow)
        }

        item.outcome == GameResult.LOSE -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_lost), tone = GotestChipTone.Grey)
        }

        else -> {
            GotestChip(
                stringResource(R.string.multiplayer_outcome_ranked, item.rank?.toString() ?: "-", item.playerCount),
                tone = GotestChipTone.Violet,
            )
        }
    }
}

/** Web `historyMeta`: "10 soal • 10 menit • vs Rina" or "• 5 pemain"; a cancelled game shows no players. */
@Composable
private fun historyMeta(item: GameHistoryItem): String {
    val parts =
        mutableListOf(
            stringResource(R.string.multiplayer_questions, item.questionCount),
            stringResource(R.string.multiplayer_minutes, item.durationMinutes),
        )
    if (item.finished) {
        parts +=
            item.opponentName?.takeIf { item.mode == GameState.MODE_DUEL }?.let {
                stringResource(R.string.multiplayer_versus, it)
            } ?: stringResource(R.string.multiplayer_players_count, item.playerCount)
    }
    return parts.joinToString(" • ")
}

/** "Detail" of a cancelled game (web DetailBody). */
@Composable
private fun CancelledDetail(
    item: GameHistoryItem,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip(item.mode)
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = GotestTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                GotestChip(stringResource(R.string.multiplayer_outcome_cancelled), tone = GotestChipTone.Red)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_question_count),
                    value = stringResource(R.string.multiplayer_questions, item.questionCount),
                    modifier = Modifier.weight(1f),
                )
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_duration),
                    value = stringResource(R.string.multiplayer_minutes, item.durationMinutes),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_code),
                    value = item.code,
                    modifier = Modifier.weight(1f),
                )
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_cancelled_at),
                    value = formatDateTime(item.closedAt),
                    modifier = Modifier.weight(1f),
                )
            }
            Body(stringResource(R.string.multiplayer_detail_cancelled_body))
            GotestButton(
                text = stringResource(R.string.multiplayer_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun HubContentPreview() {
    GotestTheme {
        HubContent(
            uiState =
                MainBarengUiState(
                    loading = false,
                    history =
                        listOf(
                            GameHistoryItem(
                                "HX1A2B",
                                "duel",
                                "finished",
                                "Penalaran Umum",
                                10,
                                10,
                                2,
                                "Rina Ayu",
                                "win",
                            ),
                            GameHistoryItem("HX5E6F", "duel", "cancelled", "Penalaran Umum", 10, 10, 1),
                        ),
                ),
            onCreate = {},
            onJoin = {},
            onResume = {},
            onViewResult = {},
        )
    }
}
