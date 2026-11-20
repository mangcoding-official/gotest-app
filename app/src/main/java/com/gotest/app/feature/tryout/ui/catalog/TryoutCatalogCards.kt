package com.gotest.app.feature.tryout.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.ui.catalog.CatalogCard
import com.gotest.app.feature.practice.ui.catalog.FooterFact
import com.gotest.app.feature.practice.ui.catalog.shortDate
import com.gotest.app.feature.ruang.ui.components.serverWindow
import com.gotest.app.feature.tryout.domain.TryoutPackage
import com.gotest.app.feature.tryout.domain.TryoutSession
import com.gotest.app.feature.tryout.domain.TryoutWindow
import kotlin.math.roundToInt

/** A Try Out package (web TryoutPackageCard): subtests, window, its button, last score, and ranking. */
@Composable
fun TryoutPackageCard(
    item: TryoutPackage,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogCard(modifier = modifier, footer = { RankFooter(item.rank, item.totalParticipants, item.lastFinishedAt) }) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            if (item.premium) GotestChip(text = stringResource(R.string.practice_premium), tone = GotestChipTone.Yellow)
        }
        Text(
            text = stringResource(R.string.tryout_sections, item.sections.size),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        windowNotice(item.window)?.let {
            Text(text = it, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = stringResource(cardLabel(item)),
                onClick = onStart,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
            Box(modifier = Modifier.weight(1f))
            val score = item.lastScore
            if (item.finishedCount > 0 && score != null) ScoreBadge(score)
        }
    }
}

/** The card's button (web TryoutPackageCard `label`). A Premium package opens the start sheet (A2c-3). */
fun cardLabel(item: TryoutPackage): Int {
    val windowLabel =
        when (item.windowState) {
            TryoutWindow.STATE_CLOSED -> R.string.tryout_closed
            TryoutWindow.STATE_UPCOMING -> R.string.tryout_upcoming
            else -> null
        }
    val toPremium =
        item.premiumLocked && item.continueAttemptId == null && item.resultOnlyId == null && windowLabel == null
    return when {
        item.continueAttemptId != null -> R.string.tryout_continue
        item.resultOnlyId != null -> R.string.tryout_see_result
        windowLabel != null -> windowLabel
        toPremium -> R.string.practice_premium_only
        item.finishedCount > 0 -> R.string.tryout_again
        else -> R.string.tryout_start
    }
}

@Composable
fun ScoreBadge(
    score: Double,
    modifier: Modifier = Modifier,
) {
    GotestChip(
        text = stringResource(R.string.tryout_score, score.roundToInt()),
        tone = GotestChipTone.Green,
        modifier = modifier,
    )
}

/** Ranking and when it was done; the ranking shows only once it is open and ranked (DEC-064, DEC-065). */
@Composable
fun RankFooter(
    rank: Int?,
    total: Int?,
    date: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        val rankText =
            when {
                rank == null -> stringResource(R.string.practice_none)
                total != null -> stringResource(R.string.tryout_rank_of, rank, total)
                else -> stringResource(R.string.tryout_rank, rank)
            }
        FooterFact(stringResource(R.string.tryout_ranking), rankText, Modifier.weight(1f))
        FooterFact(
            stringResource(R.string.tryout_done_at),
            shortDate(date) ?: stringResource(R.string.practice_none),
            Modifier.weight(1f),
        )
    }
}

/** Web `windowNotice`: when the package opens, closed, or runs until. */
@Composable
fun windowNotice(window: TryoutWindow?): String? =
    when (window?.state) {
        null, TryoutWindow.STATE_NONE -> {
            null
        }

        TryoutWindow.STATE_UPCOMING -> {
            stringResource(R.string.tryout_window_opens, window.opensAt?.let(::serverWindow).orEmpty())
        }

        TryoutWindow.STATE_CLOSED -> {
            stringResource(R.string.tryout_window_closed, window.closesAt?.let(::serverWindow).orEmpty())
        }

        else -> {
            window.closesAt?.let { stringResource(R.string.tryout_window_until, serverWindow(it)) }
                ?: stringResource(R.string.tryout_window_running)
        }
    }

/** One session of Seasonal Try Out (web SessionCard). */
@Composable
fun SessionCard(
    session: TryoutSession,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reason = blockedReason(session)
    CatalogCard(modifier = modifier, footer = {
        RankFooter(session.rank, session.totalParticipants, session.submittedAt)
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            GotestChip(text = stringResource(sessionStateLabel(session.state)), tone = sessionTone(session.state))
            if (session.premium) {
                GotestChip(
                    text = stringResource(R.string.practice_premium),
                    tone = GotestChipTone.Yellow,
                )
            }
        }
        Text(text = session.title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(
            text =
                if (session.state == TryoutSession.SCHEDULED) {
                    stringResource(R.string.session_starts_at, serverWindow(session.startsAt))
                } else {
                    stringResource(R.string.session_until, serverWindow(session.endsAt))
                },
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = stringResource(if (reason != null) R.string.session_detail else sessionActionLabel(session)),
                onClick = onOpen,
                variant = if (reason != null) GotestButtonVariant.Plain else GotestButtonVariant.Filled,
                size = GotestButtonSize.Small,
            )
            Box(modifier = Modifier.weight(1f))
            val score = session.score
            if (session.attemptDone && score != null) ScoreBadge(score)
        }
    }
}

fun sessionStateLabel(state: String): Int =
    when (state) {
        TryoutSession.SCHEDULED -> R.string.session_state_scheduled
        TryoutSession.RUNNING -> R.string.session_state_running
        else -> R.string.session_state_closed
    }

private fun sessionTone(state: String): GotestChipTone =
    when (state) {
        TryoutSession.RUNNING -> GotestChipTone.Green
        TryoutSession.SCHEDULED -> GotestChipTone.Violet
        else -> GotestChipTone.Grey
    }

/** Why nothing can be done with a session; null when there is an action (web `blockedReason`). */
fun blockedReason(session: TryoutSession): Int? =
    when {
        session.attemptDone -> {
            null
        }

        session.state == TryoutSession.CLOSED -> {
            R.string.session_blocked_closed
        }

        session.state == TryoutSession.RUNNING && !session.registered -> {
            R.string.session_blocked_registration
        }

        session.state == TryoutSession.SCHEDULED && !session.registered && session.isFull -> {
            R.string.session_blocked_full
        }

        else -> {
            null
        }
    }

/** Web `sessionActionLabel`. */
fun sessionActionLabel(session: TryoutSession): Int =
    when {
        session.attemptDone -> R.string.session_action_result
        session.state == TryoutSession.RUNNING && !session.registered -> R.string.session_action_closed_registration
        session.state == TryoutSession.RUNNING && session.attemptId != null -> R.string.session_action_continue
        session.state == TryoutSession.RUNNING && session.premiumLocked -> R.string.practice_premium_only
        session.state == TryoutSession.RUNNING -> R.string.session_action_start_now
        session.state == TryoutSession.CLOSED -> R.string.session_action_finished
        session.registered -> R.string.session_action_registered
        session.premiumLocked -> R.string.practice_premium_only
        session.isFull -> R.string.session_action_full
        else -> R.string.session_action_register
    }
