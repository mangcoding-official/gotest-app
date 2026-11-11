package com.gotest.app.feature.exam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.exam.domain.AnswerPart
import com.gotest.app.feature.exam.domain.ReviewRules
import com.gotest.app.feature.exam.domain.ReviewStatus

/** Sizes from the exam frames that the shared tokens do not have. */
internal object ExamDimens {
    /** Legend swatches in "Navigasi Soal" and Pembahasan. */
    val legendSwatch = 10.dp

    /** Topic accuracy track on Hasil. */
    val topicTrack = 6.dp

    /** "illus_trophy" on Hasil. */
    val trophyWidth = 162.dp
    val trophyHeight = 106.dp

    /** Book icon of the Pembahasan card. */
    val explanationIcon = 18.dp
}

/**
 * White action bar pinned to the bottom of the exam screens, with the hairline on top and
 * the navigation-bar inset below.
 */
@Composable
internal fun ExamBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp),
    ) {
        GotestDivider()
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content,
        )
    }
}

/** Small rounded swatch plus caption ("Dijawab", "Jawabanmu", …). */
@Composable
internal fun ExamLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
) {
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs + GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ExamDimens.legendSwatch)
                    .background(color, shape)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(GotestTheme.sizes.strokeThin, borderColor, shape)
                        } else {
                            Modifier
                        },
                    ),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/** "Benar • 1/1", "Salah • 0/1" or "Kosong • 0/1". */
@Composable
internal fun ReviewStatusChip(
    status: ReviewStatus,
    modifier: Modifier = Modifier,
) {
    val (textRes, tone) =
        when (status) {
            ReviewStatus.Correct -> R.string.exam_review_status_correct to GotestChipTone.Green
            ReviewStatus.Wrong -> R.string.exam_review_status_wrong to GotestChipTone.Red
            ReviewStatus.Empty -> R.string.exam_review_status_empty to GotestChipTone.Yellow
        }
    val text = stringResource(textRes, ReviewRules.points(status), ReviewRules.MAX_POINTS)
    GotestChip(text = text, modifier = modifier, tone = tone)
}

private data class AnswerBoxColors(
    val container: Color,
    val content: Color,
)

@Composable
private fun answerBoxColors(status: ReviewStatus): AnswerBoxColors {
    val colors = GotestTheme.colors
    return when (status) {
        ReviewStatus.Correct -> AnswerBoxColors(colors.bgGreenSoft, colors.bgGreenDark)
        ReviewStatus.Wrong -> AnswerBoxColors(colors.statusDangerSoft, colors.illustrationRedDark)
        ReviewStatus.Empty -> AnswerBoxColors(colors.bgYellowSoft, colors.bgYellowDark)
    }
}

/** The "Jawabanmu" / "Jawaban benar" pair of boxes on review cards. */
@Composable
internal fun AnswerComparison(
    question: Question,
    answer: QuestionAnswer,
    status: ReviewStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        AnswerBox(
            label = stringResource(R.string.exam_summary_your_answer),
            value = answerText(ReviewRules.answerParts(question, answer)),
            colors = answerBoxColors(status),
            modifier = Modifier.weight(1f),
        )
        AnswerBox(
            label = stringResource(R.string.exam_summary_correct_answer),
            value = answerText(ReviewRules.correctParts(question)),
            colors = answerBoxColors(ReviewStatus.Correct),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnswerBox(
    label: String,
    value: String,
    colors: AnswerBoxColors,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .background(colors.container, shape)
                .border(BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle), shape)
                .padding(spacing.sm + spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.content)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = colors.content)
    }
}

/**
 * "C. 24" for one option, "A, C" for several (PG Kompleks), "1. Benar, 2. Salah" for
 * statements, or "Tidak dijawab".
 */
@Composable
private fun answerText(parts: List<AnswerPart>): String {
    val separator = stringResource(R.string.exam_answer_separator)
    val trueLabel = stringResource(R.string.question_true)
    val falseLabel = stringResource(R.string.question_false)
    val single = parts.singleOrNull()
    return when {
        parts.isEmpty() -> {
            stringResource(R.string.exam_answer_none)
        }

        single is AnswerPart.Option -> {
            stringResource(R.string.exam_answer_option, single.key, single.text)
        }

        else -> {
            val statementFormat = stringResource(R.string.exam_answer_statement)
            parts.joinToString(separator) { part ->
                when (part) {
                    is AnswerPart.Option -> {
                        part.key
                    }

                    is AnswerPart.Statement -> {
                        statementFormat.format(
                            part.number,
                            if (part.isTrue) trueLabel else falseLabel,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg)) {
                ExamLegendItem(color = GotestTheme.colors.bgGreen, label = "Dijawab")
                ExamLegendItem(color = GotestTheme.colors.bgYellow, label = "Ditandai")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                ReviewStatusChip(status = ReviewStatus.Correct)
                ReviewStatusChip(status = ReviewStatus.Wrong)
                ReviewStatusChip(status = ReviewStatus.Empty)
            }
            AnswerComparison(
                question = ExamPreviewData.permutationQuestion,
                answer = QuestionAnswer(selectedKeys = setOf("B")),
                status = ReviewStatus.Wrong,
                modifier = Modifier.fillMaxWidth(),
            )
            ExamBottomBar {
                Text(text = "Bottom bar")
            }
        }
    }
}
