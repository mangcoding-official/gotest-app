package com.gotest.app.core.designsystem.component.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionType

/** "State" of the design's "Mobile/Answer Option". Correct/Wrong are for Pembahasan. */
enum class AnswerOptionState { Default, Selected, Correct, Wrong }

private data class OptionColors(
    val border: Color,
    val letterContainer: Color,
    val content: Color,
)

private fun GotestColors.optionColors(state: AnswerOptionState): OptionColors =
    when (state) {
        AnswerOptionState.Default -> OptionColors(borderSubtle, bgSubtle, textPrimary)
        AnswerOptionState.Selected -> OptionColors(bgBrand, bgBrandSoft, textBrandDark)
        AnswerOptionState.Correct -> OptionColors(bgGreen, bgGreenSoft, statusSuccess)
        AnswerOptionState.Wrong -> OptionColors(statusDanger, statusDangerSoft, statusDanger)
    }

/** One "Pilihan Ganda" option: letter tile plus text. Pass a null [onClick] in review mode. */
@Composable
fun GotestAnswerOption(
    letter: String,
    text: String,
    state: AnswerOptionState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors.optionColors(state)
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.small
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.selectable(
                            selected = state == AnswerOptionState.Selected,
                            role = Role.RadioButton,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
        shape = shape,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.border),
    ) {
        Row(
            modifier =
                Modifier
                    .height(IntrinsicSize.Min)
                    .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(GotestTheme.sizes.buttonLarge)
                        .fillMaxHeight()
                        .heightIn(min = GotestTheme.sizes.buttonLarge)
                        .background(colors.letterContainer, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = letter, style = MaterialTheme.typography.labelLarge, color = colors.content)
            }
            Text(
                text = text,
                modifier =
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.content,
            )
        }
    }
}

/** One "PG Kompleks" option: checkbox plus text. */
@Composable
fun GotestCheckOption(
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onCheckedChange != null) {
                        Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, if (checked) colors.borderFocus else colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(GotestTheme.sizes.buttonLarge), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (checked) GotestIcons.Bold.TickSquare else GotestIcons.Linear.Square),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconLg),
                    tint = if (checked) colors.iconBrandDark else colors.iconDefault,
                )
            }
            Text(
                text = text,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) colors.textBrandDark else colors.textPrimary,
            )
        }
    }
}

