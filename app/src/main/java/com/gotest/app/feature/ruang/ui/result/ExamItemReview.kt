package com.gotest.app.feature.ruang.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamResultItem
import com.gotest.app.feature.ruang.ui.work.optionLetter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** The student's answer next to the key, for every type (web: ExamItemReview.tsx). Points are not shown. */
@Composable
fun ExamItemReview(
    item: ExamResultItem,
    modifier: Modifier = Modifier,
) {
    val type = item.type
    val raw = item.content
    val key = item.answerKey
    if (type == null || raw == null || key == null) return
    val content = ExamContent.of(type, raw)
    val answer = ExamAnswer.fromJson(type, item.answer)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        when (content) {
            is ExamContent.Choice -> {
                ChoiceReview(content, answer, key)
            }

            is ExamContent.TrueFalse -> {
                TrueFalseReview(content, answer as? ExamAnswer.TrueFalse, key)
            }

            is ExamContent.Matching -> {
                MatchingReview(content, answer as? ExamAnswer.Matching, key)
            }

            is ExamContent.ShortAnswer -> {
                ReviewRow(stringResource(R.string.result_yours), (answer as? ExamAnswer.Text)?.text)
                ReviewRow(stringResource(R.string.result_accepted), key.strings("accepted").joinToString(" / "))
            }
        }
    }
}

@Composable
private fun ChoiceReview(
    content: ExamContent.Choice,
    answer: ExamAnswer?,
    key: JsonObject,
) {
    val colors = GotestTheme.colors
    val chosen =
        when (answer) {
            is ExamAnswer.Single -> setOf(answer.selected)
            is ExamAnswer.Multiple -> answer.selected.toSet()
            else -> emptySet()
        }
    val correct = key.strings("correct").toSet()
    content.options.forEachIndexed { index, option ->
        val isKey = option.id in correct
        val wrongPick = option.id in chosen && !isKey
        val background: Color =
            when {
                isKey -> colors.statusSuccessSoft
                wrongPick -> colors.statusDangerSoft
                else -> colors.bgApp
            }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(background, MaterialTheme.shapes.small)
                    .padding(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
                Text(
                    text = "${optionLetter(index)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                RichText(content = option.text, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                if (option.id in chosen) Tag(stringResource(R.string.result_yours), colors.textBrandDark)
                if (isKey) Tag(stringResource(R.string.result_key), colors.statusSuccess)
            }
        }
    }
}

@Composable
private fun TrueFalseReview(
    content: ExamContent.TrueFalse,
    answer: ExamAnswer.TrueFalse?,
    key: JsonObject,
) {
    val keys = (key["answers"] as? JsonObject).orEmpty()
    content.statements.forEachIndexed { index, statement ->
        Numbered(index + 1, statement.text) {
            ReviewRow(
                stringResource(R.string.result_yours),
                answer?.answers?.get(statement.id)?.let(content.labels::getOrNull),
            )
            ReviewRow(
                stringResource(R.string.result_key),
                keys[statement.id]?.jsonPrimitive?.intOrNull?.let(content.labels::getOrNull),
            )
        }
    }
}

@Composable
private fun MatchingReview(
    content: ExamContent.Matching,
    answer: ExamAnswer.Matching?,
    key: JsonObject,
) {
    val keys = (key["pairs"] as? JsonObject).orEmpty()

    fun rightText(id: String?): String? {
        val index = content.right.indexOfFirst { it.id == id }
        return if (index < 0) null else "${optionLetter(index)}. ${content.right[index].text}"
    }
    content.left.forEachIndexed { index, left ->
        Numbered(index + 1, left.text) {
            ReviewRow(stringResource(R.string.result_yours), rightText(answer?.pairs?.get(left.id)))
            ReviewRow(stringResource(R.string.result_key), rightText(keys[left.id]?.jsonPrimitive?.contentOrNull))
        }
    }
}

@Composable
private fun Numbered(
    number: Int,
    text: String,
    rows: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            RichText(content = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textPrimary)
            rows()
        }
    }
}

/** "Jawabanmu" / "Kunci" with its value, or "Tidak dijawab". */
@Composable
private fun ReviewRow(
    label: String,
    value: String?,
) {
    val colors = GotestTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        if (value.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.result_not_answered),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            RichText(content = value, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
        }
    }
}

@Composable
private fun Tag(
    text: String,
    color: Color,
) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

/** A key field that is one string or a list of strings (`correct`, `accepted`). */
private fun JsonObject.strings(name: String): List<String> =
    when (val value = this[name]) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> listOfNotNull(value.contentOrNull)
        else -> emptyList()
    }
