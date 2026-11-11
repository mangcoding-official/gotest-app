package com.gotest.app.feature.studypackage.ui.practicedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.studypackage.ui.components.DetailBottomBar
import com.gotest.app.feature.studypackage.ui.components.InfoTileData
import com.gotest.app.feature.studypackage.ui.components.NumberedItem
import com.gotest.app.feature.studypackage.ui.components.NumberedListCard
import com.gotest.app.feature.studypackage.ui.components.PackageHeroCard
import com.gotest.app.feature.studypackage.ui.components.RulesCard
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData
import com.gotest.app.feature.studypackage.ui.components.hasResumableProgress
import kotlinx.coroutines.launch

@Composable
fun PracticeDetailRoute(
    onBack: () -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            PracticeDetailEffect.ShowComingSoon -> scope.launch { snackbarHostState.showSnackbar(comingSoon) }
        }
    }
    PracticeDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onStartExam = onStartExam,
        modifier = modifier,
    )
}

/** "Detail Paket" (frame 2152). */
@Composable
fun PracticeDetailScreen(
    uiState: PracticeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (PracticeDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.studypackage_detail_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
                actions = {
                    GotestIconButton(
                        icon = GotestIcons.Linear.ArchiveAdd,
                        contentDescription = stringResource(R.string.studypackage_cd_bookmark),
                        onClick = { onEvent(PracticeDetailUiEvent.BookmarkClicked) },
                    )
                },
            )
        },
        bottomBar = {
            if (uiState is PracticeDetailUiState.Success) {
                PracticeBottomBar(detail = uiState.detail, onStartExam = onStartExam)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GotestTheme.colors.bgSubtle,
    ) { padding ->
        when (uiState) {
            PracticeDetailUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            PracticeDetailUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(PracticeDetailUiEvent.Retry) },
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
            }

            is PracticeDetailUiState.Success -> {
                PracticeDetailContent(detail = uiState.detail, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun PracticeDetailContent(
    detail: StudyPackageDetail,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val summary = detail.summary
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.lg,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PackageHeroCard(
            examLabel = summary.examLabel,
            typeLabel = stringResource(R.string.studypackage_chip_practice),
            typeTone = GotestChipTone.Violet,
            title = summary.title,
            description = summary.description,
            tiles =
                listOf(
                    InfoTileData(
                        icon = GotestIcons.Linear.Note2,
                        value = GotestFormat.number(summary.questionCount),
                        label = stringResource(R.string.studypackage_tile_questions),
                    ),
                    InfoTileData(
                        icon = GotestIcons.Linear.Calendar,
                        value =
                            summary.durationMinutes?.let { stringResource(R.string.studypackage_duration_minutes, it) }
                                ?: stringResource(R.string.studypackage_duration_untimed),
                        label = stringResource(R.string.studypackage_tile_duration),
                    ),
                    InfoTileData(
                        icon = GotestIcons.Linear.Award,
                        value = summary.scoringLabel,
                        label = stringResource(R.string.studypackage_tile_scoring),
                    ),
                ),
        )
        if (detail.topics.isNotEmpty()) {
            NumberedListCard(
                title = stringResource(R.string.studypackage_materials_title),
                items =
                    detail.topics.mapIndexed { index, topic ->
                        NumberedItem(
                            number = index + 1,
                            title = topic.name,
                            meta = stringResource(R.string.studypackage_question_count, topic.questionCount),
                        )
                    },
            )
        }
        if (detail.rules.isNotEmpty()) {
            RulesCard(title = stringResource(R.string.studypackage_rules_before_start), rules = detail.rules)
        }
    }
}

@Composable
private fun PracticeBottomBar(
    detail: StudyPackageDetail,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = detail.summary
    val progress = summary.progress?.takeIf { it.answered > 0 }
    DetailBottomBar(
        buttonText =
            stringResource(
                if (summary.hasResumableProgress) {
                    R.string.studypackage_detail_continue
                } else {
                    R.string.studypackage_detail_start
                },
            ),
        onClick = { onStartExam(summary.id) },
        modifier = modifier,
        progressText =
            progress?.let { stringResource(R.string.studypackage_last_progress_value, it.answered, it.total) },
    )
}

@Composable
private fun PracticeDetailPreview(uiState: PracticeDetailUiState) {
    GotestTheme {
        PracticeDetailScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onBack = {},
            onStartExam = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PracticeDetailResumePreview() {
    PracticeDetailPreview(PracticeDetailUiState.Success(StudyPackagePreviewData.practiceDetail))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PracticeDetailFreshPreview() {
    val detail = StudyPackagePreviewData.practiceDetail
    PracticeDetailPreview(PracticeDetailUiState.Success(detail.copy(summary = detail.summary.copy(progress = null))))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PracticeDetailLoadingPreview() {
    PracticeDetailPreview(PracticeDetailUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PracticeDetailErrorPreview() {
    PracticeDetailPreview(PracticeDetailUiState.Error)
}
