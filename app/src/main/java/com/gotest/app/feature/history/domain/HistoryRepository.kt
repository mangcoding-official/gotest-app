package com.gotest.app.feature.history.domain

import com.gotest.app.core.model.AttemptSummary
import kotlinx.coroutines.flow.Flow

/** A finished attempt with the share of its questions answered correctly. */
data class HistoryAttempt(
    val summary: AttemptSummary,
    val accuracyPercent: Int,
)

/**
 * Riwayat's attempts. [AttemptSummary] has no accuracy, which the "Akurasi" sort orders and the
 * export lists, so Riwayat reads its attempts from here.
 */
interface HistoryRepository {
    /** Finished attempts, newest first. */
    fun observeAttempts(): Flow<List<HistoryAttempt>>
}
