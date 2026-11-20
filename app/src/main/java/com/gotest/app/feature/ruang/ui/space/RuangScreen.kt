package com.gotest.app.feature.ruang.ui.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.domain.MitraProgress
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.PlanHead
import com.gotest.app.feature.ruang.domain.PlanProgress
import com.gotest.app.feature.ruang.domain.ProgressAttempt
import com.gotest.app.feature.ruang.domain.ProgressSubject
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangPractice
import com.gotest.app.feature.ruang.domain.TodayTasks
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.tasks.PlanProgressSummary
import com.gotest.app.feature.ruang.ui.tasks.RecordDialog
import com.gotest.app.feature.ruang.ui.tasks.RoomTaskCard
import com.gotest.app.feature.ruang.ui.tasks.TaskActionState
import com.gotest.app.feature.ruang.ui.tasks.TaskEvent
import com.gotest.app.feature.ruang.ui.tasks.TaskNavigation
import com.gotest.app.feature.ruang.ui.tasks.TopicList
import com.gotest.app.feature.ruang.ui.tasks.handle
import com.gotest.app.feature.ruang.ui.tasks.previewProgress
import com.gotest.app.feature.ruang.ui.tasks.previewTasks
import com.gotest.app.feature.tryout.domain.TryoutSession

/** Where the Try Out and Latihan parts of the space lead. */
data class RuangLinks(
    val onOpenExams: (code: String) -> Unit,
    val onOpenPlan: (code: String) -> Unit,
    val onOpenPractice: (attemptId: Long, packageName: String) -> Unit,
    val onOpenPracticeResult: (attemptId: Long, packageName: String) -> Unit,
    val onOpenSession: (sessionId: Long) -> Unit,
    val onOpenTryoutResult: (attemptId: Long) -> Unit,
)

@Composable
fun RuangRoute(
    onBack: () -> Unit,
    links: RuangLinks,
    taskNavigation: TaskNavigation,
    modifier: Modifier = Modifier,
    viewModel: RuangViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by viewModel.tasks.state.collectAsStateWithLifecycle()
    // Back from a quiz or its result: the task shows its new status, as the web reloads the page.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ObserveAsEvents(viewModel.tasks.effects) { taskNavigation.handle(viewModel.code, it) }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is RuangEffect.OpenPractice -> links.onOpenPractice(effect.attemptId, effect.packageName)
            is RuangEffect.OpenPracticeResult -> links.onOpenPracticeResult(effect.attemptId, effect.packageName)
        }
    }
    RuangScreen(
        uiState = uiState,
        taskState = taskState,
        actions =
            RuangActions(
                onRetry = viewModel::retry,
                onBack = onBack,
                onOpenExams = { links.onOpenExams(viewModel.code) },
                onOpenPlan = { links.onOpenPlan(viewModel.code) },
                onTaskEvent = viewModel.tasks::onEvent,
                onPractice = viewModel::onPractice,
                // A finished session opens its result, the others the session page (web SessionGroup).
                onOpenSession = { session ->
                    val attemptId = session.attemptId
                    if (session.attemptDone && attemptId != null) {
                        links.onOpenTryoutResult(attemptId)
                    } else {
                        links.onOpenSession(session.id)
                    }
                },
            ),
        modifier = modifier,
    )
}

data class RuangActions(
    val onRetry: () -> Unit,
    val onBack: () -> Unit,
    val onOpenExams: () -> Unit,
    val onOpenPlan: () -> Unit = {},
    val onTaskEvent: (TaskEvent) -> Unit = {},
    val onPractice: (RuangPractice, PracticeAction) -> Unit = { _, _ -> },
    val onOpenSession: (TryoutSession) -> Unit = {},
)

