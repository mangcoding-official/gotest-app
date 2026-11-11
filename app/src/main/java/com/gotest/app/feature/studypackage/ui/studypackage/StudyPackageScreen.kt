package com.gotest.app.feature.studypackage.ui.studypackage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.studypackage.ui.components.JenjangDropdown
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData

@Composable
fun StudyPackageRoute(
    onSearch: () -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    onTryoutClick: (packageId: String) -> Unit,
    onActivityClick: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyPackageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StudyPackageScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onSearch = onSearch,
        onCategoryClick = onCategoryClick,
        onTryoutClick = onTryoutClick,
        onActivityClick = onActivityClick,
        modifier = modifier,
    )
}

/**
 * Paket tab root. Latihan (frame 6284) keeps every block in one card; Tryout (frame 7315) splits
 * the controls from the list. Drawn under the floating bottom bar.
 */
@Composable
fun StudyPackageScreen(
    uiState: StudyPackageUiState,
    onEvent: (StudyPackageUiEvent) -> Unit,
    onSearch: () -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    onTryoutClick: (packageId: String) -> Unit,
    onActivityClick: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val isPractice = uiState.selectedTab == PackageType.Practice
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title =
                stringResource(
                    if (isPractice) R.string.studypackage_practice_title else R.string.studypackage_tryout_title,
                ),
            subtitle =
                stringResource(
                    if (isPractice) R.string.studypackage_practice_subtitle else R.string.studypackage_tryout_subtitle,
                ),
            style = GotestTopAppBarStyle.Leading,
            actions = {
                GotestIconButton(
                    icon = GotestIcons.Linear.SearchNormal,
                    contentDescription = stringResource(R.string.studypackage_cd_search),
                    onClick = onSearch,
                )
            },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = spacing.screenHorizontal,
                        end = spacing.screenHorizontal,
                        top = spacing.sm,
                        bottom = GotestTheme.sizes.bottomBarHeight + bottomInset + spacing.lg,
                    ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            val body: @Composable () -> Unit = {
                StudyPackageBody(
                    uiState = uiState,
                    onRetry = { onEvent(StudyPackageUiEvent.Retry) },
                    onCategoryClick = onCategoryClick,
                    onTryoutClick = onTryoutClick,
                    onActivityClick = onActivityClick,
                )
            }
            if (isPractice) {
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    StudyPackageControls(uiState = uiState, onEvent = onEvent)
                    body()
                }
            } else {
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    StudyPackageControls(uiState = uiState, onEvent = onEvent)
                }
                GotestCard(modifier = Modifier.fillMaxWidth()) { body() }
            }
        }
    }
}

/** Latihan / Tryout switch plus the "Semua Jenjang" dropdown. */
@Composable
private fun StudyPackageControls(
    uiState: StudyPackageUiState,
    onEvent: (StudyPackageUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = PackageType.entries
    val options = uiState.jenjangOptions
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        GotestSegmentedControl(
            options =
                tabs.map {
                    stringResource(
                        when (it) {
                            PackageType.Practice -> R.string.studypackage_tab_practice
                            PackageType.Tryout -> R.string.studypackage_tab_tryout
                        },
                    )
                },
            selectedIndex = tabs.indexOf(uiState.selectedTab),
            onSelect = { onEvent(StudyPackageUiEvent.TabSelected(tabs[it])) },
        )
        JenjangDropdown(
            options = options.map { it.name },
            selectedIndex = options.indexOfFirst { it.id == uiState.selectedJenjangId }.takeIf { it >= 0 },
            onSelect = { index -> onEvent(StudyPackageUiEvent.JenjangSelected(index?.let { options[it].id })) },
        )
    }
}

@Composable
private fun StudyPackageBody(
    uiState: StudyPackageUiState,
    onRetry: () -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    onTryoutClick: (packageId: String) -> Unit,
    onActivityClick: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    when (val content = uiState.content) {
        StudyPackageContent.Loading -> {
            GotestLoadingView(modifier = modifier.padding(vertical = spacing.xxxl))
        }

        StudyPackageContent.Error -> {
            GotestErrorView(onRetry = onRetry, modifier = modifier.fillMaxWidth())
        }

        StudyPackageContent.Empty -> {
            GotestEmptyView(
                icon = GotestIcons.Linear.TaskSquare,
                title = stringResource(R.string.studypackage_categories_empty_title),
                message = stringResource(R.string.studypackage_categories_empty_message),
                modifier = modifier.fillMaxWidth(),
            )
        }

        is StudyPackageContent.Success -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                content.categories.forEach { category ->
                    key(category.id) {
                        CategoryCard(
                            category = category,
                            type = uiState.selectedTab,
                            onClick = { onCategoryClick(category.id) },
                        )
                    }
                }
                when (uiState.selectedTab) {
                    PackageType.Practice -> {
                        RecentActivityBlock(activities = content.recentActivities, onActivityClick = onActivityClick)
                    }

                    PackageType.Tryout -> {
                        content.seasonalTryout?.let { seasonal ->
                            SeasonalTryoutBlock(
                                seasonal = seasonal,
                                countdown = uiState.countdown,
                                onTryoutClick = onTryoutClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val previewSuccess =
    StudyPackageContent.Success(
        categories = StudyPackagePreviewData.categories,
        recentActivities = StudyPackagePreviewData.history,
        seasonalTryout = StudyPackagePreviewData.seasonalTryout,
    )

@Composable
private fun StudyPackagePreview(uiState: StudyPackageUiState) {
    GotestTheme {
        StudyPackageScreen(
            uiState = uiState,
            onEvent = {},
            onSearch = {},
            onCategoryClick = {},
            onTryoutClick = {},
            onActivityClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun StudyPackagePracticePreview() {
    StudyPackagePreview(
        StudyPackageUiState(jenjangOptions = StudyPackagePreviewData.categories, content = previewSuccess),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun StudyPackageTryoutPreview() {
    StudyPackagePreview(
        StudyPackageUiState(
            selectedTab = PackageType.Tryout,
            jenjangOptions = StudyPackagePreviewData.categories,
            content = previewSuccess,
            countdown = Countdown(hours = 24, minutes = 41, seconds = 31),
        ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun StudyPackageLoadingPreview() {
    StudyPackagePreview(StudyPackageUiState())
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun StudyPackageEmptyPreview() {
    StudyPackagePreview(StudyPackageUiState(content = StudyPackageContent.Empty))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun StudyPackageErrorPreview() {
    StudyPackagePreview(StudyPackageUiState(content = StudyPackageContent.Error))
}
