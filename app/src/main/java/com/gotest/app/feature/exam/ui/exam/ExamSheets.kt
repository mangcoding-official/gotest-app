package com.gotest.app.feature.exam.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.question.GotestQuestionCell
import com.gotest.app.core.designsystem.component.question.QuestionCellState
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.exam.domain.ExamCounts
import com.gotest.app.feature.exam.domain.NavigatorCell
import com.gotest.app.feature.exam.domain.NavigatorCellStatus
import com.gotest.app.feature.exam.ui.components.ExamLegendItem
import kotlinx.coroutines.launch

private const val NAVIGATOR_COLUMNS = 5

/** "Navigasi Soal" sheet; tapping a cell slides the sheet away and jumps to that question. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamNavigatorSheet(
    cells: List<NavigatorCell>,
    currentIndex: Int,
    answeredCount: Int,
    finishLabel: String,
    onSelect: (index: Int) -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        ExamNavigatorContent(
            cells = cells,
            answeredCount = answeredCount,
            finishLabel = finishLabel,
            onSelect = { index -> scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(index) } },
            onFinish = onFinish,
            gridState = rememberLazyGridState(initialFirstVisibleItemIndex = currentIndex),
        )
    }
}

@Composable
internal fun ExamNavigatorContent(
    cells: List<NavigatorCell>,
    answeredCount: Int,
    finishLabel: String,
    onSelect: (index: Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.exam_navigator_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.exam_navigator_filled, answeredCount, cells.size),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(NAVIGATOR_COLUMNS),
            modifier = Modifier.weight(1f, fill = false),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            itemsIndexed(cells, key = { _, cell -> cell.questionId }) { index, cell ->
                GotestQuestionCell(
                    number = cell.number,
                    state = cell.status.toCellState(),
                    onClick = { onSelect(index) },
                    modifier = Modifier.height(GotestTheme.sizes.questionCell),
                )
            }
        }
        NavigatorLegend()
        GotestButton(
            text = finishLabel,
            onClick = onFinish,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm),
        )
    }
}

private fun NavigatorCellStatus.toCellState(): QuestionCellState =
    when (this) {
        NavigatorCellStatus.Current -> QuestionCellState.Current
        NavigatorCellStatus.Marked -> QuestionCellState.Skipped
        NavigatorCellStatus.Answered -> QuestionCellState.Answered
        NavigatorCellStatus.Empty -> QuestionCellState.Empty
    }

@Composable
private fun NavigatorLegend(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ExamLegendItem(color = colors.bgGreen, label = stringResource(R.string.exam_legend_answered))
        ExamLegendItem(color = colors.borderFocus, label = stringResource(R.string.exam_legend_current))
        ExamLegendItem(color = colors.bgGrey, label = stringResource(R.string.exam_legend_empty))
        ExamLegendItem(color = colors.bgYellow, label = stringResource(R.string.exam_legend_marked))
    }
}

/** "Selesaikan tryout sekarang?" confirmation sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinishTryoutSheet(
    counts: ExamCounts,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        FinishTryoutContent(
            counts = counts,
            onConfirm = onConfirm,
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        )
    }
}

@Composable
internal fun FinishTryoutContent(
    counts: ExamCounts,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val sizes = GotestTheme.sizes
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Box(
            modifier =
                Modifier
                    .size(sizes.dialogIcon)
                    .background(colors.bgYellowSoft, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.InfoCircle),
                contentDescription = null,
                modifier = Modifier.size(sizes.iconXl),
                tint = colors.bgYellow,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.exam_finish_tryout_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text =
                    if (counts.unanswered == 0) {
                        stringResource(R.string.exam_finish_tryout_message_complete)
                    } else {
                        pluralStringResource(R.plurals.exam_finish_tryout_message, counts.unanswered, counts.unanswered)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        FinishCountRow(counts = counts)
        Column(
            modifier = Modifier.padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GotestButton(
                text = stringResource(R.string.exam_finish_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
            GotestButton(
                text = stringResource(R.string.exam_finish_keep_working),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
            )
        }
    }
}

@Composable
private fun FinishCountRow(
    counts: ExamCounts,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        CountTile(
            count = counts.answered,
            label = stringResource(R.string.exam_count_answered),
            container = colors.bgGreenSoft,
            content = colors.bgGreenDark,
            modifier = Modifier.weight(1f),
        )
        CountTile(
            count = counts.marked,
            label = stringResource(R.string.exam_count_marked),
            container = colors.bgYellowSoft,
            content = colors.bgYellowDark,
            modifier = Modifier.weight(1f),
        )
        CountTile(
            count = counts.empty,
            label = stringResource(R.string.exam_count_empty),
            container = colors.bgSubtle,
            content = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CountTile(
    count: Int,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(container, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall, color = content)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun ExamNavigatorContentPreview() {
    GotestTheme {
        ExamNavigatorContent(
            cells =
                List(20) { index ->
                    val status =
                        when (index) {
                            1, 2 -> NavigatorCellStatus.Answered
                            3 -> NavigatorCellStatus.Current
                            6 -> NavigatorCellStatus.Marked
                            else -> NavigatorCellStatus.Empty
                        }
                    NavigatorCell(questionId = "q$index", number = index + 1, status = status)
                },
            answeredCount = 4,
            finishLabel = "Selesaikan Tryout",
            onSelect = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun FinishTryoutContentPreview() {
    GotestTheme {
        FinishTryoutContent(
            counts = ExamCounts(total = 20, answered = 4, marked = 1, empty = 15),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
