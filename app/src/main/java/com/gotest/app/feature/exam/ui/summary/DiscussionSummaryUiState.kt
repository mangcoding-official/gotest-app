package com.gotest.app.feature.exam.ui.summary

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.feature.exam.domain.ReviewRules
import com.gotest.app.feature.exam.domain.ReviewStatus
import com.gotest.app.feature.exam.domain.ReviewTally
import com.gotest.app.feature.exam.domain.status

/** One review card: the question, the student's answer and its 1-based number. */
data class SummaryItem(
    val number: Int,
    val review: QuestionReview,
    val status: ReviewStatus,
)

sealed interface DiscussionSummaryUiState {
    data object Loading : DiscussionSummaryUiState

    data object Error : DiscussionSummaryUiState

    /** The attempt has no reviewed questions. */
    data object Empty : DiscussionSummaryUiState

    @Immutable
    data class Success(
        val packageTitle: String,
        val tally: ReviewTally,
        val score: Int,
        val topics: List<String>,
        /** Null is "Semua Topik". */
        val selectedTopic: String?,
        val items: List<SummaryItem>,
    ) : DiscussionSummaryUiState

    companion object {
        fun from(
            packageTitle: String,
            score: Int,
            reviews: List<QuestionReview>,
            selectedTopic: String?,
        ): DiscussionSummaryUiState {
            if (reviews.isEmpty()) return Empty
            val topics = ReviewRules.topics(reviews)
            val topic = selectedTopic?.takeIf { it in topics }
            val numbered = reviews.mapIndexed { index, review -> SummaryItem(index + 1, review, review.status) }
            return Success(
                packageTitle = packageTitle,
                tally = ReviewRules.tally(reviews),
                score = score,
                topics = topics,
                selectedTopic = topic,
                items = if (topic == null) numbered else numbered.filter { it.review.question.topic == topic },
            )
        }
    }
}

sealed interface DiscussionSummaryUiEvent {
    /** [topic] null selects "Semua Topik". */
    data class SelectTopic(
        val topic: String?,
    ) : DiscussionSummaryUiEvent

    data object Retry : DiscussionSummaryUiEvent
}