@Composable
fun RuangScreen(
    uiState: RuangUiState,
    actions: RuangActions,
    modifier: Modifier = Modifier,
    taskState: TaskActionState = TaskActionState(),
) {
    val page = uiState.page
    RuangScaffold(
        title = page?.organization?.name ?: stringResource(R.string.ruang_kicker),
        subtitle = stringResource(R.string.ruang_kicker).takeIf { page != null },
        onBack = actions.onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(onRetry = actions.onRetry, modifier = Modifier.fillMaxSize().padding(padding))
            }

            else -> {
                RuangContent(
                    uiState = uiState,
                    page = page,
                    taskState = taskState,
                    actions = actions,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
    taskState.record?.let { RecordDialog(form = it, onEvent = actions.onTaskEvent) }
}

@Composable
private fun RuangContent(
    uiState: RuangUiState,
    page: RuangPage,
    taskState: TaskActionState,
    actions: RuangActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val name = page.organization.name
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text =
                if (page.former) {
                    stringResource(R.string.ruang_lede_former, name)
                } else {
                    stringResource(R.string.ruang_lede)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        page.today.plan?.let { plan ->
            TodayCard(
                plan = plan,
                today = page.today,
                family = uiState.family,
                taskState = taskState,
                actions = actions,
            )
        }
        if (uiState.hasExams) ExamsCard(name = name, former = page.former, onOpenExams = actions.onOpenExams)
        val (finished, upcoming) = page.sessions.partition { it.state == TryoutSession.CLOSED }
        if (!page.former) {
            SessionGroup(
                title = stringResource(R.string.ruang_tryout_title),
                sessions = upcoming,
                onOpen = actions.onOpenSession,
                empty = stringResource(R.string.ruang_tryout_empty),
            )
        }
        if (page.practice.isNotEmpty() || !page.former) {
            PracticeGroup(
                packages = page.practice,
                state = PracticeCardState(page.former, uiState.startingPracticeId, uiState.practiceError),
                onAction = actions.onPractice,
            )
        }
        if (finished.isNotEmpty()) {
            SessionGroup(
                title = stringResource(R.string.ruang_finished_title),
                sessions = finished,
                onOpen = actions.onOpenSession,
            )
        }
        if (page.progress.attempts.isNotEmpty() || page.planProgress != null) {
            ProgressCard(plan = page.planProgress, progress = page.progress, onOpenPlan = actions.onOpenPlan)
        }
    }
}

/** Tugas hari ini: today's tasks of the main plan, earlier ones not done yet, then the late ones (Ruang/Show). */
@Composable
private fun TodayCard(
    plan: PlanHead,
    today: TodayTasks,
    family: Boolean,
    taskState: TaskActionState,
    actions: RuangActions,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        CardHeader(
            title = stringResource(R.string.plan_today_title),
            subtitle = plan.title,
            onOpenPlan = actions.onOpenPlan,
        )
        if (today.tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.plan_today_empty),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        } else {
            today.tasks.forEach { task ->
                RoomTaskCard(
                    task = task,
                    family = family,
                    state = taskState,
                    onEvent = actions.onTaskEvent,
                )
            }
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    subtitle: String?,
    onOpenPlan: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
            }
        }
        GotestButton(
            text = stringResource(R.string.plan_open),
            onClick = onOpenPlan,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

@Composable
private fun ExamsCard(
    name: String,
    former: Boolean,
    onOpenExams: () -> Unit,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ruang_exams_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text =
                if (former) {
                    stringResource(R.string.ruang_exams_text_former, name)
                } else {
                    stringResource(R.string.ruang_exams_text, name)
                },
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        GotestButton(
            text = stringResource(R.string.ruang_exams_open),
            onClick = onOpenExams,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Perkembanganmu: the plan part, then the Try Out and Latihan progress of this Mitra (web Ruang/Show). */
@Composable
private fun ProgressCard(
    plan: PlanProgress?,
    progress: MitraProgress,
    onOpenPlan: () -> Unit,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.plan_progress_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        plan?.let { PlanPart(it, onOpenPlan) }
        if (plan != null && progress.attempts.isNotEmpty()) HorizontalDivider(color = GotestTheme.colors.borderSubtle)
        MitraProgressView(progress)
    }
}

@Composable
private fun PlanPart(
    progress: PlanProgress,
    onOpenPlan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progress.plan.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            GotestButton(
                text = stringResource(R.string.plan_open),
                onClick = onOpenPlan,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
        PlanProgressSummary(progress)
        if (progress.topics.isNotEmpty()) {
            Text(
                text = stringResource(R.string.plan_topics),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            TopicList(topics = progress.topics, emptyText = "")
        }
    }
}

private val previewActions = RuangActions(onRetry = {}, onBack = {}, onOpenExams = {})

private val previewPage =
    RuangPage(
        organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
        today = TodayTasks(PlanHead(31, "Persiapan PTS Semester 1"), previewTasks()),
        planProgress = previewProgress(),
        practice =
            listOf(
                RuangPractice(
                    601,
                    "Latihan Pecahan Kelas 8",
                    "Latihan pecahan untuk persiapan PTS.",
                    5,
                    canStart = true,
                ),
                RuangPractice(
                    602,
                    "Latihan Aljabar Kelas 8",
                    null,
                    5,
                    RuangPractice.TIMER_PACKAGE,
                    600,
                    canStart = true,
                    finishedCount = 2,
                    lastAttemptId = 903,
                    lastScore = 60.0,
                ),
            ),
        progress =
            MitraProgress(
                attempts =
                    listOf(
                        ProgressAttempt(
                            904,
                            "latihan",
                            "Latihan Bilangan Bulat",
                            "2026-10-02T08:10:00Z",
                            2,
                            5,
                            40,
                            40.0,
                        ),
                        ProgressAttempt(
                            953,
                            "simulasi",
                            "Try Out Tengah Semester",
                            "2026-10-11T02:40:00Z",
                            13,
                            20,
                            65,
                            640.0,
                        ),
                    ),
                subjects = listOf(ProgressSubject("Matematika", 8, 15, 53)),
            ),
    )

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun RuangScreenPreview() {
    GotestTheme {
        RuangScreen(
            uiState = RuangUiState(isLoading = false, page = previewPage, hasExams = true),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenTodayEmptyPreview() {
    GotestTheme {
        RuangScreen(
            uiState =
                RuangUiState(
                    isLoading = false,
                    page =
                        previewPage.copy(
                            today = TodayTasks(PlanHead(31, "Persiapan PTS Semester 1")),
                            planProgress = null,
                        ),
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenFormerPreview() {
    GotestTheme {
        RuangScreen(
            uiState =
                RuangUiState(
                    isLoading = false,
                    page = RuangPage(MitraRef("Bimbel Cerdas", "BCERDAS"), former = true),
                    hasExams = true,
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenLoadingPreview() {
    GotestTheme { RuangScreen(uiState = RuangUiState(), actions = previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun RuangScreenErrorPreview() {
    GotestTheme {
        RuangScreen(uiState = RuangUiState(isLoading = false, loadFailed = true), actions = previewActions)
    }
}
