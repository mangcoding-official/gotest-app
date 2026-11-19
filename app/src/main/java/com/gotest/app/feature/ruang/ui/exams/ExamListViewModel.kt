package com.gotest.app.feature.ruang.ui.exams

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamSchedule
import com.gotest.app.feature.ruang.domain.MyExamStatus
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.ScheduleState
import com.gotest.app.navigation.UjianListDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a schedule card's button does (Ruang/Exams/Index.tsx, checked in this order). */
sealed interface ExamCardAction {
    data class Result(
        val attemptId: Long,
    ) : ExamCardAction

    data class Continue(
        val attemptId: Long,
    ) : ExamCardAction

    /** The start page: "Buka Ujian" while running, "Lihat Detail" before it opens. */
    data class Detail(
        val scheduleId: Long,
        val running: Boolean,
    ) : ExamCardAction

    companion object {
        fun of(
            schedule: ExamSchedule,
            former: Boolean,
        ): ExamCardAction? {
            val attempt = schedule.attemptId
            return when {
                schedule.myStatus == MyExamStatus.Submitted && attempt != null -> Result(attempt)
                former -> null
                schedule.myStatus == MyExamStatus.InProgress && attempt != null -> Continue(attempt)
                schedule.state == ScheduleState.Closed -> null
                else -> Detail(schedule.id, running = schedule.state == ScheduleState.Running)
            }
        }
    }
}

@Immutable
data class ExamListUiState(
    val isLoading: Boolean = true,
    val page: ExamListPage? = null,
    val former: Boolean = false,
)

/** "Ujian" of one Mitra (web: Ruang/Exams/Index). */
@HiltViewModel
class ExamListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val exams: ExamRepository,
        private val ruang: RuangRepository,
    ) : ViewModel() {
        val code: String = savedStateHandle.toRoute<UjianListDestination>().code

        private val _uiState = MutableStateFlow(ExamListUiState())
        val uiState: StateFlow<ExamListUiState> = _uiState.asStateFlow()

        /** Reloaded when the screen comes back (after an Ujian, its status changed). */
        fun load() {
            _uiState.update { it.copy(isLoading = it.page == null) }
            viewModelScope.launch {
                val members = async { ruang.memberships() }
                val result = exams.schedules(code)
                val former =
                    members
                        .await()
                        .getOrNull()
                        ?.firstOrNull { it.code == code }
                        ?.former ?: false
                _uiState.update {
                    ExamListUiState(isLoading = false, page = result.getOrNull() ?: it.page, former = former)
                }
            }
        }
    }
