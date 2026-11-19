package com.gotest.app.feature.ruang.ui.plan

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.PlanProgress
import com.gotest.app.feature.ruang.domain.RoomPlan
import com.gotest.app.feature.ruang.domain.RoomWeek
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.tasks.HistoryList
import com.gotest.app.feature.ruang.ui.tasks.PlanProgressSummary
import com.gotest.app.feature.ruang.ui.tasks.RecordDialog
import com.gotest.app.feature.ruang.ui.tasks.RoomTaskCard
import com.gotest.app.feature.ruang.ui.tasks.TaskActionState
import com.gotest.app.feature.ruang.ui.tasks.TaskEvent
import com.gotest.app.feature.ruang.ui.tasks.TaskNavigation
import com.gotest.app.feature.ruang.ui.tasks.TopicList
import com.gotest.app.feature.ruang.ui.tasks.handle
import com.gotest.app.feature.ruang.ui.tasks.longDate
import com.gotest.app.feature.ruang.ui.tasks.planState
import com.gotest.app.feature.ruang.ui.tasks.previewProgress
import com.gotest.app.feature.ruang.ui.tasks.previewTasks
import com.gotest.app.feature.ruang.ui.tasks.shortDate
import com.gotest.app.feature.ruang.ui.tasks.weekRangeText

@Composable
fun PlanRoute(
    onBack: () -> Unit,
    taskNavigation: TaskNavigation,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taskState by viewModel.tasks.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    ObserveAsEvents(viewModel.tasks.effects) { taskNavigation.handle(viewModel.code, it) }
    PlanScreen(
        uiState = uiState,
        actions =
            PlanActions(
                onBack = onBack,
                onRetry = viewModel::retry,
                onChoose = viewModel::choose,
                onTaskEvent = viewModel.tasks::onEvent,
            ),
        modifier = modifier,
        taskState = taskState,
        practiceOpens = taskNavigation.onOpenPractice != null,
    )
}

data class PlanActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onChoose: (Long) -> Unit = {},
    val onTaskEvent: (TaskEvent) -> Unit = {},
)

