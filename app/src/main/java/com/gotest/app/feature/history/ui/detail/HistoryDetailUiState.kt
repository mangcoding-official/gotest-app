package com.gotest.app.feature.history.ui.detail

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.AttemptResult
import java.time.LocalDate

/** One row of the "Peringkat" card: a participant's best result on this package. */
@Immutable
data class HistoryRankingRow(
    val rank: Int,
    val name: String,
    /** Only known for the student's own row until the leaderboard API returns attempt dates. */
    val completedOn: LocalDate?,
    val durationSeconds: Int?,
    val score: Int,
    val isCurrentUser: Boolean,
)

sealed interface HistoryDetailUiState {
    data object Loading : HistoryDetailUiState

    data object Error : HistoryDetailUiState

    /** The attempt is no longer in the student's history. */
    data object Empty : HistoryDetailUiState

    @Immutable
    data class Success(
        val result: AttemptResult,
        val ranking: List<HistoryRankingRow>,
    ) : HistoryDetailUiState
}

sealed interface HistoryDetailUiEvent {
    data object Retry : HistoryDetailUiEvent
}
