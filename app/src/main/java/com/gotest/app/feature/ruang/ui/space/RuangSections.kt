package com.gotest.app.feature.ruang.ui.space

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.ui.catalog.StartProblem
import com.gotest.app.feature.practice.ui.catalog.problemText
import com.gotest.app.feature.ruang.domain.RuangPractice
import com.gotest.app.feature.ruang.ui.components.formatGrade
import com.gotest.app.feature.ruang.ui.components.serverWindow
import com.gotest.app.feature.tryout.domain.TryoutSession
import com.gotest.app.feature.tryout.ui.catalog.sessionActionLabel
import com.gotest.app.feature.tryout.ui.catalog.sessionStateLabel
import com.gotest.app.feature.tryout.ui.catalog.sessionTone

/** The Try Out sessions of the school, or the finished ones under "Sudah selesai" (web Ruang/Show SessionGroup). */
@Composable
internal fun SessionGroup(
    title: String,
    sessions: List<TryoutSession>,
    onOpen: (TryoutSession) -> Unit,
    modifier: Modifier = Modifier,
    empty: String? = null,
) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        if (sessions.isEmpty()) {
            empty?.let { EmptyText(it) }
        }
        sessions.forEach { session -> RuangSessionCard(session = session, onOpen = { onOpen(session) }) }
    }
}

@Composable
private fun RuangSessionCard(
    session: TryoutSession,
    onOpen: () -> Unit,
) {
    ItemBox {
        GotestChip(text = stringResource(sessionStateLabel(session.state)), tone = sessionTone(session.state))
        Text(text = session.title, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
        Text(
            text = session.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text =
                stringResource(
                    R.string.ruang_session_window,
                    serverWindow(session.startsAt),
                    serverWindow(session.endsAt),
                ),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        val score = session.score
        if (session.attemptDone && score != null) {
            val rank = session.rank
            Text(
                text =
                    if (rank != null && rank > 0) {
                        stringResource(
                            R.string.ruang_session_score_rank,
                            formatGrade(score),
                            rank,
                            session.totalParticipants ?: 0,
                        )
                    } else {
                        stringResource(R.string.ruang_session_score, formatGrade(score))
                    },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = GotestTheme.colors.textPrimary,
            )
        }
        GotestButton(
            text =
                stringResource(
                    if (session.state ==
                        TryoutSession.SCHEDULED
                    ) {
                        R.string.session_detail
                    } else {
                        sessionActionLabel(session)
                    },
                ),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().padding(top = GotestTheme.spacing.xs),
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

/** What a Latihan sekolah card can do, and its state on screen. */
internal data class PracticeCardState(
    val former: Boolean,
    val startingId: Long?,
    val error: Pair<Long, StartProblem>?,
)

/** Latihan sekolah given to the student, or worked on before (web Ruang/Show PracticeGroup). */
@Composable
internal fun PracticeGroup(
    packages: List<RuangPractice>,
    state: PracticeCardState,
    onAction: (RuangPractice, PracticeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.ruang_practice_title))
        if (packages.isEmpty()) EmptyText(stringResource(R.string.ruang_practice_empty))
        packages.forEach { item ->
            RuangPracticeCard(
                item = item,
                state = state,
                onAction = { action -> onAction(item, action) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuangPracticeCard(
    item: RuangPractice,
    state: PracticeCardState,
    onAction: (PracticeAction) -> Unit,
) {
    ItemBox {
        Text(text = item.name, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
        item.description?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        }
        Text(
            text =
                stringResource(
                    if (item.closed) R.string.ruang_practice_meta_closed else R.string.ruang_practice_meta,
                    item.questionCount,
                    timerLabel(item),
                ),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        item.lastScore?.let { score ->
            Text(
                text =
                    if (item.finishedCount > 1) {
                        stringResource(R.string.ruang_practice_last_count, formatGrade(score), item.finishedCount)
                    } else {
                        stringResource(R.string.ruang_practice_last, formatGrade(score))
                    },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = GotestTheme.colors.textPrimary,
            )
        }
        state.error?.takeIf { it.first == item.id }?.let { (_, problem) ->
            Text(
                text = problemText(problem),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.statusDanger,
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = GotestTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            PracticeButtons(item = item, state = state, onAction = onAction)
        }
    }
}

@Composable
private fun PracticeButtons(
    item: RuangPractice,
    state: PracticeCardState,
    onAction: (PracticeAction) -> Unit,
) {
    if (!state.former && item.continueAttemptId != null) {
        GotestButton(
            text = stringResource(R.string.ruang_practice_continue),
            onClick = { onAction(PracticeAction.Continue) },
            size = GotestButtonSize.Small,
        )
    }
    if (!state.former && item.canStart) {
        GotestButton(
            text =
                stringResource(
                    when {
                        state.startingId == item.id -> R.string.task_opening
                        item.finishedCount > 0 -> R.string.practice_again
                        else -> R.string.practice_start
                    },
                ),
            onClick = { onAction(PracticeAction.Start) },
            size = GotestButtonSize.Small,
            enabled = state.startingId == null,
        )
    }
    if (item.lastAttemptId != null) {
        GotestButton(
            text = stringResource(R.string.practice_review),
            onClick = { onAction(PracticeAction.Review) },
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

@Composable
private fun timerLabel(item: RuangPractice): String {
    val duration = item.durationSeconds
    return when {
        item.timerMode == RuangPractice.TIMER_PACKAGE && duration != null && duration > 0 -> {
            stringResource(R.string.ruang_practice_minutes, Math.round(duration / SECONDS_PER_MINUTE))
        }

        item.timerMode == RuangPractice.TIMER_PER_QUESTION -> {
            stringResource(R.string.ruang_practice_per_question)
        }

        else -> {
            stringResource(R.string.ruang_practice_untimed)
        }
    }
}

private const val SECONDS_PER_MINUTE = 60f

@Composable
internal fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
}

@Composable
private fun EmptyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** One bordered item inside a section card (web `rounded-control border border-line p-4`). */
@Composable
private fun ItemBox(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
    ) {
        content()
    }
}
