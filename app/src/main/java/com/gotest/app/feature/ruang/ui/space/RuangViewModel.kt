package com.gotest.app.feature.ruang.ui.space

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.ui.tasks.TaskActions
import com.gotest.app.navigation.RuangDestination
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
data class RuangUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: RuangPage? = null,
    /** The Ujian card: the plan includes Ujian, or there is a schedule or result here (API-K7 B). */
    val hasExams: Boolean = false,
    /** A Mitra Keluarga: tasks are "dicatat orang tua", elsewhere "dicatat guru" (TF9, TF13). */
    val family: Boolean = false,
)

/**
 * A student's Mitra space (web: Ruang/Show): Tugas hari ini and the plan progress (Rencana Belajar, T-031), and
 * Ujian. Try Out, Latihan, and their progress join with A2c.
 */
@HiltViewModel
class RuangViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: RuangRepository,
        plans: StudyPlanRepository,
        local: ExamLocalStore,
    ) : ViewModel() {
        val code: String = savedStateHandle.toRoute<RuangDestination>().code

        private val _uiState = MutableStateFlow(RuangUiState())
        val uiState: StateFlow<RuangUiState> = _uiState.asStateFlow()

        val tasks = TaskActions(viewModelScope, code, plans, local, onChanged = ::refresh)

        private var request: Job? = null

        init {
            load()
        }

        fun retry() = load()

        /** Back from a quiz or after a record: the page again, keeping what is on screen meanwhile. */
        fun refresh() {
            if (_uiState.value.page == null || request?.isActive == true) return
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
                    val members = async { repository.memberships() }
                    repository
                        .ruang(code)
                        .onSuccess { page ->
                            val member = members.await().getOrNull()?.firstOrNull { it.code == code }
                            _uiState.update {
                                RuangUiState(
                                    isLoading = false,
                                    page = page,
                                    hasExams = member?.hasExams ?: false,
                                    family = member?.type == FAMILY,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, loadFailed = it.page == null) }
                        }
                }
        }

        private companion object {
            const val FAMILY = "keluarga"
        }
    }
