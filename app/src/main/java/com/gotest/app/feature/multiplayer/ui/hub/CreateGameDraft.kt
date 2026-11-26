package com.gotest.app.feature.multiplayer.ui.hub

import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.OptionCategory
import com.gotest.app.feature.multiplayer.domain.OptionSubject
import com.gotest.app.feature.multiplayer.domain.OptionTopic

/**
 * The choices of "Buat permainan" and what follows from them, as the web's CreateGameModal: the stock of the
 * chosen subtest or topics, at most 30 questions, and no duration shorter than 30 seconds per question.
 */
data class CreateGameDraft(
    val mode: String = GameState.MODE_DUEL,
    val categoryId: Long? = null,
    val subjectId: Long? = null,
    val topicIds: List<Long> = emptyList(),
    val questionCount: Int = GameRules.DEFAULT_QUESTIONS,
    val durationMinutes: Int = GameRules.DEFAULT_DURATION_MINUTES,
) {
    fun category(categories: List<OptionCategory>): OptionCategory? =
        categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull()

    fun subject(categories: List<OptionCategory>): OptionSubject? {
        val subjects = category(categories)?.subjects.orEmpty()
        return subjects.firstOrNull { it.id == subjectId } ?: subjects.firstOrNull()
    }

    fun selectedTopics(categories: List<OptionCategory>): List<OptionTopic> =
        subject(categories)?.topics.orEmpty().filter { it.id in topicIds }

    fun otherTopics(categories: List<OptionCategory>): List<OptionTopic> =
        subject(categories)?.topics.orEmpty().filterNot { it.id in topicIds }

    fun stock(categories: List<OptionCategory>): Int {
        val subject = subject(categories) ?: return 0
        return if (topicIds.isEmpty()) subject.questionCount else selectedTopics(categories).sumOf { it.questionCount }
    }

    fun maxCount(categories: List<OptionCategory>): Int = minOf(GameRules.MAX_QUESTIONS, stock(categories))

    fun count(categories: List<OptionCategory>): Int = minOf(questionCount, maxOf(1, maxCount(categories)))

    fun minimumDuration(categories: List<OptionCategory>): Int = GameRules.shortestDuration(count(categories))

    fun duration(categories: List<OptionCategory>): Int = maxOf(durationMinutes, minimumDuration(categories))

    fun tooShort(categories: List<OptionCategory>): List<Int> =
        GameRules.durationChoices.filter { it < minimumDuration(categories) }

    fun pickCategory(
        id: Long,
        categories: List<OptionCategory>,
    ) = copy(
        categoryId = id,
        subjectId =
            categories
                .firstOrNull { it.id == id }
                ?.subjects
                ?.firstOrNull()
                ?.id,
        topicIds = emptyList(),
    )

    fun pickSubject(id: Long) = copy(subjectId = id, topicIds = emptyList())

    /** The settings sent to the server; null while the choice has no questions. */
    fun settings(categories: List<OptionCategory>): GameSettings? {
        val category = category(categories)
        val subject = subject(categories)
        if (category == null || subject == null || maxCount(categories) == 0) return null
        return GameSettings(
            mode = mode,
            categoryId = category.id,
            subjectId = subject.id,
            topicIds = topicIds,
            questionCount = count(categories),
            durationMinutes = duration(categories),
        )
    }
}
