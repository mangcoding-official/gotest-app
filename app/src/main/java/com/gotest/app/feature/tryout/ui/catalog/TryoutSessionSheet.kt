package com.gotest.app.feature.tryout.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.ui.catalog.shortDate
import com.gotest.app.feature.ruang.ui.components.serverWindow
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.TryoutSession

data class SessionCallbacks(
    val onRegister: () -> Unit,
    val onUnregister: () -> Unit,
    val onStart: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * One scheduled session (web SessionDetail): its times, participants, the student's status, the rules, the subtest
 * order, and one action: Daftar, Batalkan, Mulai or Lanjutkan, or Lihat Hasil; a finished one shows its ranking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailSheet(
    session: TryoutSession,
    sheet: SessionSheet,
    access: TryoutAccess,
    actions: SessionCallbacks,
    modifier: Modifier = Modifier,
) {
    GotestBottomSheet(onDismissRequest = actions.onClose, modifier = modifier) {
        SessionDetailContent(
            session = session,
            sheet = sheet,
            access = access,
            actions = actions,
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
        )
    }
}

/** The content of a session, in the catalog's sheet and on the session page (web SessionDetail). */
@Composable
fun SessionDetailContent(
    session: TryoutSession,
    sheet: SessionSheet,
    access: TryoutAccess,
    actions: SessionCallbacks,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        Text(
            text = stringResource(R.string.session_kicker, stringResource(sessionStateLabel(session.state))),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textBrandDark,
        )
        Text(text = session.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        session.description?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        SessionFacts(session)
        SessionRules(session)
        if (session.sections.isNotEmpty()) SessionOrder(session)
        sheet.message?.let { SheetNotice(it) }
        sheet.error?.let {
            Text(
                text = problemText(it, R.string.session_start_failed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.statusDanger,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.practice_close),
                onClick = actions.onClose,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            SessionAction(
                session = session,
                sheet = sheet,
                access = access,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
        }
        sheet.leaderboard?.let { SessionBoard(it) }
    }
}

@Composable
private fun SessionFacts(session: TryoutSession) {
    val participants =
        when {
            session.partner != null -> {
                stringResource(R.string.session_participants_partner, session.registeredCount)
            }

            session.capacity != null -> {
                stringResource(
                    R.string.session_participants_capacity,
                    session.registeredCount,
                    session.capacity,
                )
            }

            else -> {
                stringResource(R.string.session_participants_registered, session.registeredCount)
            }
        }
    val status =
        when {
            session.attemptDone -> R.string.session_status_done
            session.attemptId != null -> R.string.session_status_working
            session.registered -> R.string.session_status_registered
            else -> R.string.session_status_not_registered
        }
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            Fact(stringResource(R.string.session_starts), serverWindow(session.startsAt), Modifier.weight(1f))
            Fact(stringResource(R.string.session_ends), serverWindow(session.endsAt), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            Fact(stringResource(R.string.session_participants), participants, Modifier.weight(1f))
            Fact(stringResource(R.string.session_your_status), stringResource(status), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        showBorder = false,
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
    }
}

@Composable
private fun SessionRules(session: TryoutSession) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.session_rules),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        val rules =
            listOfNotNull(
                R.string.session_rule_register.takeIf { session.partner == null },
                R.string.session_rule_once,
                R.string.session_rule_stop,
                R.string.session_rule_ranking,
            )
        rules.forEach { rule ->
            Text(
                text = "• " + stringResource(rule),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SessionOrder(session: TryoutSession) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.session_order),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        session.sections.forEach { section ->
            Row {
                Text(
                    text = stringResource(R.string.session_order_item, section.orderNumber, section.title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                val minutes = section.durationSeconds / SECONDS_PER_MINUTE
                val rest = section.durationSeconds % SECONDS_PER_MINUTE
                Text(
                    text =
                        if (rest == 0) {
                            stringResource(R.string.session_duration_minutes, minutes)
                        } else {
                            stringResource(R.string.session_duration_both, minutes, rest)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textBrandDark,
                )
            }
        }
    }
}

/** Web SessionDetail `action`: the one button for this session and this account. */
@Composable
private fun SessionAction(
    session: TryoutSession,
    sheet: SessionSheet,
    access: TryoutAccess,
    actions: SessionCallbacks,
    modifier: Modifier = Modifier,
) {
    val (label, onClick) = sessionAction(session, sheet, access, actions)
    GotestButton(
        text = label,
        onClick = { onClick?.invoke() },
        modifier = modifier,
        variant =
            if (session.registered && session.state == TryoutSession.SCHEDULED &&
                session.partner == null
            ) {
                GotestButtonVariant.Plain
            } else {
                GotestButtonVariant.Filled
            },
        enabled = onClick != null && !sheet.busy,
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun sessionAction(
    session: TryoutSession,
    sheet: SessionSheet,
    access: TryoutAccess,
    actions: SessionCallbacks,
): Pair<String, (() -> Unit)?> {
    val premium = access.premiumLocked
    return when {
        session.attemptDone && session.attemptId != null -> {
            stringResource(R.string.tryout_see_result) to
                actions.onStart
        }

        session.state == TryoutSession.RUNNING -> {
            runningAction(session, sheet, access, actions)
        }

        session.state == TryoutSession.CLOSED -> {
            stringResource(R.string.session_finished) to null
        }

        session.partner != null -> {
            stringResource(R.string.session_starts_on, serverWindow(session.startsAt)) to null
        }

        session.registered -> {
            stringResource(R.string.session_unregister) to actions.onUnregister
        }

        premium -> {
            stringResource(R.string.practice_premium_only) to null
        }

        !(session.canRegister ?: access.canStart) -> {
            stringResource(access.blockedLabel ?: R.string.session_extend_register) to
                null
        }

        session.isFull -> {
            stringResource(R.string.session_full) to null
        }

        sheet.busy -> {
            stringResource(R.string.session_registering) to actions.onRegister
        }

        else -> {
            stringResource(R.string.session_register) to actions.onRegister
        }
    }
}

@Composable
private fun runningAction(
    session: TryoutSession,
    sheet: SessionSheet,
    access: TryoutAccess,
    actions: SessionCallbacks,
): Pair<String, (() -> Unit)?> =
    when {
        !session.registered -> {
            stringResource(R.string.session_closed_registration) to null
        }

        access.premiumLocked && session.attemptId == null -> {
            stringResource(R.string.practice_premium_only) to null
        }

        !access.canStart && session.attemptId == null -> {
            stringResource(access.blockedLabel ?: R.string.session_extend_start) to
                null
        }

        sheet.busy -> {
            stringResource(R.string.session_starting) to actions.onStart
        }

        session.attemptId != null -> {
            stringResource(R.string.session_continue) to actions.onStart
        }

        else -> {
            stringResource(R.string.session_start) to actions.onStart
        }
    }

/** The session ranking, open once the session ended (web LeaderboardPanel with `sessionId`). */
@Composable
private fun SessionBoard(board: SessionLeaderboard) {
    if (board.entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.tryout_leaderboard),
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        val rows =
            board.entries +
                listOfNotNull(board.userEntry?.takeIf { user -> board.entries.none { it.rank == user.rank } })
        rows.forEach { entry ->
            BoardLine(
                rank = entry.rank,
                name = entry.displayName,
                meta =
                    listOfNotNull(
                        shortDate(entry.submittedAt),
                        durationText(entry.durationUsed),
                    ).joinToString(" · "),
                score = entry.score,
            )
        }
    }
}

private const val SECONDS_PER_MINUTE = 60
