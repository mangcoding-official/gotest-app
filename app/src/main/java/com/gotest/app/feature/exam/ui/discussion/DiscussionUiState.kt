package com.gotest.app.feature.exam.ui.discussion

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.feature.exam.domain.ReviewRules
import com.gotest.app.feature.exam.domain.ReviewStatus
import com.gotest.app.feature.exam.domain.status

/** One numbered cell of the Pembahasan strip. */
data class DiscussionStripItem(
    val questionId: String,
    val number: Int,
    val status: ReviewStatus,
)

sealed interface DiscussionUiState {
    data object Loading : DiscussionUiState

    data object Error : DiscussionUiState

    /** The attempt has no reviewed questions. */
    data object Empty : DiscussionUiState

    @Immutable
    data class Success(
        val packageTitle: String,
        val strip: List<DiscussionStripItem>,
        val current: QuestionReview,
        /** 1-based position of [current] in the attempt. */
        val currentNumber: Int,
    ) : DiscussionUiState {
        val currentStatus: ReviewStatus get() = current.status
        val hasPrevious: Boolean get() = currentNumber > 1
        val hasNext: Boolean get() = currentNumber < strip.size
    }

    companion object {
        fun from(
            packageTitle: String,
            reviews: List<QuestionReview>,
            number: Int,
        ): DiscussionUiState {
            if (reviews.isEmpty()) return Empty
            val currentNumber = ReviewRules.clampNumber(number, reviews.size)
            return Success(
                packageTitle = packageTitle,
                strip =
                    reviews.mapIndexed { index, review ->
                        DiscussionStripItem(review.question.id, index + 1, review.status)
                    },
                current = reviews[currentNumber - 1],
                currentNumber = currentNumber,
            )
        }
    }
}

sealed interface DiscussionUiEvent {
    data class SelectQuestion(
        val number: Int,
    ) : DiscussionUiEvent

    data object Previous : DiscussionUiEvent

    data object Next : DiscussionUiEvent

    data object Retry : DiscussionUiEvent
}
