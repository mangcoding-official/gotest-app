package com.gotest.app.feature.ruang.ui.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamSchedule
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.MyExamStatus
import com.gotest.app.feature.ruang.domain.ScheduleState
import com.gotest.app.feature.ruang.ui.components.ExamStatusChips
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.components.formatGrade
import com.gotest.app.feature.ruang.ui.components.serverWindow

@Composable
fun ExamListRoute(
    onBack: () -> Unit,
    onAction: (code: String, action: ExamCardAction) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }
    ExamListScreen(
        uiState = uiState,
        onRetry = viewModel::load,
        onBack = onBack,
        onAction = { onAction(viewModel.code, it) },
        modifier = modifier,
    )
}

@Composable
fun ExamListScreen(
    uiState: ExamListUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onAction: (ExamCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    RuangScaffold(
        title = stringResource(R.string.exam_list_title),
        subtitle = page?.let { stringResource(R.string.exam_list_back, it.organization.name) },
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize().padding(padding))
            }

            else -> {
                ExamList(
                    page = page,
                    former = uiState.former,
                    onAction = onAction,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ExamList(
    page: ExamListPage,
    former: Boolean,
    onAction: (ExamCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Text(
                text = stringResource(if (former) R.string.exam_list_lede_former else R.string.exam_list_lede),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        if (page.schedules.isEmpty()) {
            item {
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.exam_list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GotestTheme.colors.textSecondary,
                    )
                }
            }
        }
        items(page.schedules, key = { it.id }) { schedule ->
            ExamScheduleCard(schedule = schedule, action = ExamCardAction.of(schedule, former), onAction = onAction)
        }
    }
}

@Composable
private fun ExamScheduleCard(
    schedule: ExamSchedule,
    action: ExamCardAction?,
    onAction: (ExamCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        ExamStatusChips(state = schedule.state, myStatus = schedule.myStatus)
        Text(text = schedule.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            text =
                stringResource(
                    R.string.exam_window,
                    serverWindow(schedule.opensAt),
                    serverWindow(schedule.closesAt),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.exam_duration, schedule.durationMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (schedule.myStatus == MyExamStatus.Submitted) {
            Text(
                text =
                    schedule.grade?.let { stringResource(R.string.exam_grade, formatGrade(it)) }
                        ?: stringResource(R.string.exam_grade_hidden),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
        }
        action?.let { ExamCardButton(action = it, onClick = { onAction(it) }) }
    }
}

@Composable
private fun ExamCardButton(
    action: ExamCardAction,
    onClick: () -> Unit,
) {
    val (label, variant) =
        when (action) {
            is ExamCardAction.Result -> {
                R.string.exam_action_result to GotestButtonVariant.Plain
            }

            is ExamCardAction.Continue -> {
                R.string.exam_action_continue to GotestButtonVariant.Filled
            }

            is ExamCardAction.Detail -> {
                if (action.running) {
                    R.string.exam_action_open to GotestButtonVariant.Filled
                } else {
                    R.string.exam_action_detail to GotestButtonVariant.Plain
                }
            }
        }
    GotestButton(text = stringResource(label), onClick = onClick, modifier = Modifier.fillMaxWidth(), variant = variant)
}

private val previewPage =
    ExamListPage(
        organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
        schedules =
            listOf(
                ExamSchedule(
                    41,
                    "PTS Matematika Kelas 8",
                    "2026-12-01T00:30:00+00:00",
                    "2026-12-01T03:30:00+00:00",
                    90,
                    ScheduleState.Running,
                    MyExamStatus.NotStarted,
                ),
                ExamSchedule(
                    40,
                    "Ulangan Bahasa Indonesia",
                    "2026-11-10T00:30:00+00:00",
                    "2026-11-10T03:00:00+00:00",
                    60,
                    ScheduleState.Closed,
                    MyExamStatus.Submitted,
                    7,
                    82.5,
                ),
            ),
    )

@Preview(showBackground = true)
@Composable
private fun ExamListScreenPreview() {
    GotestTheme {
        ExamListScreen(uiState = ExamListUiState(isLoading = false, page = previewPage), onRetry = {
        }, onBack = {}, onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamListScreenEmptyPreview() {
    GotestTheme {
        ExamListScreen(
            uiState =
                ExamListUiState(
                    isLoading = false,
                    page = previewPage.copy(schedules = emptyList()),
                    former = true,
                ),
            onRetry = {},
            onBack = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamListScreenErrorPreview() {
    GotestTheme {
        ExamListScreen(uiState = ExamListUiState(isLoading = false), onRetry = {}, onBack = {}, onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamListScreenLoadingPreview() {
    GotestTheme {
        ExamListScreen(uiState = ExamListUiState(), onRetry = {}, onBack = {}, onAction = {})
    }
}