/** One "Benar-Salah" statement with its two choice buttons. [answer] is null until picked. */
@Composable
fun GotestStatementOption(
    number: Int,
    text: String,
    answer: Boolean?,
    onAnswer: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier =
                Modifier
                    .height(IntrinsicSize.Min)
                    .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(GotestTheme.sizes.buttonLarge)
                        .fillMaxHeight()
                        .background(colors.bgSubtle, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    StatementChoice(
                        label = stringResource(R.string.question_true),
                        selected = answer == true,
                        onClick = onAnswer?.let { { it(true) } },
                        modifier = Modifier.weight(1f),
                    )
                    StatementChoice(
                        label = stringResource(R.string.question_false),
                        selected = answer == false,
                        onClick = onAnswer?.let { { it(false) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatementChoice(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Surface(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (onClick != null) {
                        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        border =
            BorderStroke(
                GotestTheme.sizes.strokeThin,
                if (selected) colors.borderFocus else colors.borderSubtle,
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = GotestTheme.spacing.sm),
            style = GotestTheme.typography.buttonLarge,
            color = if (selected) colors.textBrandDark else colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** "State" of the design's "Mobile/Soal Cell" (question number tile). */
enum class QuestionCellState { Answered, Current, Empty, Correct, Wrong, Skipped }

/** Numbered tile used by the question navigator and the Pembahasan strip. */
@Composable
fun GotestQuestionCell(
    number: Int,
    state: QuestionCellState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val (container, content) =
        when (state) {
            QuestionCellState.Answered -> colors.bgGreen to colors.illustrationGreenSoft
            QuestionCellState.Current -> colors.bgApp to colors.textBrandDark
            QuestionCellState.Empty -> colors.bgSubtle to colors.textPlaceholder
            QuestionCellState.Correct -> colors.bgGreenSoft to colors.statusSuccess
            QuestionCellState.Wrong -> colors.statusDangerSoft to colors.statusDanger
            QuestionCellState.Skipped -> colors.statusWarningSoft to colors.statusWarning
        }
    val borderColor =
        when (state) {
            QuestionCellState.Current -> colors.borderFocus
            QuestionCellState.Wrong -> if (onClick == null) colors.borderSubtle else colors.statusDanger
            else -> colors.borderSubtle
        }
    Surface(
        modifier =
            modifier
                .heightIn(min = GotestTheme.sizes.buttonLarge)
                .then(
                    if (onClick !=
                        null
                    ) {
                        Modifier.selectable(selected = state == QuestionCellState.Current, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        border = BorderStroke(GotestTheme.sizes.strokeThin, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

/** Blue "stimulus" card with the passage (and optional picture) a question refers to. */
@Composable
fun GotestStimulusCard(
    title: String?,
    body: String?,
    imageModel: Any?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgBlueSoft, MaterialTheme.shapes.medium)
                .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = colors.bgBlueDark)
        }
        if (body != null) {
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = colors.bgBlueDark)
        }
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = title,
                modifier =
                    Modifier
                        .fillMaxWidth(QUESTION_IMAGE_WIDTH_FRACTION)
                        .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

private const val QUESTION_IMAGE_WIDTH_FRACTION = 0.65f

/**
 * The answerable part of a question for every [QuestionType]. With [onAnswerChange] null the
 * question is read-only; [revealAnswers] colours options correct/wrong for Pembahasan.
 */
@Composable
fun GotestQuestionOptions(
    question: Question,
    answer: QuestionAnswer,
    onAnswerChange: ((QuestionAnswer) -> Unit)?,
    modifier: Modifier = Modifier,
    revealAnswers: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        when (question.type) {
            QuestionType.MultipleChoice -> MultipleChoiceOptions(question, answer, onAnswerChange, revealAnswers)
            QuestionType.ComplexMultipleChoice -> ComplexChoiceOptions(question, answer, onAnswerChange)
            QuestionType.TrueFalse -> TrueFalseStatements(question, answer, onAnswerChange)
        }
    }
}

@Composable
private fun MultipleChoiceOptions(
    question: Question,
    answer: QuestionAnswer,
    onAnswerChange: ((QuestionAnswer) -> Unit)?,
    revealAnswers: Boolean,
) {
    question.options.forEach { option ->
        val picked = option.key in answer.selectedKeys
        val state =
            when {
                revealAnswers && option.key in question.correctKeys -> AnswerOptionState.Correct
                revealAnswers && picked -> AnswerOptionState.Wrong
                picked -> AnswerOptionState.Selected
                else -> AnswerOptionState.Default
            }
        GotestAnswerOption(
            letter = option.key,
            text = option.text,
            state = state,
            onClick = onAnswerChange?.let { { it(QuestionAnswer(selectedKeys = setOf(option.key))) } },
        )
    }
}

@Composable
private fun ComplexChoiceOptions(
    question: Question,
    answer: QuestionAnswer,
    onAnswerChange: ((QuestionAnswer) -> Unit)?,
) {
    question.options.forEach { option ->
        GotestCheckOption(
            text = option.text,
            checked = option.key in answer.selectedKeys,
            onCheckedChange =
                onAnswerChange?.let { change ->
                    { isChecked ->
                        val keys = if (isChecked) answer.selectedKeys + option.key else answer.selectedKeys - option.key
                        change(answer.copy(selectedKeys = keys))
                    }
                },
        )
    }
}

@Composable
private fun TrueFalseStatements(
    question: Question,
    answer: QuestionAnswer,
    onAnswerChange: ((QuestionAnswer) -> Unit)?,
) {
    question.statements.forEach { statement ->
        GotestStatementOption(
            number = statement.number,
            text = statement.text,
            answer = answer.statementAnswers[statement.number],
            onAnswer =
                onAnswerChange?.let { change ->
                    { value ->
                        change(
                            answer.copy(
                                statementAnswers =
                                    answer.statementAnswers + (statement.number to value),
                            ),
                        )
                    }
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuestionComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            GotestStimulusCard(
                title = "Rute Angkutan Pagi di Kampung Melati",
                body = "Dalam pemantauan selama satu pekan, rute A mengangkut 44 penumpang per hari.",
                imageModel = null,
            )
            GotestAnswerOption("A", "Rute A perlu dipantau lebih dulu.", AnswerOptionState.Default, onClick = {})
            GotestAnswerOption(
                "B",
                "Rute dengan jumlah penumpang paling rendah.",
                AnswerOptionState.Selected,
                onClick = {},
            )
            GotestAnswerOption("C", "24", AnswerOptionState.Correct, onClick = null)
            GotestAnswerOption("D", "120", AnswerOptionState.Wrong, onClick = null)
            GotestCheckOption(text = "Semua rute harus diberi kendaraan.", checked = true, onCheckedChange = {})
            GotestStatementOption(
                1,
                "Volume akhir dapat dihitung dengan 504 − 110 + 150.",
                answer = true,
                onAnswer = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                QuestionCellState.entries.forEachIndexed { i, state ->
                    GotestQuestionCell(number = i + 1, state = state, onClick = {
                    }, modifier = Modifier.size(GotestTheme.sizes.questionCell))
                }
            }
        }
    }
}
