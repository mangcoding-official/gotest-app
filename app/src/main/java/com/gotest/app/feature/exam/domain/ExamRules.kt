package com.gotest.app.feature.exam.domain

import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer

/**
 * Tallies for the navigator header and the finish confirmation.
 *
 * [answered] counts every answered question, [marked] every marked one, and [empty] the
 * questions that are neither, so the three tiles match the navigator's colours.
 */
data class ExamCounts(
    val total: Int,
    val answered: Int,
    val marked: Int,
    val empty: Int,
) {
    /** Questions still without an answer, marked or not. */
    val unanswered: Int get() = total - answered
}

/** True when the question comes with a passage or picture for the blue stimulus card. */
val Question.hasStimulus: Boolean
    get() = stimulusTitle != null || stimulusBody != null || imageUrl != null

/** Colour of one cell in "Navigasi Soal"; a marked question wins over an answered one. */
enum class NavigatorCellStatus { Current, Marked, Answered, Empty }

data class NavigatorCell(
    val questionId: String,
    val number: Int,
    val status: NavigatorCellStatus,
)

/** Pure navigation and counting rules of the question engine, kept out of the ViewModel. */
object ExamRules {
    fun isAnswered(answer: QuestionAnswer?): Boolean = answer != null && !answer.isEmpty

    fun counts(
        questions: List<Question>,
        answers: Map<String, QuestionAnswer>,
        markedIds: Set<String>,
    ): ExamCounts {
        val answered = questions.count { isAnswered(answers[it.id]) }
        val marked = questions.count { it.id in markedIds }
        val empty = questions.count { it.id !in markedIds && !isAnswered(answers[it.id]) }
        return ExamCounts(total = questions.size, answered = answered, marked = marked, empty = empty)
    }

    private fun cellStatus(
        index: Int,
        currentIndex: Int,
        question: Question,
        answers: Map<String, QuestionAnswer>,
        markedIds: Set<String>,
    ): NavigatorCellStatus =
        when {
            index == currentIndex -> NavigatorCellStatus.Current
            question.id in markedIds -> NavigatorCellStatus.Marked
            isAnswered(answers[question.id]) -> NavigatorCellStatus.Answered
            else -> NavigatorCellStatus.Empty
        }

    fun navigatorCells(
        questions: List<Question>,
        currentIndex: Int,
        answers: Map<String, QuestionAnswer>,
        markedIds: Set<String>,
    ): List<NavigatorCell> =
        questions.mapIndexed { index, question ->
            NavigatorCell(
                questionId = question.id,
                number = index + 1,
                status = cellStatus(index, currentIndex, question, answers, markedIds),
            )
        }

    /** A resumed attempt opens on its first unanswered question (the design resumes on "Soal 4"). */
    fun resumeIndex(
        questions: List<Question>,
        answers: Map<String, QuestionAnswer>,
    ): Int = questions.indexOfFirst { !isAnswered(answers[it.id]) }.coerceAtLeast(0)

    fun previousIndex(currentIndex: Int): Int = (currentIndex - 1).coerceAtLeast(0)

    fun nextIndex(
        currentIndex: Int,
        total: Int,
    ): Int = (currentIndex + 1).coerceAtMost((total - 1).coerceAtLeast(0))

    fun clampIndex(
        index: Int,
        total: Int,
    ): Int = index.coerceIn(0, (total - 1).coerceAtLeast(0))

    fun isLast(
        currentIndex: Int,
        total: Int,
    ): Boolean = currentIndex >= total - 1

    /** Fill of the thin bar under the header: the position of the current question. */
    fun progress(
        currentIndex: Int,
        total: Int,
    ): Float = if (total == 0) 0f else (currentIndex + 1).toFloat() / total

    /** One second of the tryout countdown; never below zero. */
    fun tick(remainingSeconds: Int): Int = (remainingSeconds - 1).coerceAtLeast(0)
}
