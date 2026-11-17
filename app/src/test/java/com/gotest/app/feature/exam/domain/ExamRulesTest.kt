package com.gotest.app.feature.exam.domain

import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRulesTest {
    private val questions = (1..4).map(::question)
    private val answered = QuestionAnswer(selectedKeys = setOf("a"))

    @Test
    fun `an empty answer does not count as answered`() {
        assertFalse(ExamRules.isAnswered(null))
        assertFalse(ExamRules.isAnswered(QuestionAnswer()))
        assertTrue(ExamRules.isAnswered(answered))
        assertTrue(ExamRules.isAnswered(QuestionAnswer(statementAnswers = mapOf(0 to true))))
    }

    @Test
    fun `counts split answered, marked and empty`() {
        val counts =
            ExamRules.counts(
                questions,
                mapOf("q1" to answered, "q2" to answered),
                markedIds = setOf("q2", "q3"),
            )
        assertEquals(ExamCounts(total = 4, answered = 2, marked = 2, empty = 1), counts)
        assertEquals(2, counts.unanswered)
    }

    @Test
    fun `navigator shows current first, then marked over answered`() {
        val cells =
            ExamRules.navigatorCells(
                questions,
                currentIndex = 0,
                answers =
                    mapOf(
                        "q1" to answered,
                        "q2" to answered,
                    ),
                markedIds = setOf("q2"),
            )
        assertEquals(
            listOf(
                NavigatorCellStatus.Current,
                NavigatorCellStatus.Marked,
                NavigatorCellStatus.Empty,
                NavigatorCellStatus.Empty,
            ),
            cells.map { it.status },
        )
        assertEquals(listOf(1, 2, 3, 4), cells.map { it.number })
    }

    @Test
    fun `a resumed attempt opens on the first unanswered question`() {
        assertEquals(2, ExamRules.resumeIndex(questions, mapOf("q1" to answered, "q2" to answered)))
        assertEquals(0, ExamRules.resumeIndex(questions, questions.associate { it.id to answered }))
    }

    @Test
    fun `moving between questions stays inside the paper`() {
        assertEquals(0, ExamRules.previousIndex(0))
        assertEquals(3, ExamRules.nextIndex(3, total = 4))
        assertEquals(0, ExamRules.nextIndex(0, total = 0))
        assertEquals(3, ExamRules.clampIndex(9, total = 4))
        assertTrue(ExamRules.isLast(3, total = 4))
        assertFalse(ExamRules.isLast(2, total = 4))
    }

    @Test
    fun `progress and the countdown`() {
        assertEquals(0.5f, ExamRules.progress(1, total = 4))
        assertEquals(0f, ExamRules.progress(0, total = 0))
        assertEquals(59, ExamRules.tick(60))
        assertEquals(0, ExamRules.tick(0))
    }

    private fun question(number: Int) =
        Question(
            id = "q$number",
            number = number,
            topic = "Topik",
            type = QuestionType.MultipleChoice,
            stimulusTitle = null,
            stimulusBody = null,
            imageUrl = null,
            prompt = "Soal $number",
            instruction = null,
            options = emptyList(),
            statements = emptyList(),
            correctKeys = setOf("a"),
            correctStatements = emptyMap(),
            explanation = "",
        )
}
