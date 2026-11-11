package com.gotest.app.feature.studypackage.ui.topiclist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.Topic
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.studypackage.ui.components.CardSegment
import com.gotest.app.feature.studypackage.ui.components.CardSegmentPosition
import com.gotest.app.feature.studypackage.ui.components.CardStat
import com.gotest.app.feature.studypackage.ui.components.PracticeDetailSheet
import com.gotest.app.feature.studypackage.ui.components.StudyItemCard
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData

@Composable
fun TopicListRoute(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onTopicClick: (packageId: String) -> Unit,
    onStartExam: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopicListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is TopicListEffect.StartExam -> onStartExam(effect.packageId)
        }
    }
    TopicListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onSearch = onSearch,
        onTopicClick = onTopicClick,
        modifier = modifier,
    )
}

/** Topics of one subject (frame 6863), with the shared "Detail Latihan" sheet. */
@Composable
fun TopicListScreen(
    uiState: TopicListUiState,
    onEvent: (TopicListUiEvent) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onTopicClick: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val content = uiState.content
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title =
                when (content) {
                    is TopicListContent.Success -> content.subject.name
                    is TopicListContent.Empty -> content.subject.name
                    else -> stringResource(R.string.studypackage_topics_title_fallback)
                },
            subtitle =
                (content as? TopicListContent.Success)?.let {
                    stringResource(R.string.studypackage_topics_subtitle, it.levelLabel, it.topics.size)
                },
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
            topicListContent(content = content, onEvent = onEvent, onTopicClick = onTopicClick)
        }
        // The sheet is its own window; it sits here only to keep one root emitter.
        uiState.sheet?.let { sheet ->
            PracticeDetailSheet(
                state = sheet,
                onDismiss = { onEvent(TopicListUiEvent.SheetDismissed) },
                onStart = { onEvent(TopicListUiEvent.SheetStartClicked(it)) },
                onRetry = { onEvent(TopicListUiEvent.SheetRetry) },
            )
        }
    }
}

private fun LazyListScope.topicListContent(
    content: TopicListContent,
    onEvent: (TopicListUiEvent) -> Unit,
    onTopicClick: (packageId: String) -> Unit,
) {
    when (content) {
        TopicListContent.Loading -> {
            item(key = "loading") {
                GotestLoadingView(modifier = Modifier.padding(vertical = GotestTheme.spacing.xxxl))
            }
        }

        TopicListContent.Error -> {
            item(key = "error") {
                GotestErrorView(onRetry = { onEvent(TopicListUiEvent.Retry) }, modifier = Modifier.fillMaxWidth())
            }
        }

        is TopicListContent.Empty -> {
            item(key = "empty") {
                GotestCard(modifier = Modifier.fillMaxWidth()) {
                    GotestEmptyView(
                        icon = GotestIcons.Linear.BookOpened,
                        title = stringResource(R.string.studypackage_topics_empty_title),
                        message = stringResource(R.string.studypackage_topics_empty_message),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is TopicListContent.Success -> {
            topicCards(topics = content.topics, onEvent = onEvent, onTopicClick = onTopicClick)
        }
    }
}

/** All topics inside one white container card, drawn slice by slice. */
private fun LazyListScope.topicCards(
    topics: List<Topic>,
    onEvent: (TopicListUiEvent) -> Unit,
    onTopicClick: (packageId: String) -> Unit,
) {
    item(key = "list_top") {
        CardSegment(position = CardSegmentPosition.Top, modifier = Modifier.height(GotestTheme.spacing.lg))
    }
    itemsIndexed(topics, key = { _, topic -> topic.id }) { index, topic ->
        CardSegment(position = CardSegmentPosition.Middle) {
            TopicCard(
                topic = topic,
                onAction = { onEvent(TopicListUiEvent.TopicActionClicked(topic.packageId)) },
                onClick = { onTopicClick(topic.packageId) },
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

@Composable
private fun TopicCard(
    topic: Topic,
    onAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.studypackage_value_empty)
    val attempted = topic.attempts > 0
    StudyItemCard(
        title = topic.name,
        subtitle = stringResource(R.string.studypackage_topic_count, topic.topicCount),
        actionText =
            stringResource(
                if (attempted) R.string.studypackage_topic_again else R.string.studypackage_action_start_practice,
            ),
        onAction = onAction,
        onClick = onClick,
        stats =
            CardStat(
                label = stringResource(R.string.studypackage_done),
                value = if (attempted) GotestFormat.number(topic.attempts) else empty,
            ) to
                CardStat(
                    label = stringResource(R.string.studypackage_done_on),
                    value = topic.lastAttemptOn?.let(GotestFormat::mediumDate) ?: empty,
                ),
        modifier = modifier.fillMaxWidth(),
        scoreText = topic.lastScore?.let { stringResource(R.string.studypackage_score, it) },
    )
}

private val previewSubject = StudyPackagePreviewData.subjects.last()

@Composable
private fun TopicListPreview(uiState: TopicListUiState) {
    GotestTheme {
        TopicListScreen(uiState = uiState, onEvent = {}, onBack = {}, onSearch = {}, onTopicClick = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TopicListSuccessPreview() {
    TopicListPreview(
        TopicListUiState(
            content = TopicListContent.Success(previewSubject, "SLTA", StudyPackagePreviewData.topics),
        ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TopicListLoadingPreview() {
    TopicListPreview(TopicListUiState())
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TopicListEmptyPreview() {
    TopicListPreview(TopicListUiState(content = TopicListContent.Empty(previewSubject, "SLTA")))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun TopicListErrorPreview() {
    TopicListPreview(TopicListUiState(content = TopicListContent.Error))
}
