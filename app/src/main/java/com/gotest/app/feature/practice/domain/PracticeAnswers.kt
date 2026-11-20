package com.gotest.app.feature.practice.domain

/** How a question is answered (web `responseKind`): one option, several, or a label per statement. */
enum class ResponseKind { Single, Multiple, Statement }

fun responseKind(questionType: String?): ResponseKind =
    when (questionType) {
        "multiple_select" -> ResponseKind.Multiple
        "statement_table" -> ResponseKind.Statement
        else -> ResponseKind.Single
    }

/** An answer as the student picks it (web `AnswerValue`): option indexes, or a label index per statement. */
sealed interface AnswerValue {
    data class Single(
        val option: Int,
    ) : AnswerValue

    data class Multiple(
        val options: List<Int>,
    ) : AnswerValue

    data class Statement(
        val labels: List<Int?>,
    ) : AnswerValue
}

/** Web `answerPayload`: the shape the question's type asks for. */
fun answerPayload(
    questionType: String?,
    value: AnswerValue,
): AnswerData =
    when (responseKind(questionType)) {
        ResponseKind.Multiple -> AnswerData(selectedOptions = (value as? AnswerValue.Multiple)?.options.orEmpty())
        ResponseKind.Statement -> AnswerData(statementAnswers = (value as? AnswerValue.Statement)?.labels.orEmpty())
        ResponseKind.Single -> AnswerData(selectedOption = (value as? AnswerValue.Single)?.option)
    }

/** Web `hydrateAnswer`: the saved answer of an item, or null. */
fun savedAnswer(item: PracticeItem): AnswerValue? =
    when (responseKind(item.questionType)) {
        ResponseKind.Multiple -> {
            item.selectedOptions.takeIf { it.isNotEmpty() }?.let(AnswerValue::Multiple)
        }

        ResponseKind.Statement -> {
            item.statementAnswers
                .takeIf { list ->
                    list.any { it != null }
                }?.let(AnswerValue::Statement)
        }

        ResponseKind.Single -> {
            item.selectedOption?.let(AnswerValue::Single)
        }
    }

/** Web `isItemAnswered`: every statement labelled, at least one option, or the single option chosen. */
fun isAnswered(
    item: PracticeItem,
    value: AnswerValue?,
): Boolean {
    val answer = value ?: savedAnswer(item)
    return when (responseKind(item.questionType)) {
        ResponseKind.Multiple -> {
            (answer as? AnswerValue.Multiple)?.options?.isNotEmpty() == true
        }

        ResponseKind.Statement -> {
            (answer as? AnswerValue.Statement)?.labels?.let {
                it.isNotEmpty() &&
                    it.all { label -> label != null }
            } ==
                true
        }

        ResponseKind.Single -> {
            answer is AnswerValue.Single || item.answeredAt != null || item.selectedOption != null
        }
    }
}
