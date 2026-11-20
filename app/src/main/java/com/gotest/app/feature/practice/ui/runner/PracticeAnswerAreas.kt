package com.gotest.app.feature.practice.ui.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.OptionText
import com.gotest.app.feature.practice.domain.QuestionContent
import com.gotest.app.feature.practice.domain.ResponseKind
import com.gotest.app.feature.ruang.ui.work.ExamImage

/** Web `mediaUrl`: an address as it is, or a stored file under `/storage/`. */
fun mediaUrl(path: String?): String? =
    when {
        path.isNullOrBlank() -> null
        path.startsWith("http") || path.startsWith("/") -> path
        else -> "/storage/$path"
    }

fun optionLetter(index: Int): String = ('A' + index).toString()

/** The answer area of one question (web QuestionResponse): one option, several, or a label per statement. */
@Composable
fun PracticeResponse(
    kind: ResponseKind,
    content: QuestionContent,
    answer: AnswerValue?,
    enabled: Boolean,
    onEvent: (PracticeRunnerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        when (kind) {
            ResponseKind.Statement -> {
                StatementTable(content, (answer as? AnswerValue.Statement)?.labels.orEmpty(), enabled, onEvent)
            }

            ResponseKind.Multiple -> {
                Hint(stringResource(R.string.runner_multiple_hint))
                val chosen = (answer as? AnswerValue.Multiple)?.options.orEmpty()
                content.options.forEachIndexed { index, option ->
                    OptionRow(index, option, index in chosen, enabled, square = true) {
                        onEvent(PracticeRunnerEvent.ToggleOption(index))
                    }
                }
            }

            ResponseKind.Single -> {
                val chosen = (answer as? AnswerValue.Single)?.option
                content.options.forEachIndexed { index, option ->
                    OptionRow(index, option, index == chosen, enabled, square = false) {
                        onEvent(PracticeRunnerEvent.SelectOption(index))
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    index: Int,
    option: OptionText,
    selected: Boolean,
    enabled: Boolean,
    square: Boolean,
    onClick: () -> Unit,
) {
    val colors = GotestTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    GotestTheme.sizes.strokeThin,
                    if (selected) colors.borderBrandDark else colors.borderSubtle,
                    MaterialTheme.shapes.medium,
                ).background(if (selected) colors.bgBrandSoft else colors.bgApp, MaterialTheme.shapes.medium)
                .clickable(enabled = enabled, role = if (square) Role.Checkbox else Role.RadioButton, onClick = onClick)
                .semantics { this.selected = selected }
                .padding(GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .background(
                        if (selected) colors.bgBrandDark else colors.bgSubtle,
                        if (square) MaterialTheme.shapes.small else CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = optionLetter(index),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.textOnBrand else colors.textPrimary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            RichText(
                content = option.text,
                isHtml = true,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            mediaUrl(option.mediaPath)?.let {
                ExamImage(url = it, description = stringResource(R.string.runner_option_image, optionLetter(index)))
            }
        }
    }
}

/** One label per statement; two to four labels from the server, otherwise Benar / Salah. */
@Composable
private fun StatementTable(
    content: QuestionContent,
    answers: List<Int?>,
    enabled: Boolean,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    val labels =
        content.answerLabels.takeIf { it.size in 2..MAX_LABELS }?.map { it.text }
            ?: listOf(stringResource(R.string.runner_label_true), stringResource(R.string.runner_label_false))
    Hint(stringResource(R.string.runner_statement_hint, labels.joinToString(" / ")))
    content.statements.forEachIndexed { row, statement ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium)
                    .padding(GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            RichText(
                content = statement.text,
                isHtml = true,
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                labels.forEachIndexed { index, label ->
                    GotestFilterPill(
                        text = label,
                        selected = answers.getOrNull(row) == index,
                        onClick = { if (enabled) onEvent(PracticeRunnerEvent.StatementAnswer(row, index)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

private const val MAX_LABELS = 4