@Composable
fun PlanScreen(
    uiState: PlanUiState,
    actions: PlanActions,
    modifier: Modifier = Modifier,
    taskState: TaskActionState = TaskActionState(),
    practiceOpens: Boolean = false,
) {
    val page = uiState.page
    RuangScaffold(
        title = stringResource(R.string.plan_title),
        subtitle = page?.let { stringResource(R.string.plan_back, it.organization.name) },
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
                PlanContent(
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
private fun PlanContent(
    uiState: PlanUiState,
    page: StudyPlanPage,
    taskState: TaskActionState,
    actions: PlanActions,
    practiceOpens: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val plan = uiState.plan
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
                    stringResource(R.string.plan_lede_former, page.organization.name)
                } else {
                    stringResource(R.string.plan_lede)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        if (page.plans.size > 1 && plan != null) {
            PlanPicker(plans = page.plans, chosen = plan, onChoose = actions.onChoose)
        }
        if (plan == null) {
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.plan_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        } else {
            // The progress belongs to one plan: the main plan, or the plan of the latest result.
            PlanHeaderCard(
                plan = plan,
                former = page.former,
                progress = page.planProgress?.takeIf { it.plan.id == plan.id },
                family = uiState.family,
            )
            plan.weeks.forEach { week ->
                WeekCard(
                    week = week,
                    family = uiState.family,
                    taskState = taskState,
                    onTaskEvent = actions.onTaskEvent,
                    practiceOpens = practiceOpens,
                )
            }
        }
    }
}

/** "Pilih rencana", with "(rencana utama)" after the main plan (Ruang/Plan select). */
@Composable
private fun PlanPicker(
    plans: List<RoomPlan>,
    chosen: RoomPlan,
    onChoose: (Long) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        GotestTextField(
            value = choiceLabel(chosen),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.plan_choose),
            trailingContent = {
                Icon(
                    painter = painterResource(GotestIcons.Linear.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconSm),
                    tint = GotestTheme.colors.iconDefault,
                )
            },
            readOnly = true,
        )
        Box(modifier = Modifier.matchParentSize().clickable(role = Role.DropdownList) { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text(choiceLabel(plan)) },
                    onClick = {
                        onChoose(plan.id)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun choiceLabel(plan: RoomPlan): String =
    if (plan.isPrimary) stringResource(R.string.plan_choice_primary, plan.title) else plan.title

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanHeaderCard(
    plan: RoomPlan,
    former: Boolean,
    progress: PlanProgress?,
    family: Boolean,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            Text(text = plan.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            val (state, tone) = planState(plan.state)
            GotestChip(text = stringResource(state), tone = tone)
            if (plan.isPrimary) GotestChip(text = stringResource(R.string.plan_primary), tone = GotestChipTone.Green)
        }
        Text(text = examLine(plan), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        if (!plan.isPrimary && !former) {
            Text(
                text = stringResource(R.string.plan_today_from_primary),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        progress?.let { ProgressPart(it, family) }
    }
}

@Composable
private fun ProgressPart(
    progress: PlanProgress,
    family: Boolean,
) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        PlanProgressSummary(progress)
        Text(
            text = stringResource(R.string.plan_topics),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        TopicList(topics = progress.topics, emptyText = stringResource(R.string.plan_topics_empty))
        Column {
            Text(
                text = stringResource(R.string.plan_history_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            weekRangeText(progress.week)?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        HistoryList(
            history = progress.history,
            emptyText = stringResource(R.string.plan_history_empty),
            family = family,
        )
    }
}

/** "PTS Semester 1 · Ujian 6 Desember 2026 · Minggu 2 dari 4". */
@Composable
private fun examLine(plan: RoomPlan): String {
    val target = plan.examTarget?.let { stringResource(R.string.plan_exam_target, it) }.orEmpty()
    val weeks =
        if (plan.state == RoomPlan.STATE_RUNNING && plan.currentWeek != null) {
            stringResource(R.string.plan_week_of, plan.currentWeek, plan.weekCount)
        } else {
            stringResource(R.string.plan_weeks, plan.weekCount)
        }
    return stringResource(R.string.plan_exam_line, target, longDate(plan.examDate), weeks)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekCard(
    week: RoomWeek,
    family: Boolean,
    taskState: TaskActionState,
    onTaskEvent: (TaskEvent) -> Unit,
    practiceOpens: Boolean,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (week.isCurrent) {
                        Modifier.border(
                            GotestTheme.sizes.strokeThin,
                            colors.borderBrandDark,
                            MaterialTheme.shapes.large,
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.plan_week, week.number),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.plan_week_range, shortDate(week.startsOn), shortDate(week.endsOn)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            if (week.isCurrent) {
                GotestChip(
                    text = stringResource(R.string.plan_week_current),
                    tone = GotestChipTone.Violet,
                )
            }
        }
        if (week.tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.plan_week_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            week.tasks.forEach { task ->
                RoomTaskCard(
                    task = task,
                    family = family,
                    state = taskState,
                    onEvent = onTaskEvent,
                    practiceOpens = practiceOpens,
                )
            }
        }
    }
}

private val previewActions = PlanActions(onBack = {}, onRetry = {})

private val previewPlan =
    RoomPlan(
        id = 31,
        title = "Persiapan PTS Semester 1",
        examTarget = "PTS Semester 1",
        startsOn = "2026-11-09",
        examDate = "2026-12-06",
        state = RoomPlan.STATE_RUNNING,
        isPrimary = true,
        weekCount = 4,
        currentWeek = 2,
        weeks =
            listOf(
                RoomWeek(1, "2026-11-09", "2026-11-15", tasks = emptyList()),
                RoomWeek(2, "2026-11-16", "2026-11-22", isCurrent = true, tasks = previewTasks()),
            ),
    )

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun PlanScreenPreview() {
    GotestTheme {
        PlanScreen(
            uiState =
                PlanUiState(
                    isLoading = false,
                    page =
                        StudyPlanPage(
                            organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
                            primaryPlanId = 31,
                            plans =
                                listOf(
                                    previewPlan,
                                    previewPlan.copy(
                                        id = 32,
                                        title = "Persiapan Ujian Akhir",
                                        isPrimary = false,
                                        state = "upcoming",
                                    ),
                                ),
                            planProgress = previewProgress(),
                        ),
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanScreenEmptyPreview() {
    GotestTheme {
        PlanScreen(
            uiState =
                PlanUiState(
                    isLoading = false,
                    page = StudyPlanPage(MitraRef("Bimbel Cerdas", "BCERDAS"), former = true),
                ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanScreenLoadingPreview() {
    GotestTheme { PlanScreen(uiState = PlanUiState(), actions = previewActions) }
}

@Preview(showBackground = true)
@Composable
private fun PlanScreenErrorPreview() {
    GotestTheme { PlanScreen(uiState = PlanUiState(isLoading = false, loadFailed = true), actions = previewActions) }
}
