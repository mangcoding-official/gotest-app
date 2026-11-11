package com.gotest.app.feature.multiplayer.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.DuelOutcome

private data class BannerStyle(
    val container: Color,
    val accent: Color,
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
)

private fun GotestColors.bannerStyle(outcome: DuelOutcome): BannerStyle =
    when (outcome) {
        DuelOutcome.Win -> {
            BannerStyle(
                bgGreenSoft,
                statusSuccess,
                GotestIcons.Bold.Award,
                R.string.multiplayer_duel_win_title,
                R.string.multiplayer_duel_win_message,
            )
        }

        DuelOutcome.Lose -> {
            BannerStyle(
                statusDangerSoft,
                statusDanger,
                GotestIcons.Bold.CloseCircle,
                R.string.multiplayer_duel_lose_title,
                R.string.multiplayer_duel_lose_message,
            )
        }

        DuelOutcome.Draw -> {
            BannerStyle(
                statusWarningSoft,
                statusWarning,
                GotestIcons.Bold.MinusCircle,
                R.string.multiplayer_duel_draw_title,
                R.string.multiplayer_duel_draw_message,
            )
        }
    }

/** Headline of "Hasil Duel" ("MB/Duel Result Banner": Menang / Kalah / Seri). */
@Composable
fun MbDuelResultBanner(
    outcome: DuelOutcome,
    modifier: Modifier = Modifier,
) {
    val style = GotestTheme.colors.bannerStyle(outcome)
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(style.container, MaterialTheme.shapes.large)
                .padding(spacing.screenHorizontal)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(GotestTheme.sizes.dialogIcon)
                    .background(GotestTheme.colors.bgApp, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(style.icon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconXl),
                tint = style.accent,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(
                text = stringResource(style.title),
                style = MaterialTheme.typography.headlineMedium,
                color = style.accent,
            )
            Text(
                text = stringResource(style.message),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textPlaceholder,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MbDuelResultBannerPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            DuelOutcome.entries.forEach { MbDuelResultBanner(outcome = it) }
        }
    }
}
