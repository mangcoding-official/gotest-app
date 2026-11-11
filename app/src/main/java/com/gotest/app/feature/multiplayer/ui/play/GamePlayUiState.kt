package com.gotest.app.feature.multiplayer.ui.play

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.multiplayer.domain.GameType

sealed interface GamePlayUiState {
    data object Loading : GamePlayUiState

    data object Error : GamePlayUiState

    @Immutable
    data class Success(
        val type: GameType,
        /** Duel opponent shown in the header; null for rooms. */
        val opponentName: String?,
        val playerCount: Int,
        val questions: List<Question>,
        val currentIndex: Int,
        val answers: Map<String, QuestionAnswer>,
        val remainingSeconds: Int,
        /** True while answers cannot be sent (frame 19377). */
        val isOffline: Boolean = false,
        /** Numbers of the questions whose answers wait for the connection. */
        val unsavedQuestionNumbers: List<Int> = emptyList(),
        val sheet: PlaySheet? = null,
        val isSubmitting: Boolean = false,
    ) : GamePlayUiState {
        val current: Question get() = questions[currentIndex]
        val isFirst: Boolean get() = currentIndex == 0
        val isLast: Boolean get() = currentIndex == questions.lastIndex
        val answeredCount: Int get() = questions.count { answers[it.id]?.isEmpty == false }
        val emptyCount: Int get() = questions.size - answeredCount

        fun answerFor(question: Question): QuestionAnswer = answers[question.id] ?: QuestionAnswer()
    }
}

enum class PlaySheet {
    /** "Navigasi Soal", as in the exam flow. */
    Navigator,

    /** "Selesai sekarang?" (frame 19162). */
    ConfirmFinish,
}

sealed interface GamePlayUiEvent {
    data object Retry : GamePlayUiEvent

    data class Answer(
        val answer: QuestionAnswer,
    ) : GamePlayUiEvent

    data object Next : GamePlayUiEvent

    data object Previous : GamePlayUiEvent

    data class GoTo(
        val index: Int,
    ) : GamePlayUiEvent

    data object OpenNavigator : GamePlayUiEvent

    data object OpenFinish : GamePlayUiEvent

    data object DismissSheet : GamePlayUiEvent

    data object ConfirmFinish : GamePlayUiEvent
}

sealed interface GamePlayEffect {
    /** Answers are in; wait for the others. */
    data class Submitted(
        val code: String,
    ) : GamePlayEffect
}
