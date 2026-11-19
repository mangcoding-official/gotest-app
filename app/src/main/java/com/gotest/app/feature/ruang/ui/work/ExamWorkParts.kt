package com.gotest.app.feature.ruang.ui.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import com.gotest.app.BuildConfig
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.data.listNumbers
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamBlock
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone

/** A server image address: `/storage/...` is joined with the server, as on the web. */
private fun imageAddress(url: String): String =
    if (url.startsWith("/") && !url.startsWith("//")) BuildConfig.API_BASE_URL.trimEnd('/') + url else url

@Composable
fun ExamImage(
    url: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageAddress(url),
        contentDescription = description,
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.small),
        contentScale = ContentScale.FillWidth,
    )
}

/** Connection, save notices, refusals, and time up, above the paper (Work.tsx). */
@Composable
fun WorkBanners(
    uiState: ExamWorkUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val quiz = uiState.page?.isQuiz == true
        if (uiState.timeUp) {
            NoticeBox(text = stringResource(if (quiz) R.string.quiz_time_up else R.string.work_time_up))
        }
        uiState.stopped?.let {
            val fallback = stringResource(if (quiz) R.string.quiz_stopped else R.string.work_stopped)
            NoticeBox(text = it.ifBlank { fallback }, tone = NoticeTone.Danger)
        }
        if (uiState.connectionLost && uiState.unsentItemIds.isNotEmpty()) {
            val numbers = uiState.unsentItemIds.mapNotNull { uiState.items[it]?.item?.number }.sorted()
            NoticeBox(text = stringResource(R.string.work_connection_lost, listNumbers(numbers)))
        }
        when (val notice = uiState.notice) {
            is WorkNotice.Rejected -> NoticeBox(text = stringResource(R.string.work_rejected, notice.numbers))
            WorkNotice.Stale -> NoticeBox(text = stringResource(R.string.work_stale), tone = NoticeTone.Info)
            null -> Unit
        }
        val error = uiState.errorMessage ?: stringResource(R.string.work_save_failed).takeIf { uiState.saveFailed }
        error?.let { NoticeBox(text = it, tone = NoticeTone.Danger) }
    }
}

/** One screen of the paper: a reading passage with its items, or one item. */
@Composable
fun WorkBlock(
    block: ExamBlock,
    uiState: ExamWorkUiState,
    onAnswer: (Long, ExamAnswer?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
        val colors = GotestTheme.colors
        if (block.kind == "group") {
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgBlueSoft, showBorder = false) {
                block.title?.let {
                    Text(text = it, style = MaterialTheme.typography.labelLarge, color = colors.bgBlueDark)
                }
                block.passage?.let {
                    RichText(
                        content = it,
                        isHtml = true,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                }
                block.mediaUrl?.let { ExamImage(url = it, description = stringResource(R.string.work_passage_image)) }
            }
        }
        block.itemIds.mapNotNull { uiState.items[it] }.forEach { workItem ->
            val item = workItem.item
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.work_question, item.number),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                RichText(
                    content = item.prompt,
                    isHtml = true,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                item.mediaUrl?.let {
                    ExamImage(
                        url = it,
                        description = stringResource(R.string.work_question_image, item.number),
                    )
                }
                ExamAnswerArea(
                    content = workItem.content,
                    answer = uiState.answers[item.id],
                    onAnswer = if (uiState.locked) null else { answer -> onAnswer(item.id, answer) },
                )
                if (uiState.isAnswered(item.id) && !uiState.locked) {
                    GotestButton(
                        text = stringResource(R.string.work_clear),
                        onClick = { onAnswer(item.id, null) },
                        variant = GotestButtonVariant.Text,
                        size = GotestButtonSize.Small,
                    )
                }
            }
        }
    }
}

@Composable
fun ViolationPanel(
    violations: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.xs) {
        Text(
            text = stringResource(R.string.work_violations, violations, limit),
            style = MaterialTheme.typography.labelLarge,
            color = if (violations > 0) GotestTheme.colors.statusDanger else GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.work_violations_note),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

/** "Navigasi Soal": answered green, current outlined, unsent dashed red while offline (ExamPaper). */
@Composable
fun QuestionNavigator(
    uiState: ExamWorkUiState,
    onGoTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val current =
        uiState.currentBlock
            ?.itemIds
            .orEmpty()
            .toSet()
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.work_nav_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.work_nav_filled, uiState.answeredCount, uiState.items.size),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            uiState.orderedItems.chunked(NAV_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                    row.forEach { workItem ->
                        val id = workItem.item.id
                        NavCell(
                            number = workItem.item.number,
                            answered = uiState.isAnswered(id),
                            current = id in current,
                            unsent = uiState.connectionLost && id in uiState.unsentItemIds,
                            enabled = !uiState.otherDevice,
                            onClick = { onGoTo(id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(NAV_COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun NavCell(
    number: Int,
    answered: Boolean,
    current: Boolean,
    unsent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val base =
        stringResource(if (answered) R.string.work_nav_answered else R.string.work_nav_empty, number)
    val label = if (unsent) stringResource(R.string.work_nav_unsent, base) else base
    val border =
        when {
            unsent -> BorderStroke(GotestTheme.sizes.strokeThin * 2, colors.statusDanger)
            current -> BorderStroke(GotestTheme.sizes.strokeThin * 2, colors.bgBrand)
            else -> BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
        }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.aspectRatio(1f).semantics { contentDescription = label },
        shape = RoundedCornerShape(GotestTheme.spacing.sm),
        color = if (answered) colors.statusSuccessSoft else colors.bgApp,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (answered) colors.statusSuccess else colors.textPrimary,
            )
        }
    }
}

private const val NAV_COLUMNS = 5
