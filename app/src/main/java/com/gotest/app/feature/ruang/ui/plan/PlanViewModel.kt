package com.gotest.app.feature.ruang.ui.plan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.RoomPlan
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.ui.tasks.TaskActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PlanUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: StudyPlanPage? = null,
    /** The plan chosen in "Pilih rencana"; the first (main) plan until one is chosen. */
    val chosenId: Long? = null,
    val family: Boolean = false,
) {
    val plan: RoomPlan?
        get() = page?.plans?.let { plans -> plans.firstOrNull { it.id == chosenId } ?: plans.firstOrNull() }
}

/**
 * The child's Rencana Belajar in a Mitra space (web: Ruang/Plan, T-031): every week with its tasks and their
 * status, and the child's own progress. Tasks open the same actions as Tugas hari ini; the plan is read only.
 */
@HiltViewModel
class PlanViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: StudyPlanRepository,
        private val ruang: RuangRepository,
        local: ExamLocalStore,
    ) : ViewModel() {
        // Read by name (the RencanaDestination field), so the view model also runs in unit tests.
        val code: String = checkNotNull(savedStateHandle[ARG_CODE])

        private val _uiState = MutableStateFlow(PlanUiState(chosenId = savedStateHandle[KEY_CHOSEN]))
        val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

        val tasks = TaskActions(viewModelScope, code, repository, local, onChanged = ::refresh)

        private var request: Job? = null

        init {
            load()
        }

        fun retry() = load()

        fun choose(planId: Long) {
            savedStateHandle[KEY_CHOSEN] = planId
            _uiState.update { it.copy(chosenId = planId) }
        }

        /**
         * Back from a quiz or a Latihan, or after a record: the page again, keeping what is on screen meanwhile. A
         * request still on its way is cancelled and sent again, so the page shows the newest state.
         */
        fun refresh() {
            if (_uiState.value.page == null) return
            fetch()
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            fetch()
        }

        private fun fetch() {
            request?.cancel()
            request =
                viewModelScope.launch {
                    val members = async { ruang.memberships() }
                    repository
                        .plan(code)
                        .onSuccess { page ->
                            val member = members.await().getOrNull()?.firstOrNull { it.code == code }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadFailed = false,
                                    page = page,
                                    family =
                                        member?.type == FAMILY,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, loadFailed = it.page == null) }
                        }
                }
        }

        companion object {
            const val ARG_CODE = "code"
            private const val KEY_CHOSEN = "plan.chosen"
            private const val FAMILY = "keluarga"
        }
    }
