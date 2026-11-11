package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome

/** "Duel" (violet) or "Room" (blue) chip of a history row. */
@Composable
fun MbGameTypeChip(
    type: GameType,
    modifier: Modifier = Modifier,
) {
    GotestChip(
        text = gameTypeLabel(type),
        modifier = modifier,
        tone = if (type == GameType.Duel) GotestChipTone.Violet else GotestChipTone.Blue,
    )
}

/**
 * Result chip of a history row. The hub card [highlighted]s a win with the award icon and a
 * ranking in violet (frame 12410); the full list keeps both plain, the ranking grey (frame 23813).
 */
@Composable
fun MbOutcomeChip(
    outcome: HistoryOutcome,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    when (outcome) {
        HistoryOutcome.Won -> {
            GotestChip(
                text = stringResource(R.string.multiplayer_outcome_won),
                modifier = modifier,
                tone = GotestChipTone.Green,
                leadingIcon = if (highlighted) GotestIcons.Bold.Award else null,
            )
        }

        HistoryOutcome.Lost -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_lost), modifier, GotestChipTone.Red)
        }

        HistoryOutcome.Draw -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_draw), modifier, GotestChipTone.Yellow)
        }

        is HistoryOutcome.Ranked -> {
            GotestChip(
                text = stringResource(R.string.multiplayer_outcome_ranked, outcome.rank, outcome.players),
                modifier = modifier,
                tone = if (highlighted) GotestChipTone.Violet else GotestChipTone.Grey,
            )
        }

        HistoryOutcome.Cancelled -> {
            GotestChip(stringResource(R.string.multiplayer_outcome_cancelled), modifier, GotestChipTone.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MbHistoryChipsPreview() {
    val outcomes =
        listOf(
            HistoryOutcome.Won,
            HistoryOutcome.Ranked(rank = 2, players = 6),
            HistoryOutcome.Draw,
            HistoryOutcome.Cancelled,
            HistoryOutcome.Lost,
        )
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                MbGameTypeChip(type = GameType.Duel)
                MbGameTypeChip(type = GameType.Room)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                outcomes.take(2).forEach { MbOutcomeChip(outcome = it, highlighted = true) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                outcomes.forEach { MbOutcomeChip(outcome = it) }
            }
        }
    }
}
