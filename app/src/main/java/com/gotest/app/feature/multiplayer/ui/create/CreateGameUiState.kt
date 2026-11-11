package com.gotest.app.feature.multiplayer.ui.create

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.GameCategory
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameSubtest
import com.gotest.app.feature.multiplayer.domain.GameType

sealed interface CreateGameUiState {
    data object Loading : CreateGameUiState

    data object Error : CreateGameUiState

    @Immutable
    data class Success(
        val form: CreateGameForm,
        val isSubmitting: Boolean = false,
    ) : CreateGameUiState
}

data class DurationOption(
    val minutes: Int,
    val enabled: Boolean,
)

/** "Buat permainan" form. Derived values keep the rules (stock, seconds per question) in one place. */
@Immutable
data class CreateGameForm(
    val categories: List<GameCategory>,
    val type: GameType = GameType.Duel,
    val categoryName: String = categories.first().name,
    val subtestName: String =
        categories
            .first()
            .subtests
            .first()
            .name,
    val selectedTopics: List<String> = emptyList(),
    val questionCount: Int = GameRules.DEFAULT_QUESTIONS,
    val durationMinutes: Int = GameRules.DEFAULT_DURATION_MINUTES,
) {
    val category: GameCategory get() = categories.firstOrNull { it.name == categoryName } ?: categories.first()

    val subtest: GameSubtest get() =
        category.subtests.firstOrNull { it.name == subtestName }
            ?: category.subtests.first()

    /** Topics that are not chosen yet, offered by "+ Tambah topik". */
    val addableTopics: List<String> get() = subtest.topics.map { it.name }.filterNot { it in selectedTopics }

    /** Questions in the bank for the chosen topics, or the whole subtest when none is chosen. */
    val availableQuestions: Int
        get() =
            subtest.topics
                .filter { selectedTopics.isEmpty() || it.name in selectedTopics }
                .sumOf { it.questionStock }

    val isStockShort: Boolean get() = questionCount > availableQuestions

    val durationOptions: List<DurationOption>
        get() =
            GameRules.durationOptionsMinutes.map {
                DurationOption(minutes = it, enabled = GameRules.isDurationAllowed(it, questionCount))
            }

    val disabledDurations: List<Int> get() = durationOptions.filterNot { it.enabled }.map { it.minutes }

    /** Shortest duration still allowed; the hint suggests it. */
    val suggestedDuration: Int? get() = durationOptions.firstOrNull { it.enabled }?.minutes

    val canDecrease: Boolean get() = questionCount > GameRules.MIN_QUESTIONS
    val canIncrease: Boolean get() = questionCount < GameRules.MAX_QUESTIONS

    val canSubmit: Boolean get() = !isStockShort && GameRules.isDurationAllowed(durationMinutes, questionCount)

    fun toSettings(): GameSettings =
        GameSettings(
            type = type,
            category = category.name,
            subtest = subtest.name,
            topics = selectedTopics,
            questionCount = questionCount,
            durationMinutes = durationMinutes,
        )

    /** Changes the count within the allowed range and moves the duration up if it became too short. */
    fun withQuestionCount(count: Int): CreateGameForm {
        if (count !in GameRules.MIN_QUESTIONS..GameRules.MAX_QUESTIONS) return this
        val updated = copy(questionCount = count)
        val duration =
            updated.durationOptions.firstOrNull { it.enabled && it.minutes >= durationMinutes }
                ?: updated.durationOptions.lastOrNull { it.enabled }
        return updated.copy(durationMinutes = duration?.minutes ?: durationMinutes)
    }

    fun withDuration(minutes: Int): CreateGameForm =
        if (durationOptions.any { it.minutes == minutes && it.enabled }) copy(durationMinutes = minutes) else this

    /**
     * "Buat Ulang dengan Pengaturan Sama": the form filled with [settings] through the same rules as
     * the controls, so a count or duration that is no longer allowed falls back and a stock shortfall
     * still shows. Settings the catalog no longer has leave the form unchanged.
     */
    fun withSettings(settings: GameSettings): CreateGameForm {
        val subtest =
            categories
                .firstOrNull { it.name == settings.category }
                ?.subtests
                ?.firstOrNull { it.name == settings.subtest }
                ?: return this
        val topics = subtest.topics.map { it.name }
        return copy(
            type = settings.type,
            categoryName = settings.category,
            subtestName = subtest.name,
            selectedTopics = settings.topics.filter { it in topics },
        ).withQuestionCount(settings.questionCount)
            .withDuration(settings.durationMinutes)
    }
}

sealed interface CreateGameUiEvent {
    data object Retry : CreateGameUiEvent

    data class TypeChange(
        val type: GameType,
    ) : CreateGameUiEvent

    data class CategoryChange(
        val name: String,
    ) : CreateGameUiEvent

    data class SubtestChange(
        val name: String,
    ) : CreateGameUiEvent

    data class TopicAdd(
        val name: String,
    ) : CreateGameUiEvent

    data class TopicRemove(
        val name: String,
    ) : CreateGameUiEvent

    data object DecreaseQuestions : CreateGameUiEvent

    data object IncreaseQuestions : CreateGameUiEvent

    data class DurationChange(
        val minutes: Int,
    ) : CreateGameUiEvent

    data object Submit : CreateGameUiEvent
}

sealed interface CreateGameEffect {
    data class Created(
        val code: String,
    ) : CreateGameEffect

    /** Creating failed, e.g. the student joined another game meanwhile. */
    data object Failed : CreateGameEffect
}
