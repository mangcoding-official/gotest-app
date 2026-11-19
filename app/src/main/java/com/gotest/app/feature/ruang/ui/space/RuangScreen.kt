package com.gotest.app.feature.ruang.ui.space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.PlanHead
import com.gotest.app.feature.ruang.domain.PlanProgress
import com.gotest.app.feature.ruang.domain.RuangPage
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

@Composable
fun RuangRoute(
    onBack: () -> Unit,
    onOpenExams: (String) -> Unit,
    onOpenPlan: (String) -> Unit,
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
    RuangScreen(
        uiState = uiState,
        taskState = taskState,
        actions =
            RuangActions(
                onRetry = viewModel::retry,
                onBack = onBack,
                onOpenExams = { onOpenExams(viewModel.code) },
                onOpenPlan = { onOpenPlan(viewModel.code) },
                onTaskEvent = viewModel.tasks::onEvent,
            ),
        modifier = modifier,
        practiceOpens = taskNavigation.onOpenPractice != null,
    )
}

data class RuangActions(
    val onRetry: () -> Unit,
    val onBack: () -> Unit,
    val onOpenExams: () -> Unit,
    val onOpenPlan: () -> Unit = {},
    val onTaskEvent: (TaskEvent) -> Unit = {},
)

@Composable
fun RuangScreen(
    uiState: RuangUiState,
    actions: RuangActions,
    modifier: Modifier = Modifier,
    taskState: TaskActionState = TaskActionState(),
    practiceOpens: Boolean = false,
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
                    practiceOpens = practiceOpens,
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
    practiceOpens: Boolean,
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
                practiceOpens = practiceOpens,
            )
        }
        if (uiState.hasExams) ExamsCard(name = name, former = page.former, onOpenExams = actions.onOpenExams)
        page.planProgress?.let { ProgressCard(progress = it, onOpenPlan = actions.onOpenPlan) }
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
    practiceOpens: Boolean,
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
                    practiceOpens = practiceOpens,
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

/** Perkembanganmu: the plan part (Try Out and Latihan progress join with A2c). */
@Composable
private fun ProgressCard(
    progress: PlanProgress,
    onOpenPlan: () -> Unit,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.plan_progress_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
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
