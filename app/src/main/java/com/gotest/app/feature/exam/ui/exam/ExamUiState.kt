package com.gotest.app.feature.exam.ui.exam

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.ExamSession
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.exam.domain.ExamCounts
import com.gotest.app.feature.exam.domain.ExamRules
import com.gotest.app.feature.exam.domain.NavigatorCell

sealed interface ExamUiState {
    data object Loading : ExamUiState

    data object Error : ExamUiState

    /** The package has no questions. */
    data object Empty : ExamUiState

    @Immutable
    data class Success(
        val attemptId: String,
        val title: String,
        val type: PackageType,
        val questions: List<Question>,
        val answers: Map<String, QuestionAnswer>,
        val markedIds: Set<String>,
        val currentIndex: Int,
        /** Null for untimed latihan. */
        val remainingSeconds: Int?,
        val isNavigatorOpen: Boolean = false,
        val isFinishConfirmOpen: Boolean = false,
        val isSubmitting: Boolean = false,
    ) : ExamUiState {
        val currentQuestion: Question get() = questions[currentIndex]
        val currentAnswer: QuestionAnswer get() = answers[currentQuestion.id] ?: QuestionAnswer()
        val isCurrentMarked: Boolean get() = currentQuestion.id in markedIds
        val isFirst: Boolean get() = currentIndex == 0
        val isLast: Boolean get() = ExamRules.isLast(currentIndex, questions.size)
        val isTryout: Boolean get() = type == PackageType.Tryout
        val progress: Float get() = ExamRules.progress(currentIndex, questions.size)
        val counts: ExamCounts get() = ExamRules.counts(questions, answers, markedIds)
        val navigatorCells: List<NavigatorCell>
            get() = ExamRules.navigatorCells(questions, currentIndex, answers, markedIds)
    }

    companion object {
        fun from(session: ExamSession): ExamUiState =
            if (session.questions.isEmpty()) {
                Empty
            } else {
                Success(
                    attemptId = session.attemptId,
                    title = session.title,
                    type = session.type,
                    questions = session.questions,
                    answers = session.answers,
                    markedIds = session.markedQuestionIds,
                    currentIndex = ExamRules.resumeIndex(session.questions, session.answers),
                    remainingSeconds = session.remainingSeconds,
                )
            }
    }
}

sealed interface ExamUiEvent {
    data class AnswerChanged(
        val answer: QuestionAnswer,
    ) : ExamUiEvent

    data object ToggleMark : ExamUiEvent

    data object Previous : ExamUiEvent

    data object Next : ExamUiEvent

    data object OpenNavigator : ExamUiEvent

    data object DismissNavigator : ExamUiEvent

    data class JumpTo(
        val index: Int,
    ) : ExamUiEvent

    /** "Selesaikan" on the last question or "Selesaikan Tryout" in the navigator. */
    data object RequestFinish : ExamUiEvent

    data object DismissFinish : ExamUiEvent

    data object ConfirmFinish : ExamUiEvent

    data object Retry : ExamUiEvent
}

/** One-off results the route turns into navigation or a snackbar. */
sealed interface ExamEffect {
    data class Submitted(
        val attemptId: String,
    ) : ExamEffect

    data object SubmitFailed : ExamEffect

    /** An answer or a mark could not be saved. */
    data object SaveFailed : ExamEffect
}
