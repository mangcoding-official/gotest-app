package com.gotest.app.feature.studypackage.ui.subjectlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.CategoryAccent
import com.gotest.app.core.model.Subject
import com.gotest.app.feature.studypackage.ui.components.AccentBar
import com.gotest.app.feature.studypackage.ui.components.CardMetaLine
import com.gotest.app.feature.studypackage.ui.components.CardSegment
import com.gotest.app.feature.studypackage.ui.components.CardSegmentPosition
import com.gotest.app.feature.studypackage.ui.components.JenjangDropdown
import com.gotest.app.feature.studypackage.ui.components.NestedCard
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData

@Composable
fun SubjectListRoute(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSubjectClick: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubjectListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubjectListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onSearch = onSearch,
        onSubjectClick = onSubjectClick,
        modifier = modifier,
    )
}

/** Subjects of one category (frame 6652, "SLTA Sederajat"). */
@Composable
fun SubjectListScreen(
    uiState: SubjectListUiState,
    onEvent: (SubjectListUiEvent) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSubjectClick: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val header =
        when (uiState) {
            is SubjectListUiState.Success -> uiState.header
            is SubjectListUiState.Empty -> uiState.header
            else -> null
        }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = header?.category?.name ?: stringResource(R.string.studypackage_subjects_subtitle),
            subtitle = header?.let { stringResource(R.string.studypackage_subjects_subtitle) },
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
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
            // No spacedBy(): it would open grey gaps inside the segmented list card.
            if (header != null) {
                item(key = "jenjang") {
                    JenjangSwitchCard(
                        header = header,
                        onEvent = onEvent,
                        modifier = Modifier.padding(bottom = spacing.sm),
                    )
                }
            }
            subjectListContent(uiState = uiState, onEvent = onEvent, onSubjectClick = onSubjectClick)
        }
    }
}

private fun LazyListScope.subjectListContent(
    uiState: SubjectListUiState,
    onEvent: (SubjectListUiEvent) -> Unit,
    onSubjectClick: (subjectId: String) -> Unit,
) {
    when (uiState) {
        SubjectListUiState.Loading -> {
            item(key = "loading") {
                GotestLoadingView(modifier = Modifier.padding(vertical = GotestTheme.spacing.xxxl))
            }
        }

        SubjectListUiState.Error -> {
            item(key = "error") {
                GotestErrorView(onRetry = { onEvent(SubjectListUiEvent.Retry) }, modifier = Modifier.fillMaxWidth())
            }
        }

        is SubjectListUiState.Empty -> {
            item(key = "empty") {
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.BookOpened,
                        title = stringResource(R.string.studypackage_subjects_empty_title),
                        message = stringResource(R.string.studypackage_subjects_empty_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is SubjectListUiState.Success -> {
            subjectCards(
                subjects = uiState.subjects,
                accent = uiState.header.category.accent,
                onSubjectClick = onSubjectClick,
            )
        }
    }
}

/** All subjects inside one white container card, drawn slice by slice. */
private fun LazyListScope.subjectCards(
    subjects: List<Subject>,
    accent: CategoryAccent,
    onSubjectClick: (subjectId: String) -> Unit,
) {
    item(key = "list_top") {
        CardSegment(position = CardSegmentPosition.Top, modifier = Modifier.height(GotestTheme.spacing.lg))
    }
    itemsIndexed(subjects, key = { _, subject -> subject.id }) { index, subject ->
        CardSegment(position = CardSegmentPosition.Middle) {
            SubjectCard(
                subject = subject,
                accent = accent,
                onClick = { onSubjectClick(subject.id) },
                modifier =
                    Modifier.padding(
                        start = GotestTheme.spacing.lg,
                        end = GotestTheme.spacing.lg,
                        top = if (index == 0) GotestTheme.spacing.none else GotestTheme.spacing.md,
                    ),
            )
        }
    }
    item(key = "list_bottom") {
        CardSegment(position = CardSegmentPosition.Bottom, modifier = Modifier.height(GotestTheme.spacing.lg))
    }
}

/** The design's "Semua Jenjang" field; here it switches between categories (jenjang). */
@Composable
private fun JenjangSwitchCard(
    header: SubjectListHeader,
    onEvent: (SubjectListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = header.categories
    GotestCard(modifier = modifier.fillMaxWidth()) {
        JenjangDropdown(
            options = categories.map { it.name },
            selectedIndex = categories.indexOfFirst { it.id == header.category.id }.takeIf { it >= 0 },
            onSelect = { index ->
                index?.let { onEvent(SubjectListUiEvent.CategorySelected(categories[it].id)) }
            },
            allOptionLabel = null,
        )
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    accent: CategoryAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    NestedCard(
        onClick = onClick,
        footer = {
            CardMetaLine(
                start = stringResource(R.string.studypackage_subject_count, subject.subjectCount),
                end = stringResource(R.string.studypackage_category_practice_count, subject.packageCount),
            )
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            AccentBar(accent = accent)
            Text(
                text = subject.name,
                style = MaterialTheme.typography.bodyLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = subject.description,
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SubjectProgress(
            completed = subject.completedPackages,
            total = subject.totalPackages,
            modifier = Modifier.padding(spacing.lg),
        )
    }
}

/** Green bar in a bordered track plus "Dikerjakan … 0 dari 24". */
@Composable
private fun SubjectProgress(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Surface(
            shape = GotestTheme.shapes.full,
            color = colors.bgSubtle,
            border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
        ) {
            Box(modifier = Modifier.padding(GotestTheme.spacing.xxs)) {
                GotestProgressBar(
                    progress = if (total == 0) 0f else completed.toFloat() / total,
                    color = colors.bgGreen,
                    trackColor = Color.Transparent,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.studypackage_done),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.studypackage_subject_progress, completed, total),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
            )
        }
    }
}

private val previewHeader =
    SubjectListHeader(
        category = StudyPackagePreviewData.categories[1],
        categories = StudyPackagePreviewData.categories,
    )

@Composable
private fun SubjectListPreview(uiState: SubjectListUiState) {
    GotestTheme {
        SubjectListScreen(uiState = uiState, onEvent = {}, onBack = {}, onSearch = {}, onSubjectClick = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubjectListSuccessPreview() {
    SubjectListPreview(SubjectListUiState.Success(previewHeader, StudyPackagePreviewData.subjects))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubjectListLoadingPreview() {
    SubjectListPreview(SubjectListUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubjectListEmptyPreview() {
    SubjectListPreview(SubjectListUiState.Empty(previewHeader))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubjectListErrorPreview() {
    SubjectListPreview(SubjectListUiState.Error)
}
