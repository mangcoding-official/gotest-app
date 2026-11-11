package com.gotest.app.feature.studypackage.ui.tryoutdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch

@Composable
fun TryoutDetailRoute(
    onBack: () -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TryoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            TryoutDetailEffect.ShowComingSoon -> scope.launch { snackbarHostState.showSnackbar(comingSoon) }
        }
    }
    TryoutDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onStartExam = onStartExam,
        modifier = modifier,
    )
}

/** "Detail Tryout" (frame 7674). */
@Composable
fun TryoutDetailScreen(
    uiState: TryoutDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TryoutDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.studypackage_tryout_detail_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
                actions = {
                    GotestIconButton(
                        icon = GotestIcons.Linear.ArchiveAdd,
                        contentDescription = stringResource(R.string.studypackage_cd_bookmark),
                        onClick = { onEvent(TryoutDetailUiEvent.BookmarkClicked) },
                    )
                },
            )
        },
        bottomBar = {
            if (uiState is TryoutDetailUiState.Success) {
                DetailBottomBar(
                    buttonText = stringResource(R.string.studypackage_tryout_start),
                    onClick = { onStartExam(uiState.detail.summary.id) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GotestTheme.colors.bgSubtle,
    ) { padding ->
        when (uiState) {
            TryoutDetailUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            TryoutDetailUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(TryoutDetailUiEvent.Retry) },
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
            }

            is TryoutDetailUiState.Success -> {
                TryoutDetailContent(detail = uiState.detail, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun TryoutDetailContent(
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
            typeLabel = stringResource(R.string.studypackage_chip_tryout),
            typeTone = GotestChipTone.Green,
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
                        icon = GotestIcons.Linear.MenuBoard,
                        value = GotestFormat.number(detail.sections.size),
                        label = stringResource(R.string.studypackage_tile_sections),
                    ),
                ),
            titleStyle = MaterialTheme.typography.headlineMedium,
        )
        if (detail.sections.isNotEmpty()) {
            NumberedListCard(
                title = stringResource(R.string.studypackage_sections_title),
                items =
                    detail.sections.map { section ->
                        NumberedItem(
                            number = section.order,
                            title = section.name,
                            meta =
                                stringResource(
                                    R.string.studypackage_section_meta,
                                    section.questionCount,
                                    section.durationMinutes,
                                ),
                        )
                    },
                headerChip = stringResource(R.string.studypackage_sections_server_time),
                itemStyle = MaterialTheme.typography.bodySmall,
            )
        }
        if (detail.rules.isNotEmpty()) {
            RulesCard(
                title = stringResource(R.string.studypackage_tryout_rules_title),
                rules = detail.rules,
                showBorder = true,
            )
        }
    }
}

@Composable
private fun TryoutDetailPreview(uiState: TryoutDetailUiState) {
    GotestTheme {
        TryoutDetailScreen(
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
private fun TryoutDetailSuccessPreview() {
    TryoutDetailPreview(TryoutDetailUiState.Success(StudyPackagePreviewData.tryoutDetail))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TryoutDetailLoadingPreview() {
    TryoutDetailPreview(TryoutDetailUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TryoutDetailErrorPreview() {
    TryoutDetailPreview(TryoutDetailUiState.Error)
}
