package com.gotest.app.feature.exam.domain

import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.QuestionType

/** Outcome of one question in Pembahasan. */
enum class ReviewStatus { Correct, Wrong, Empty }

val QuestionReview.status: ReviewStatus
    get() =
        when {
            isEmpty -> ReviewStatus.Empty
            isCorrect -> ReviewStatus.Correct
            else -> ReviewStatus.Wrong
        }

data class ReviewTally(
    val correct: Int,
    val wrong: Int,
    val empty: Int,
)

/** One piece of an answer as the review cards print it ("C. 24", "1. Benar"). */
sealed interface AnswerPart {
    data class Option(
        val key: String,
        val text: String,
    ) : AnswerPart

    data class Statement(
        val number: Int,
        val isTrue: Boolean,
    ) : AnswerPart
}

/** Tone of a topic accuracy bar on the result screen. */
enum class AccuracyLevel { High, Medium, Low }

/** Pure rules behind Hasil and Pembahasan. */
object ReviewRules {
    /** Every question is worth one point in the review chips ("Benar • 1/1"). */
    const val MAX_POINTS = 1

    private const val PERCENT = 100
    private const val HIGH_ACCURACY = 75
    private const val MEDIUM_ACCURACY = 50

    fun points(status: ReviewStatus): Int = if (status == ReviewStatus.Correct) MAX_POINTS else 0

    fun tally(reviews: List<QuestionReview>): ReviewTally {
        val statuses = reviews.map { it.status }
        return ReviewTally(
            correct = statuses.count { it == ReviewStatus.Correct },
            wrong = statuses.count { it == ReviewStatus.Wrong },
            empty = statuses.count { it == ReviewStatus.Empty },
        )
    }

    /** Topics in question order, for the "Semua Topik" filter row. */
    fun topics(reviews: List<QuestionReview>): List<String> = reviews.map { it.question.topic }.distinct()

    /** Keeps a 1-based question number inside the attempt. */
    fun clampNumber(
        number: Int,
        total: Int,
    ): Int = number.coerceIn(1, total.coerceAtLeast(1))

    fun answerParts(
        question: Question,
        answer: QuestionAnswer,
    ): List<AnswerPart> =
        when (question.type) {
            QuestionType.TrueFalse -> {
                answer.statementAnswers.toSortedMap().map { (number, value) -> AnswerPart.Statement(number, value) }
            }

            else -> {
                question.options
                    .filter { it.key in answer.selectedKeys }
                    .map { AnswerPart.Option(it.key, it.text) }
            }
        }

    fun correctParts(question: Question): List<AnswerPart> =
        answerParts(
            question,
            QuestionAnswer(selectedKeys = question.correctKeys, statementAnswers = question.correctStatements),
        )

    /** "Rata-rata" on the result screen: the share of correct answers over the whole attempt. */
    fun averagePercent(
        correct: Int,
        wrong: Int,
        empty: Int,
    ): Int {
        val total = correct + wrong + empty
        return if (total == 0) 0 else correct * PERCENT / total
    }

    fun accuracyLevel(percent: Int): AccuracyLevel =
        when {
            percent >= HIGH_ACCURACY -> AccuracyLevel.High
            percent >= MEDIUM_ACCURACY -> AccuracyLevel.Medium
            else -> AccuracyLevel.Low
        }

    /** Ranks only mean something for tryouts, which every participant takes under the same rules. */
    fun showsRank(result: AttemptResult): Boolean =
        result.type == PackageType.Tryout && result.rank != null && result.participants != null
}
