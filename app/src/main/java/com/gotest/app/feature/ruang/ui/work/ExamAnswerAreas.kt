package com.gotest.app.feature.ruang.ui.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.component.question.AnswerOptionState
import com.gotest.app.core.designsystem.component.question.GotestAnswerOption
import com.gotest.app.core.designsystem.component.question.GotestCheckOption
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamContent

/** A, B, C… (the web's `optionLetter`). */
fun optionLetter(index: Int): String = ('A' + index).toString()

/**
 * The answerable part of one item for all five types (web: ExamItemAnswer.tsx). [onAnswer] is null
 * while locked; null passed to it clears the answer.
 */
@Composable
fun ExamAnswerArea(
    content: ExamContent,
    answer: ExamAnswer?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        when (content) {
            is ExamContent.Choice -> {
                if (content.multiple) {
                    MultipleChoice(content, answer as? ExamAnswer.Multiple, onAnswer)
                } else {
                    SingleChoice(content, answer as? ExamAnswer.Single, onAnswer)
                }
            }

            is ExamContent.TrueFalse -> {
                TrueFalse(content, answer as? ExamAnswer.TrueFalse, onAnswer)
            }

            is ExamContent.Matching -> {
                Matching(content, answer as? ExamAnswer.Matching, onAnswer)
            }

            is ExamContent.ShortAnswer -> {
                ShortAnswer(content, answer as? ExamAnswer.Text, onAnswer)
            }
        }
    }
}

/** Tapping the chosen option again keeps it; "Kosongkan jawaban" clears it, as on the web. */
@Composable
private fun SingleChoice(
    content: ExamContent.Choice,
    answer: ExamAnswer.Single?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
) {
    AreaColumn {
        content.options.forEachIndexed { index, option ->
            GotestAnswerOption(
                letter = optionLetter(index),
                text = option.text,
                state = if (answer?.selected == option.id) AnswerOptionState.Selected else AnswerOptionState.Default,
                onClick = onAnswer?.let { { it(ExamAnswer.Single(option.id)) } },
            )
            option.mediaUrl?.let {
                ExamImage(url = it, description = stringResource(R.string.work_option_image, optionLetter(index)))
            }
        }
    }
}

@Composable
private fun MultipleChoice(
    content: ExamContent.Choice,
    answer: ExamAnswer.Multiple?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
) {
    AreaColumn {
        Hint(stringResource(R.string.work_multiple_hint))
        val selected = answer?.selected.orEmpty()
        content.options.forEachIndexed { index, option ->
            GotestCheckOption(
                text = option.text,
                checked = option.id in selected,
                onCheckedChange =
                    onAnswer?.let { change ->
                        { checked: Boolean ->
                            val next = if (checked) selected + option.id else selected - option.id
                            change(ExamAnswer.Multiple(next))
                        }
                    },
            )
            option.mediaUrl?.let {
                ExamImage(url = it, description = stringResource(R.string.work_option_image, optionLetter(index)))
            }
        }
    }
}

/** Tapping the chosen label again clears that statement, as on the web. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrueFalse(
    content: ExamContent.TrueFalse,
    answer: ExamAnswer.TrueFalse?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
) {
    AreaColumn {
        val chosen = answer?.answers.orEmpty()
        Hint(stringResource(R.string.work_true_false_hint, content.labels[0], content.labels[1]))
        content.statements.forEachIndexed { index, statement ->
            NumberedBox(number = index + 1) {
                RichText(
                    content = statement.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textPrimary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                    content.labels.forEachIndexed { labelIndex, label ->
                        GotestFilterPill(
                            text = label,
                            selected = chosen[statement.id] == labelIndex,
                            onClick = {
                                onAnswer?.invoke(
                                    ExamAnswer.TrueFalse(
                                        if (chosen[statement.id] ==
                                            labelIndex
                                        ) {
                                            chosen - statement.id
                                        } else {
                                            chosen + (statement.id to labelIndex)
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (chosen.isNotEmpty() && chosen.size < content.statements.size) {
            Hint(stringResource(R.string.work_true_false_partial, chosen.size, content.statements.size))
        }
    }
}

/**
 * One right entry per left entry. Each row picks a letter from "Pilihan jawaban", which shows the full
 * answers with their math (T-026 A2b-5); a letter already used moves to this row, as on the web.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Matching(
    content: ExamContent.Matching,
    answer: ExamAnswer.Matching?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
) {
    AreaColumn {
        val pairs = answer?.pairs.orEmpty()
        val colors = GotestTheme.colors
        Hint(stringResource(R.string.work_matching_hint))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle, MaterialTheme.shapes.small)
                    .padding(GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.work_matching_choices),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            content.right.forEachIndexed { index, entry ->
                Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
                    Text(
                        text = "${optionLetter(index)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                    RichText(
                        content = entry.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary,
                    )
                }
            }
        }
        content.left.forEachIndexed { index, left ->
            val pairLabel = stringResource(R.string.work_matching_pair, index + 1)
            NumberedBox(number = index + 1) {
                RichText(content = left.text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                FlowRow(
                    modifier = Modifier.semantics { contentDescription = pairLabel },
                    horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                ) {
                    content.right.forEachIndexed { rightIndex, right ->
                        GotestFilterPill(
                            text = optionLetter(rightIndex),
                            selected = pairs[left.id] == right.id,
                            onClick = {
                                val next =
                                    if (pairs[left.id] == right.id) {
                                        pairs - left.id
                                    } else {
                                        pairs.filterValues { it != right.id } + (left.id to right.id)
                                    }
                                onAnswer?.invoke(ExamAnswer.Matching(next))
                            },
                        )
                    }
                }
            }
        }
        if (pairs.isNotEmpty() && pairs.size < content.left.size) {
            Hint(stringResource(R.string.work_matching_partial, pairs.size, content.left.size))
        }
    }
}

@Composable
private fun ShortAnswer(
    content: ExamContent.ShortAnswer,
    answer: ExamAnswer.Text?,
    onAnswer: ((ExamAnswer?) -> Unit)?,
) {
    AreaColumn {
        Hint(stringResource(if (content.numeric) R.string.work_short_number_hint else R.string.work_short_text_hint))
        val limit = if (content.numeric) NUMBER_MAX else TEXT_MAX
        GotestTextField(
            value = answer?.text.orEmpty(),
            onValueChange = { typed -> onAnswer?.invoke(ExamAnswer.Text(typed.take(limit))) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(if (content.numeric) NUMBER_PLACEHOLDER else TEXT_PLACEHOLDER),
            enabled = onAnswer != null,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (content.numeric) KeyboardType.Decimal else KeyboardType.Text,
                ),
        )
    }
}

/** The parts of one answer area, stacked like the web's. */
@Composable
private fun AreaColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm), content = content)
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** A numbered statement or left entry with its choices. */
@Composable
private fun NumberedBox(
    number: Int,
    content: @Composable () -> Unit,
) {
    val colors = GotestTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) { content() }
        }
    }
}

private val NUMBER_PLACEHOLDER = R.string.work_short_number_placeholder
private val TEXT_PLACEHOLDER = R.string.work_short_text_placeholder
private const val NUMBER_MAX = 20
private const val TEXT_MAX = 50
