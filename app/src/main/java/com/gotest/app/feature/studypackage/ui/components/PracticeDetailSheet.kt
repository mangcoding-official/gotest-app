package com.gotest.app.feature.studypackage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageTopic
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.ui.examChipTone
import kotlinx.coroutines.launch

/**
 * "Detail Latihan" bottom sheet shared by Paket Belajar and Topik. Buttons animate the sheet
 * away before reporting, so navigation starts after it has closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PracticeDetailSheet(
    state: PracticeSheetState,
    onDismiss: () -> Unit,
    onStart: (packageId: String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        when (state) {
            PracticeSheetState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(vertical = GotestTheme.spacing.xxxl))
            }

            PracticeSheetState.Error -> {
                GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
            }

            is PracticeSheetState.Success -> {
                PracticeSheetContent(
                    detail = state.detail,
                    onClose = { hideThen(onDismiss) },
                    onStart = { hideThen { onStart(state.detail.summary.id) } },
                )
            }
        }
    }
}

/** Sheet body; [onClose] and [onStart] back the two bottom buttons. */
@Composable
internal fun PracticeSheetContent(
    detail: StudyPackageDetail,
    onClose: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val summary = detail.summary
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        GotestChip(text = summary.examLabel, tone = examChipTone(summary.examLabel))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.headlineSmall,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = summary.description,
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (detail.topics.isNotEmpty()) {
                CompositionCard(topics = detail.topics)
            }
            TimerInfo()
        }
        Row(
            modifier = Modifier.padding(top = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            val resumable = summary.hasResumableProgress
            GotestButton(
                text =
                    stringResource(
                        if (resumable) R.string.studypackage_sheet_repeat else R.string.studypackage_sheet_close,
                    ),
                onClick = if (resumable) onStart else onClose,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text =
                    stringResource(
                        if (resumable) R.string.studypackage_sheet_continue else R.string.studypackage_sheet_start,
                    ),
                onClick = onStart,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompositionCard(
    topics: List<PackageTopic>,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(),
        verticalSpacing = spacing.none,
    ) {
        Text(
            text = stringResource(R.string.studypackage_sheet_composition),
            modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.sm),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        topics.forEach { topic ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GotestTheme.colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.studypackage_sheet_question_count, topic.questionCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = GotestTheme.colors.textSecondary,
                    )
                }
                topic.difficulty?.let { difficulty ->
                    GotestChip(text = stringResource(difficulty.labelRes), tone = difficulty.chipTone)
                }
            }
        }
    }
}

@Composable
private fun TimerInfo(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgBlueSoft, MaterialTheme.shapes.medium)
                .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.Calendar),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = colors.bgBlueDark,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(R.string.studypackage_sheet_timer_title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.bgBlueDark,
            )
            Text(
                text = stringResource(R.string.studypackage_sheet_timer_untimed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.bgBlueDark,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun PracticeSheetContentPreview() {
    GotestTheme {
        PracticeSheetContent(
            detail =
                StudyPackagePreviewData.practiceDetail.copy(
                    summary = StudyPackagePreviewData.practicePackage.copy(progress = null),
                ),
            onClose = {},
            onStart = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun PracticeSheetContentResumePreview() {
    GotestTheme {
        PracticeSheetContent(detail = StudyPackagePreviewData.practiceDetail, onClose = {}, onStart = {})
    }
}
