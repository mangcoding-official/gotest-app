package com.gotest.app.feature.exam.ui.discussion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.question.GotestQuestionCell
import com.gotest.app.core.designsystem.component.question.GotestQuestionOptions
import com.gotest.app.core.designsystem.component.question.GotestStimulusCard
import com.gotest.app.core.designsystem.component.question.QuestionCellState
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.QuestionType
import com.gotest.app.feature.exam.domain.ReviewStatus
import com.gotest.app.feature.exam.domain.hasStimulus
import com.gotest.app.feature.exam.ui.components.AnswerComparison
import com.gotest.app.feature.exam.ui.components.ExamBottomBar
import com.gotest.app.feature.exam.ui.components.ExamDimens
import com.gotest.app.feature.exam.ui.components.ExamLegendItem
import com.gotest.app.feature.exam.ui.components.ExamPreviewData
import com.gotest.app.feature.exam.ui.components.ReviewStatusChip

/** Cells kept to the left of the current one when the strip scrolls. */
private const val STRIP_LEADING_CELLS = 2

@Composable
fun DiscussionRoute(
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscussionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DiscussionScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenSummary = onOpenSummary,
        modifier = modifier,
    )
}

/** Pembahasan, one question at a time. */
@Composable
fun DiscussionScreen(
    uiState: DiscussionUiState,
    onEvent: (DiscussionUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val success = uiState as? DiscussionUiState.Success
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.exam_discussion_title),
            subtitle = success?.packageTitle,
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                GotestIconButton(
                    icon = GotestIcons.Linear.Menu,
                    contentDescription = stringResource(R.string.exam_discussion_cd_summary),
                    onClick = onOpenSummary,
                )
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                DiscussionUiState.Loading -> {
                    GotestLoadingView()
                }

                DiscussionUiState.Error -> {
                    GotestErrorView(onRetry = { onEvent(DiscussionUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
                }

                DiscussionUiState.Empty -> {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.BookOpened,
                        title = stringResource(R.string.exam_discussion_empty_title),
                        message = stringResource(R.string.exam_discussion_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is DiscussionUiState.Success -> {
                    DiscussionContent(state = uiState, onEvent = onEvent, modifier = Modifier.fillMaxSize())
                }
            }
        }
        if (success != null) {
            DiscussionControls(state = success, onEvent = onEvent)
        }
    }
}

@Composable
private fun DiscussionContent(
    state: DiscussionUiState.Success,
    onEvent: (DiscussionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier) {
        QuestionStrip(
            items = state.strip,
            currentNumber = state.currentNumber,
            onSelect = { onEvent(DiscussionUiEvent.SelectQuestion(it)) },
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        )
        // A fresh scroll position for every question.
        key(state.current.question.id) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
            ) {
                ReviewQuestionCard(
                    review = state.current,
                    number = state.currentNumber,
                    status = state.currentStatus,
                )
            }
        }
    }
}

/** Horizontal strip of numbered cells that keeps the current one in view. */
@Composable
private fun QuestionStrip(
    items: List<DiscussionStripItem>,
    currentNumber: Int,
    onSelect: (number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentNumber) {
        listState.animateScrollToItem((currentNumber - 1 - STRIP_LEADING_CELLS).coerceAtLeast(0))
    }
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = GotestTheme.spacing.lg),
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            items(items, key = { it.questionId }) { item ->
                StripCell(
                    item = item,
                    isCurrent = item.number == currentNumber,
                    onClick = { onSelect(item.number) },
                )
            }
        }
    }
}

@Composable
private fun StripCell(
    item: DiscussionStripItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.extraSmall
    val (cellState, accent) =
        when (item.status) {
            ReviewStatus.Correct -> QuestionCellState.Correct to colors.statusSuccess
            ReviewStatus.Wrong -> QuestionCellState.Wrong to colors.statusDanger
            ReviewStatus.Empty -> QuestionCellState.Skipped to colors.statusWarning
        }
    GotestQuestionCell(
        number = item.number,
        state = cellState,
        onClick = null,
        modifier =
            modifier
                .size(GotestTheme.sizes.questionCell)
                .clip(shape)
                .selectable(selected = isCurrent, role = Role.Tab, onClick = onClick)
                .then(
                    if (isCurrent) Modifier.border(GotestTheme.sizes.strokeThin * 2, accent, shape) else Modifier,
                ),
    )
}

