package com.gotest.app.core.model

enum class QuestionType {
    /** Pilihan Ganda: one correct option. */
    MultipleChoice,

    /** PG Kompleks: every correct option must be ticked. */
    ComplexMultipleChoice,

    /** Benar / Salah: each statement is judged true or false. */
    TrueFalse,
}

data class QuestionOption(
    val key: String,
    val text: String,
)

data class QuestionStatement(
    val number: Int,
    val text: String,
)

data class Question(
    val id: String,
    val number: Int,
    val topic: String,
    val type: QuestionType,
    val stimulusTitle: String?,
    val stimulusBody: String?,
    /** Coil model for an optional illustration (the water tank in the design). */
    val imageUrl: String?,
    val prompt: String,
    /** Extra instruction for PG Kompleks and Benar/Salah. */
    val instruction: String?,
    val options: List<QuestionOption>,
    val statements: List<QuestionStatement>,
    val correctKeys: Set<String>,
    val correctStatements: Map<Int, Boolean>,
    val explanation: String,
)

/** What the student picked; only the field matching the question type is used. */
data class QuestionAnswer(
    val selectedKeys: Set<String> = emptySet(),
    val statementAnswers: Map<Int, Boolean> = emptyMap(),
) {
    val isEmpty: Boolean get() = selectedKeys.isEmpty() && statementAnswers.isEmpty()
}

fun Question.isAnsweredCorrectly(answer: QuestionAnswer): Boolean =
    when (type) {
        QuestionType.TrueFalse -> answer.statementAnswers == correctStatements
        else -> answer.selectedKeys == correctKeys
    }
