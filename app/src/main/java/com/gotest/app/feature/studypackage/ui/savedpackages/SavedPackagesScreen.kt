package com.gotest.app.feature.studypackage.ui.savedpackages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.studypackage.domain.SavedPackage
import com.gotest.app.feature.studypackage.ui.components.PackageSnackbarHost
import com.gotest.app.feature.studypackage.ui.components.PracticeDetailSheet
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData
import kotlinx.coroutines.launch

@Composable
fun SavedPackagesRoute(
    onBack: () -> Unit,
    onOpenTryout: (packageId: String) -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedPackagesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.studypackage_unsaved_message)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is SavedPackagesEffect.OpenTryout -> {
                onOpenTryout(effect.packageId)
            }

            is SavedPackagesEffect.StartExam -> {
                onStartExam(effect.packageId)
            }

            SavedPackagesEffect.PackageRemoved -> {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(removedMessage)
                }
            }
        }
    }
    SavedPackagesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

/** "Paket Tersimpan" (frame 22723) with the "Detail Latihan" sheet. */
@Composable
fun SavedPackagesScreen(
    uiState: SavedPackagesUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SavedPackagesUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.studypackage_saved_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        snackbarHost = {
            PackageSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            )
        },
        containerColor = GotestTheme.colors.bgSubtle,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = padding.calculateTopPadding() + spacing.sm,
                    bottom = padding.calculateBottomPadding() + spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "filters") {
                SavedPackagesFilters(
                    selectedType = uiState.selectedType,
                    totalCount = uiState.totalCount,
                    onSelect = { onEvent(SavedPackagesUiEvent.TypeSelected(it)) },
                    // 8dp list spacing + 4dp: the design leaves 12dp under the pills.
                    modifier = Modifier.padding(bottom = spacing.xs),
                )
            }
            savedPackagesContent(content = uiState.content, onEvent = onEvent)
        }
        // The sheet is its own window; it sits here only to keep one root emitter.
        uiState.sheet?.let { sheet ->
            PracticeDetailSheet(
                state = sheet,
                onDismiss = { onEvent(SavedPackagesUiEvent.SheetDismissed) },
                onStart = { onEvent(SavedPackagesUiEvent.SheetStartClicked(it)) },
                onRetry = { onEvent(SavedPackagesUiEvent.SheetRetry) },
            )
        }
    }
}

private fun LazyListScope.savedPackagesContent(
    content: SavedPackagesContent,
    onEvent: (SavedPackagesUiEvent) -> Unit,
) {
    when (content) {
        SavedPackagesContent.Loading -> {
            item(key = "loading") {
                GotestLoadingView(modifier = Modifier.fillMaxWidth().padding(vertical = GotestTheme.spacing.xxxl))
            }
        }

        SavedPackagesContent.Error -> {
            item(key = "error") {
                GotestErrorView(onRetry = { onEvent(SavedPackagesUiEvent.Retry) }, modifier = Modifier.fillMaxWidth())
            }
        }

        SavedPackagesContent.Empty -> {
            item(key = "empty") {
                GotestEmptyView(
                    icon = GotestIcons.Linear.ArchiveTick,
                    title = stringResource(R.string.studypackage_saved_empty_title),
                    message = stringResource(R.string.studypackage_saved_empty_message),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        is SavedPackagesContent.Success -> {
            items(content.packages, key = { it.studyPackage.id }) { saved ->
                SavedPackageCard(
                    saved = saved,
                    onUnsave = { onEvent(SavedPackagesUiEvent.UnsaveClicked(saved.studyPackage.id)) },
                    onStart = { onEvent(SavedPackagesUiEvent.StartClicked(saved.studyPackage)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** "Semua (3)", "Latihan" and "Tryout". */
@Composable
private fun SavedPackagesFilters(
    selectedType: PackageType?,
    totalCount: Int?,
    onSelect: (PackageType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        GotestFilterPill(
            text =
                totalCount?.let { stringResource(R.string.studypackage_saved_filter_all, it) }
                    ?: stringResource(R.string.studypackage_filter_all),
            selected = selectedType == null,
            onClick = { onSelect(null) },
        )
        PackageType.entries.forEach { type ->
            GotestFilterPill(
                text =
                    stringResource(
                        when (type) {
                            PackageType.Practice -> R.string.studypackage_type_practice
                            PackageType.Tryout -> R.string.studypackage_type_tryout
                        },
                    ),
                selected = type == selectedType,
                onClick = { onSelect(type) },
            )
        }
    }
}

/** Exam and type chips with the unsave bookmark, title, meta line and an outlined "Mulai …" button. */
@Composable
private fun SavedPackageCard(
    saved: SavedPackage,
    onUnsave: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val pkg = saved.studyPackage
    val isPractice = pkg.type == PackageType.Practice
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestChip(text = pkg.examLabel, tone = examChipTone(pkg.examLabel))
            // This frame colours "Tryout" yellow, unlike the green chip on Detail Tryout.
            GotestChip(
                text =
                    stringResource(
                        if (isPractice) R.string.studypackage_chip_practice else R.string.studypackage_chip_tryout,
                    ),
                tone = if (isPractice) GotestChipTone.Violet else GotestChipTone.Yellow,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(GotestIcons.Bold.ArchiveTick),
                contentDescription = stringResource(R.string.studypackage_cd_unbookmark),
                modifier =
                    Modifier
                        .size(GotestTheme.sizes.iconMd)
                        .clip(GotestTheme.shapes.full)
                        .clickable(role = Role.Button, onClick = onUnsave),
                tint = colors.iconDefault,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(text = pkg.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text =
                    stringResource(
                        R.string.studypackage_saved_meta,
                        pkg.questionCount,
                        pkg.durationMinutes?.let { stringResource(R.string.studypackage_saved_duration, it) }
                            ?: stringResource(R.string.studypackage_duration_untimed),
                        GotestFormat.mediumDate(saved.savedOn),
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        // The design's "outline" button: the text variant inside a brand-dark hairline.
        GotestButton(
            text =
                stringResource(
                    if (isPractice) {
                        R.string.studypackage_action_start_practice
                    } else {
                        R.string.studypackage_action_start_tryout
                    },
                ),
            onClick = onStart,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(GotestTheme.sizes.strokeThin, colors.borderBrandDark),
                        MaterialTheme.shapes.small,
                    ),
            variant = GotestButtonVariant.Text,
            size = GotestButtonSize.Medium,
        )
    }
}

@Composable
private fun SavedPackagesPreview(uiState: SavedPackagesUiState) {
    GotestTheme {
        SavedPackagesScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SavedPackagesSuccessPreview() {
    val packages = StudyPackagePreviewData.savedPackages
    SavedPackagesPreview(
        SavedPackagesUiState(totalCount = packages.size, content = SavedPackagesContent.Success(packages)),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SavedPackagesLoadingPreview() {
    SavedPackagesPreview(SavedPackagesUiState())
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SavedPackagesEmptyPreview() {
    SavedPackagesPreview(SavedPackagesUiState(totalCount = 0, content = SavedPackagesContent.Empty))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SavedPackagesErrorPreview() {
    SavedPackagesPreview(SavedPackagesUiState(content = SavedPackagesContent.Error))
}
