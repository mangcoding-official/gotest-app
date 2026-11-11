package com.gotest.app.feature.exam.ui.result

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.exam.domain.ReviewRules

sealed interface ExamResultUiState {
    data object Loading : ExamResultUiState

    data object Error : ExamResultUiState

    @Immutable
    data class Success(
        val result: AttemptResult,
    ) : ExamResultUiState {
        val isTryout: Boolean get() = result.type == PackageType.Tryout
        val showsRank: Boolean get() = ReviewRules.showsRank(result)
        val averagePercent: Int get() = ReviewRules.averagePercent(result.correct, result.wrong, result.empty)
    }
}

sealed interface ExamResultUiEvent {
    data object Retry : ExamResultUiEvent
}
