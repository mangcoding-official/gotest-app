package com.gotest.app.feature.exam.ui.summary

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.exam.domain.ReviewTally
import com.gotest.app.feature.exam.ui.components.AnswerComparison
import com.gotest.app.feature.exam.ui.components.ExamPreviewData
import com.gotest.app.feature.exam.ui.components.ReviewStatusChip

private const val EXPLANATION_MAX_LINES = 2

@Composable
fun DiscussionSummaryRoute(
    onBack: () -> Unit,
    onOpenQuestion: (number: Int) -> Unit,
    onOpenDiscussion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscussionSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DiscussionSummaryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenQuestion = onOpenQuestion,
        onOpenDiscussion = onOpenDiscussion,
        modifier = modifier,
    )
}

/** Pembahasan as a list: tallies, topic filter and one card per question. */
@Composable
fun DiscussionSummaryScreen(
    uiState: DiscussionSummaryUiState,
    onEvent: (DiscussionSummaryUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenQuestion: (number: Int) -> Unit,
    onOpenDiscussion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.exam_discussion_title),
            subtitle = (uiState as? DiscussionSummaryUiState.Success)?.packageTitle,
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                GotestIconButton(
                    icon = GotestIcons.Linear.Menu,
                    contentDescription = stringResource(R.string.exam_summary_cd_detail),
                    onClick = onOpenDiscussion,
                )
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                DiscussionSummaryUiState.Loading -> {
                    GotestLoadingView()
                }

                DiscussionSummaryUiState.Error -> {
                    GotestErrorView(
                        onRetry = { onEvent(DiscussionSummaryUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                DiscussionSummaryUiState.Empty -> {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.BookOpened,
                        title = stringResource(R.string.exam_discussion_empty_title),
                        message = stringResource(R.string.exam_discussion_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is DiscussionSummaryUiState.Success -> {
                    SummaryList(
                        state = uiState,
                        onSelectTopic = { onEvent(DiscussionSummaryUiEvent.SelectTopic(it)) },
                        onOpenQuestion = onOpenQuestion,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryList(
    state: DiscussionSummaryUiState.Success,
    onSelectTopic: (topic: String?) -> Unit,
    onOpenQuestion: (number: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = spacing.sm, bottom = spacing.lg + navigationBarPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "tally") {
            SummaryTiles(
                tally = state.tally,
                score = state.score,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
        item(key = "topics") {
            TopicFilterRow(topics = state.topics, selectedTopic = state.selectedTopic, onSelect = onSelectTopic)
        }
        items(state.items, key = { it.review.question.id }) { item ->
            SummaryReviewCard(
                item = item,
                onClick = { onOpenQuestion(item.number) },
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
            )
        }
    }
}

@Composable
private fun SummaryTiles(
    tally: ReviewTally,
    score: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(GotestTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            SummaryTile(
                value = GotestFormat.number(tally.correct),
                label = stringResource(R.string.exam_summary_correct),
                container = colors.bgGreenSoft,
                content = colors.bgGreenDark,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                value = GotestFormat.number(tally.wrong),
                label = stringResource(R.string.exam_summary_wrong),
                container = colors.statusDangerSoft,
                content = colors.illustrationRedDark,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                value = GotestFormat.number(tally.empty),
                label = stringResource(R.string.exam_summary_empty),
                container = colors.bgYellowSoft,
                content = colors.bgYellowDark,
                modifier = Modifier.weight(1f),
            )
            SummaryTile(
                value = GotestFormat.number(score),
                label = stringResource(R.string.exam_summary_total_score),
                container = colors.bgBrandSoft,
                content = colors.textBrandDark,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryTile(
    value: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .background(container, MaterialTheme.shapes.medium)
                .padding(horizontal = spacing.sm, vertical = spacing.sm + spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = content)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopicFilterRow(
    topics: List<String>,
    selectedTopic: String?,
    onSelect: (topic: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = GotestTheme.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        item(key = "all") {
            GotestFilterPill(
                text = stringResource(R.string.exam_summary_all_topics),
                selected = selectedTopic == null,
                onClick = { onSelect(null) },
            )
        }
        items(topics, key = { it }) { topic ->
            GotestFilterPill(text = topic, selected = topic == selectedTopic, onClick = { onSelect(topic) })
        }
    }
}

@Composable
private fun SummaryReviewCard(
    item: SummaryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = item.review.question
    GotestCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.exam_question_number, item.number),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            ReviewStatusChip(status = item.status)
        }
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textPrimary,
        )
        AnswerComparison(
            question = question,
            answer = item.review.answer,
            status = item.status,
            modifier = Modifier.fillMaxWidth(),
        )
        ExplanationRow(explanation = question.explanation)
    }
}

@Composable
private fun ExplanationRow(
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgSubtle, MaterialTheme.shapes.small)
                .padding(horizontal = spacing.md, vertical = spacing.sm + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.BookOpened),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = colors.iconBrand,
        )
        Text(
            text = stringResource(R.string.exam_summary_explanation, explanation),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textPrimary,
            maxLines = EXPLANATION_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionSummaryScreenPreview() {
    GotestTheme {
        DiscussionSummaryScreen(
            uiState =
                DiscussionSummaryUiState.from(
                    packageTitle = "Simulasi UTBK-SNBT Session 1",
                    score = 140,
                    reviews = ExamPreviewData.reviews,
                    selectedTopic = null,
                ),
            onEvent = {},
            onBack = {},
            onOpenQuestion = {},
            onOpenDiscussion = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionSummaryScreenFilteredPreview() {
    GotestTheme {
        DiscussionSummaryScreen(
            uiState =
                DiscussionSummaryUiState.from(
                    packageTitle = "Simulasi UTBK-SNBT Session 1",
                    score = 140,
                    reviews = ExamPreviewData.reviews,
                    selectedTopic = "Matematika Dasar",
                ),
            onEvent = {},
            onBack = {},
            onOpenQuestion = {},
            onOpenDiscussion = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionSummaryScreenLoadingPreview() {
    GotestTheme {
        DiscussionSummaryScreen(
            uiState = DiscussionSummaryUiState.Loading,
            onEvent = {},
            onBack = {},
            onOpenQuestion = {},
            onOpenDiscussion = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionSummaryScreenErrorPreview() {
    GotestTheme {
        DiscussionSummaryScreen(
            uiState = DiscussionSummaryUiState.Error,
            onEvent = {},
            onBack = {},
            onOpenQuestion = {},
            onOpenDiscussion = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun DiscussionSummaryScreenEmptyPreview() {
    GotestTheme {
        DiscussionSummaryScreen(
            uiState = DiscussionSummaryUiState.Empty,
            onEvent = {},
            onBack = {},
            onOpenQuestion = {},
            onOpenDiscussion = {},
        )
    }
}
