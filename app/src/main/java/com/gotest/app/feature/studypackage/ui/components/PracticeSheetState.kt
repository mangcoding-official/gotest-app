package com.gotest.app.feature.studypackage.ui.components

import androidx.compose.runtime.Immutable
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.StudyPackageDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Content of the "Detail Latihan" bottom sheet; null in the owning UiState means closed. */
sealed interface PracticeSheetState {
    data object Loading : PracticeSheetState

    data object Error : PracticeSheetState

    @Immutable
    data class Success(
        val detail: StudyPackageDetail,
    ) : PracticeSheetState
}

/** Which package the sheet shows; bumping [attempt] reloads the same package after an error. */
data class PracticeSheetRequest(
    val packageId: String,
    val attempt: Int = 0,
)

/** Loads the sheet for every [requests] value (null closes it). */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun StudyPackageRepository.observePracticeSheet(
    requests: Flow<PracticeSheetRequest?>,
): Flow<PracticeSheetState?> =
    requests.flatMapLatest { request ->
        if (request == null) {
            flowOf(null)
        } else {
            observePackageDetail(request.packageId)
                .map<StudyPackageDetail, PracticeSheetState?> { PracticeSheetState.Success(it) }
                .onStart { emit(PracticeSheetState.Loading) }
                .catch { emit(PracticeSheetState.Error) }
        }
    }
