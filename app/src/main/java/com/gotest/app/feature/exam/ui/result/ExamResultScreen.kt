package com.gotest.app.feature.exam.ui.result

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.TopicAccuracy
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.exam.domain.AccuracyLevel
import com.gotest.app.feature.exam.domain.ReviewRules
import com.gotest.app.feature.exam.ui.components.ExamBottomBar
import com.gotest.app.feature.exam.ui.components.ExamDimens
import com.gotest.app.feature.exam.ui.components.ExamPreviewData

private const val PERCENT_SCALE = 100f

@Composable
fun ExamResultRoute(
    onClose: () -> Unit,
    onViewDiscussion: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ExamResultScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onShare = { context.shareResult(it) },
        onViewDiscussion = onViewDiscussion,
        onBackHome = onBackHome,
        modifier = modifier,
    )
}

/** Opens the system share sheet with a short summary of the attempt. */
private fun Context.shareResult(result: AttemptResult) {
    val rank = result.rank
    val participants = result.participants
    val text =
        if (result.type == PackageType.Tryout && rank != null && participants != null) {
            getString(
                R.string.exam_result_share_text_ranked,
                result.score,
                result.title,
                GotestFormat.number(rank),
                GotestFormat.number(participants),
            )
        } else {
            getString(R.string.exam_result_share_text, result.score, result.title)
        }
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startActivity(Intent.createChooser(send, getString(R.string.exam_result_cd_share)))
}

/** "Hasil Tryout" / "Hasil Latihan". */
@Composable
fun ExamResultScreen(
    uiState: ExamResultUiState,
    onEvent: (ExamResultUiEvent) -> Unit,
    onClose: () -> Unit,
    onShare: (AttemptResult) -> Unit,
    onViewDiscussion: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val success = uiState as? ExamResultUiState.Success
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title =
                success
                    ?.let {
                        stringResource(
                            if (it.isTryout) R.string.exam_result_title_tryout else R.string.exam_result_title_practice,
                        )
                    }.orEmpty(),
            subtitle = success?.result?.title,
            navigationIcon = {
                GotestIconButton(
                    icon = GotestIcons.Linear.CloseCircle,
                    contentDescription = stringResource(R.string.exam_cd_close),
                    onClick = onClose,
                )
            },
            actions = {
                if (success != null) {
                    GotestIconButton(
                        icon = GotestIcons.Linear.Export,
                        contentDescription = stringResource(R.string.exam_result_cd_share),
                        onClick = { onShare(success.result) },
                    )
                }
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                ExamResultUiState.Loading -> {
                    GotestLoadingView()
                }

                ExamResultUiState.Error -> {
                    GotestErrorView(
                        onRetry = { onEvent(ExamResultUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is ExamResultUiState.Success -> {
                    ResultContent(state = uiState, modifier = Modifier.fillMaxSize())
                }
            }
        }
        if (success != null) {
            ExamBottomBar {
                GotestButton(
                    text = stringResource(R.string.exam_result_view_discussion),
                    onClick = onViewDiscussion,
                    modifier = Modifier.fillMaxWidth(),
                )
                GotestButton(
                    text = stringResource(R.string.exam_result_back_home),
                    onClick = onBackHome,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: ExamResultUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        ScoreCard(state = state)
        if (state.result.topicAccuracy.isNotEmpty()) {
            TopicAccuracyCard(
                topics = state.result.topicAccuracy,
                averagePercent = state.averagePercent,
                weakestTopic = state.result.weakestTopic,
            )
        }
    }
}

@Composable
private fun ScoreCard(
    state: ExamResultUiState.Success,
    modifier: Modifier = Modifier,
) {
    val result = state.result
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.illus_trophy),
                contentDescription = null,
                modifier = Modifier.size(ExamDimens.trophyWidth, ExamDimens.trophyHeight),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.exam_result_total_score),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
            Text(
                text = GotestFormat.number(result.score),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
            )
            val rank = result.rank
            val participants = result.participants
            if (state.showsRank && rank != null && participants != null) {
                RankChip(rank = rank, participants = participants)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            ) {
                ResultTile(
                    icon = GotestIcons.Bold.TickCircle,
                    count = result.correct,
                    label = stringResource(R.string.exam_result_correct),
                    container = colors.bgGreenSoft,
                    content = colors.bgGreenDark,
                    modifier = Modifier.weight(1f),
                )
                ResultTile(
                    icon = GotestIcons.Bold.CloseCircle,
                    count = result.wrong,
                    label = stringResource(R.string.exam_result_wrong),
                    container = colors.statusDangerSoft,
                    content = colors.statusDanger,
                    modifier = Modifier.weight(1f),
                )
                ResultTile(
                    icon = GotestIcons.Bold.MinusCircle,
                    count = result.empty,
                    label = stringResource(R.string.exam_result_empty),
                    container = colors.bgYellowSoft,
                    content = colors.bgYellowDark,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RankChip(
    rank: Int,
    participants: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .background(GotestTheme.colors.bgSubtle, GotestTheme.shapes.full)
                .padding(horizontal = spacing.md, vertical = spacing.xs + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Bold.Award),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = GotestTheme.colors.bgYellow,
        )
        Text(
            text =
                stringResource(
                    R.string.exam_result_rank,
                    GotestFormat.number(rank),
                    GotestFormat.number(participants),
                ),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun ResultTile(
    @DrawableRes icon: Int,
    count: Int,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(container, MaterialTheme.shapes.large)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = content,
        )
        Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall, color = content)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Composable
private fun TopicAccuracyCard(
    topics: List<TopicAccuracy>,
    averagePercent: Int,
    weakestTopic: String?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        verticalSpacing = GotestTheme.spacing.md + GotestTheme.spacing.xxs,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.exam_result_topic_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.exam_result_average, averagePercent),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        topics.forEach { TopicAccuracyRow(topic = it) }
        if (weakestTopic != null) {
            FocusTip(topic = weakestTopic)
        }
    }
}

@Composable
private fun TopicAccuracyRow(
    topic: TopicAccuracy,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val barColor =
        when (ReviewRules.accuracyLevel(topic.percent)) {
            AccuracyLevel.High -> colors.bgGreen
            AccuracyLevel.Medium -> colors.bgBlue
            AccuracyLevel.Low -> colors.bgYellow
        }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs + GotestTheme.spacing.xxs),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = topic.topic,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.percent_value, topic.percent),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
            )
        }
        GotestProgressBar(
            progress = topic.percent / PERCENT_SCALE,
            color = barColor,
            height = ExamDimens.topicTrack,
        )
    }
}

