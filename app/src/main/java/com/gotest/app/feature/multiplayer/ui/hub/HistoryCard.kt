package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome
import com.gotest.app.feature.multiplayer.ui.components.MbGameTypeChip
import com.gotest.app.feature.multiplayer.ui.components.MbOutcomeChip
import com.gotest.app.feature.multiplayer.ui.components.MbSeatList
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.playedAtLabel
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel

/** Rows the hub card shows; "Lihat Semua" opens the rest (frame 12410). */
private const val MAX_ROWS = 4

/** "Riwayat Main Bareng": the newest games with their result chip. */
@Composable
fun HistoryCard(
    history: List<GameHistoryItem>,
    onOpenResult: (code: String) -> Unit,
    onSeeAll: () -> Unit,
    onDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.multiplayer_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.multiplayer_history_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            if (history.isNotEmpty()) {
                GotestButton(
                    text = stringResource(R.string.multiplayer_history_see_all),
                    onClick = onSeeAll,
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Small,
                )
            }
        }
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.multiplayer_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            MbSeatList {
                history.take(MAX_ROWS).forEachIndexed { index, item ->
                    if (index > 0) GotestDivider()
                    HistoryRow(
                        item = item,
                        onAction = { if (item.hasResult) onOpenResult(item.code) else onDetail(item.code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: GameHistoryItem,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier = modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MbGameTypeChip(type = item.settings.type)
            MbOutcomeChip(outcome = item.outcome, highlighted = true)
        }
        Column {
            Text(text = item.settings.subtest, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Text(text = historyMeta(item), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = playedAtLabel(item.playedAt),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            GotestButton(
                text =
                    stringResource(
                        if (item.hasResult) {
                            R.string.multiplayer_history_see_result
                        } else {
                            R.string.multiplayer_history_detail
                        },
                    ),
                onClick = onAction,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
    }
}

/** "20 soal • 15 menit • vs Ardy Pratama", "15 soal • 20 menit • 6 pemain" or "20 soal • 30 menit". */
@Composable
@ReadOnlyComposable
private fun historyMeta(item: GameHistoryItem): String {
    val count = questionCountLabel(item.settings.questionCount)
    val duration = durationLabel(item.settings.durationMinutes)
    val opponent = item.opponentName
    return when {
        item.outcome == HistoryOutcome.Cancelled -> {
            stringResource(R.string.multiplayer_meta_two, count, duration)
        }

        opponent != null -> {
            stringResource(R.string.multiplayer_meta_versus, count, duration, opponent)
        }

        else -> {
            stringResource(
                R.string.multiplayer_meta_three,
                count,
                duration,
                pluralStringResource(R.plurals.multiplayer_player_count, item.playerCount, item.playerCount),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryCardPreview() {
    GotestTheme {
        HistoryCard(
            history = MultiplayerPreviewData.history,
            onOpenResult = {},
            onSeeAll = {},
            onDetail = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}
