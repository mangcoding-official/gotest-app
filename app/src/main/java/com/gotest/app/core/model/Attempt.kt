package com.gotest.app.core.model

import java.time.LocalDate

/** A running latihan or tryout. */
data class ExamSession(
    val attemptId: String,
    val packageId: String,
    val title: String,
    val type: PackageType,
    val questions: List<Question>,
    val answers: Map<String, QuestionAnswer>,
    val markedQuestionIds: Set<String>,
    /** Null for untimed practice. */
    val remainingSeconds: Int?,
)

data class TopicAccuracy(
    val topic: String,
    val percent: Int,
)

data class SectionResult(
    val name: String,
    val correct: Int,
    val wrong: Int,
    val empty: Int,
)

data class AttemptResult(
    val attemptId: String,
    val packageId: String,
    val title: String,
    val description: String,
    val examLabel: String,
    val type: PackageType,
    val score: Int,
    val rank: Int?,
    val participants: Int?,
    val correct: Int,
    val wrong: Int,
    val empty: Int,
    val topicAccuracy: List<TopicAccuracy>,
    /** Topic the result screen suggests practising next. */
    val weakestTopic: String?,
    val sections: List<SectionResult>,
    val completedOn: LocalDate,
    val durationSeconds: Int,
)

/** One row of Riwayat and of "Aktivitas Terkini". */
data class AttemptSummary(
    val attemptId: String,
    val packageId: String,
    val title: String,
    val type: PackageType,
    val examLabel: String,
    val completedOn: LocalDate,
    val durationSeconds: Int,
    val score: Int,
)

/** A question with the student's answer, for Pembahasan. */
data class QuestionReview(
    val question: Question,
    val answer: QuestionAnswer,
) {
    val isCorrect: Boolean get() = question.isAnsweredCorrectly(answer)
    val isEmpty: Boolean get() = answer.isEmpty
}