/** Blue "Fokuskan latihan berikutnya …" box. */
@Composable
private fun FocusTip(
    topic: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgBlueSoft, MaterialTheme.shapes.medium)
                .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm + spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Bold.Like),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = colors.bgBlue,
        )
        Text(
            text = stringResource(R.string.exam_result_tip, topic),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = colors.bgBlueDark,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamResultScreenTryoutPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState = ExamResultUiState.Success(ExamPreviewData.tryoutResult),
            onEvent = {},
            onClose = {},
            onShare = {},
            onViewDiscussion = {},
            onBackHome = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamResultScreenPracticePreview() {
    GotestTheme {
        ExamResultScreen(
            uiState =
                ExamResultUiState.Success(
                    ExamPreviewData.tryoutResult.copy(
                        title = "Latihan September Session 1",
                        type = PackageType.Practice,
                        score = 12,
                    ),
                ),
            onEvent = {},
            onClose = {},
            onShare = {},
            onViewDiscussion = {},
            onBackHome = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamResultScreenLoadingPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState = ExamResultUiState.Loading,
            onEvent = {},
            onClose = {},
            onShare = {},
            onViewDiscussion = {},
            onBackHome = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamResultScreenErrorPreview() {
    GotestTheme {
        ExamResultScreen(
            uiState = ExamResultUiState.Error,
            onEvent = {},
            onClose = {},
            onShare = {},
            onViewDiscussion = {},
            onBackHome = {},
        )
    }
}
