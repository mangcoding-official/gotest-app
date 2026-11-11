package com.gotest.app.feature.history.data

import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.feature.history.domain.HistoryAttempt
import com.gotest.app.feature.history.domain.HistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val PERCENT = 100

/**
 * Adds an accuracy to the shared attempt history. The seeded attempts share one dummy result,
 * so they get fixed accuracies to make the "Akurasi" sorts visible; attempts finished in the
 * app use their graded result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FakeHistoryRepository
    @Inject
    constructor(
        private val examRepository: ExamRepository,
    ) : HistoryRepository {
        override fun observeAttempts(): Flow<List<HistoryAttempt>> =
            examRepository.observeHistory().flatMapLatest { history ->
                if (history.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(history.map(::observeAttempt)) { it.toList() }
                }
            }

        private fun observeAttempt(summary: AttemptSummary): Flow<HistoryAttempt> {
            val seeded = seededAccuracy[summary.attemptId]
            return if (seeded != null) {
                flowOf(HistoryAttempt(summary, seeded))
            } else {
                examRepository.observeResult(summary.attemptId).map { HistoryAttempt(summary, it.accuracyPercent()) }
            }
        }

        private fun AttemptResult.accuracyPercent(): Int {
            val total = correct + wrong + empty
            return if (total == 0) 0 else correct * PERCENT / total
        }

        private companion object {
            val seededAccuracy =
                mapOf(
                    "att_tryout_utbk_2" to 72,
                    "att_latihan_sept_1" to 78,
                    "att_latihan_logaritma" to 70,
                    "att_tryout_utbk_1" to 60,
                    "att_latihan_twk_1" to 65,
                )
        }
    }
