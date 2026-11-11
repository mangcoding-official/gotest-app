package com.gotest.app.feature.studypackage.ui.packagelist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestPackageCard
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.studypackage.ui.components.CardSegment
import com.gotest.app.feature.studypackage.ui.components.CardSegmentPosition
import com.gotest.app.feature.studypackage.ui.components.PracticeDetailSheet
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData
import com.gotest.app.feature.studypackage.ui.components.practiceAction

@Composable
fun PackageListRoute(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSavedPackages: () -> Unit,
    onTryoutClick: (packageId: String) -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackageListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is PackageListEffect.OpenTryout -> onTryoutClick(effect.packageId)
            is PackageListEffect.StartExam -> onStartExam(effect.packageId)
        }
    }
    PackageListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onSearch = onSearch,
        onSavedPackages = onSavedPackages,
        modifier = modifier,
    )
}

/** "Paket Belajar" (frames 1758 / 7858) with the "Detail Latihan" sheet. */
@Composable
fun PackageListScreen(
    uiState: PackageListUiState,
    onEvent: (PackageListUiEvent) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSavedPackages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = stringResource(R.string.studypackage_list_title),
            subtitle = stringResource(R.string.studypackage_list_subtitle),
            style = GotestTopAppBarStyle.Leading,
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                GotestIconButton(
                    icon = GotestIcons.Linear.ArchiveTick,
                    contentDescription = stringResource(R.string.studypackage_cd_saved_packages),
                    onClick = onSavedPackages,
                )
                GotestIconButton(
                    icon = GotestIcons.Linear.SearchNormal,
                    contentDescription = stringResource(R.string.studypackage_cd_search),
                    onClick = onSearch,
                )
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl + bottomInset,
                ),
        ) {
            item(key = "controls") {
                CardSegment(position = CardSegmentPosition.Top) {
                    PackageListControls(
                        uiState = uiState,
                        onEvent = onEvent,
                        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
                    )
                }
            }
            packageListContent(content = uiState.content, onEvent = onEvent)
            item(key = "bottom") {
                CardSegment(position = CardSegmentPosition.Bottom, modifier = Modifier.height(spacing.lg))
            }
        }
        // The sheet is its own window; it sits here only to keep one root emitter.
        uiState.sheet?.let { sheet ->
            PracticeDetailSheet(
                state = sheet,
                onDismiss = { onEvent(PackageListUiEvent.SheetDismissed) },
                onStart = { onEvent(PackageListUiEvent.SheetStartClicked(it)) },
                onRetry = { onEvent(PackageListUiEvent.SheetRetry) },
            )
        }
    }
}

private fun LazyListScope.packageListContent(
    content: PackageListContent,
    onEvent: (PackageListUiEvent) -> Unit,
) {
    when (content) {
        PackageListContent.Loading -> {
            item(key = "loading") {
                CardSegment(position = CardSegmentPosition.Middle) {
                    GotestLoadingView(modifier = Modifier.padding(vertical = GotestTheme.spacing.xxxl))
                }
            }
        }

        PackageListContent.Error -> {
            item(key = "error") {
                CardSegment(position = CardSegmentPosition.Middle) {
                    GotestErrorView(
                        onRetry = { onEvent(PackageListUiEvent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        PackageListContent.Empty -> {
            item(key = "empty") {
                CardSegment(position = CardSegmentPosition.Middle) {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.TaskSquare,
                        title = stringResource(R.string.studypackage_packages_empty_title),
                        message = stringResource(R.string.studypackage_packages_empty_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is PackageListContent.Success -> {
            itemsIndexed(content.packages, key = { _, pkg -> pkg.id }) { _, pkg ->
                CardSegment(position = CardSegmentPosition.Middle) {
                    PackageItem(
                        pkg = pkg,
                        onAction = { onEvent(PackageListUiEvent.PackageActionClicked(pkg)) },
                        modifier =
                            Modifier.padding(
                                start = GotestTheme.spacing.lg,
                                end = GotestTheme.spacing.lg,
                                top = GotestTheme.spacing.md,
                            ),
                    )
                }
            }
        }
    }
}

/** "Latihan" / "Tryout" switch and the level filter pills. */
@Composable
private fun PackageListControls(
    uiState: PackageListUiState,
    onEvent: (PackageListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val types = PackageType.entries
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        GotestSegmentedControl(
            options =
                types.map {
                    stringResource(
                        when (it) {
                            PackageType.Practice -> R.string.studypackage_type_practice
                            PackageType.Tryout -> R.string.studypackage_type_tryout
                        },
                    )
                },
            selectedIndex = types.indexOf(uiState.selectedType),
            onSelect = { onEvent(PackageListUiEvent.TypeSelected(types[it])) },
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GotestFilterPill(
                text = stringResource(R.string.studypackage_filter_all),
                selected = uiState.selectedLevel == null,
                onClick = { onEvent(PackageListUiEvent.LevelSelected(null)) },
            )
            uiState.levels.forEach { level ->
                GotestFilterPill(
                    text = level,
                    selected = level == uiState.selectedLevel,
                    onClick = { onEvent(PackageListUiEvent.LevelSelected(level)) },
                )
            }
        }
    }
}

@Composable
private fun PackageItem(
    pkg: StudyPackage,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionRes =
        when (pkg.type) {
            PackageType.Practice -> pkg.practiceAction().labelRes
            PackageType.Tryout -> R.string.studypackage_action_start_tryout
        }
    GotestPackageCard(
        chipText = pkg.examLabel,
        title = pkg.title,
        description = pkg.description,
        progress = pkg.progress?.fraction,
        actionText = stringResource(actionRes),
        onAction = onAction,
        modifier = modifier.fillMaxWidth(),
        chipTone = examChipTone(pkg.examLabel),
    )
}

private val previewLevels = listOf("UTBK-SNBT", "SMA", "CPNS", "SMP")

@Composable
private fun PackageListPreview(uiState: PackageListUiState) {
    GotestTheme {
        PackageListScreen(uiState = uiState, onEvent = {}, onBack = {}, onSearch = {}, onSavedPackages = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageListSuccessPreview() {
    PackageListPreview(
        PackageListUiState(
            levels = previewLevels,
            content = PackageListContent.Success(StudyPackagePreviewData.packages),
        ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageListLoadingPreview() {
    PackageListPreview(PackageListUiState(levels = previewLevels))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageListEmptyPreview() {
    PackageListPreview(
        PackageListUiState(levels = previewLevels, selectedLevel = "CPNS", content = PackageListContent.Empty),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageListErrorPreview() {
    PackageListPreview(PackageListUiState(levels = previewLevels, content = PackageListContent.Error))
}