@Composable
private fun ReviewQuestionCard(
    review: QuestionReview,
    number: Int,
    status: ReviewStatus,
    modifier: Modifier = Modifier,
) {
    val question = review.question
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.exam_question_number, number),
                    style = MaterialTheme.typography.titleMedium,
                    color = GotestTheme.colors.textPrimary,
                )
                GotestChip(text = question.topic, tone = GotestChipTone.Violet)
            }
            ReviewStatusChip(status = status)
        }
        if (question.hasStimulus) {
            GotestStimulusCard(
                title = question.stimulusTitle,
                body = question.stimulusBody,
                imageModel = question.imageUrl,
            )
        }
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.bodyLarge,
            color = GotestTheme.colors.textPrimary,
        )
        GotestQuestionOptions(question = question, answer = review.answer, onAnswerChange = null, revealAnswers = true)
        if (question.type == QuestionType.MultipleChoice) {
            AnswerLegend(showYourAnswer = status == ReviewStatus.Wrong)
        } else {
            // Only Pilihan Ganda options colour the key, so spell it out for the other types.
            AnswerComparison(question = question, answer = review.answer, status = status)
        }
        ExplanationCard(explanation = question.explanation)
    }
}

@Composable
private fun AnswerLegend(
    showYourAnswer: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
        if (showYourAnswer) {
            ExamLegendItem(color = colors.statusDanger, label = stringResource(R.string.exam_discussion_legend_yours))
        }
        ExamLegendItem(color = colors.statusSuccess, label = stringResource(R.string.exam_discussion_legend_correct))
    }
}

@Composable
private fun ExplanationCard(
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgSubtle, MaterialTheme.shapes.small)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.BookOpened),
                contentDescription = null,
                modifier = Modifier.size(ExamDimens.explanationIcon),
                tint = colors.iconDefault,
            )
            Text(
                text = stringResource(R.string.exam_discussion_explanation),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
        }
        Text(text = explanation, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun DiscussionControls(
    state: DiscussionUiState.Success,
    onEvent: (DiscussionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExamBottomBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            GotestButton(
                text = stringResource(R.string.exam_discussion_previous),
                onClick = { onEvent(DiscussionUiEvent.Previous) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = state.hasPrevious,
            )
            GotestButton(
                text = stringResource(R.string.exam_discussion_next),
                onClick = { onEvent(DiscussionUiEvent.Next) },
                modifier = Modifier.weight(1f),
                enabled = state.hasNext,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionScreenPreview() {
    GotestTheme {
        DiscussionScreen(
            uiState = DiscussionUiState.from("Simulasi UTBK-SNBT Session 1", ExamPreviewData.reviews, 2),
            onEvent = {},
            onBack = {},
            onOpenSummary = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionScreenTrueFalsePreview() {
    GotestTheme {
        DiscussionScreen(
            uiState = DiscussionUiState.from("Latihan September Session 1", ExamPreviewData.reviews, 5),
            onEvent = {},
            onBack = {},
            onOpenSummary = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionScreenLoadingPreview() {
    GotestTheme {
        DiscussionScreen(uiState = DiscussionUiState.Loading, onEvent = {}, onBack = {}, onOpenSummary = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionScreenErrorPreview() {
    GotestTheme {
        DiscussionScreen(uiState = DiscussionUiState.Error, onEvent = {}, onBack = {}, onOpenSummary = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionScreenEmptyPreview() {
    GotestTheme {
        DiscussionScreen(uiState = DiscussionUiState.Empty, onEvent = {}, onBack = {}, onOpenSummary = {})
    }
}
