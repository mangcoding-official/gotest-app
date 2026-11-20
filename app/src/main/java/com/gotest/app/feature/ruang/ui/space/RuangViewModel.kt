package com.gotest.app.feature.ruang.ui.space

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.ui.catalog.PracticeCatalogViewModel
import com.gotest.app.feature.practice.ui.catalog.StartProblem
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangPractice
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.ui.tasks.TaskActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    /** The Latihan sekolah whose Mulai is on its way. */
    val startingPracticeId: Long? = null,
    /** A refused start under its Latihan card. */
    val practiceError: Pair<Long, StartProblem>? = null,
)

sealed interface RuangEffect {
    data class OpenPractice(
        val attemptId: Long,
        val packageName: String,
    ) : RuangEffect

    data class OpenPracticeResult(
        val attemptId: Long,
        val packageName: String,
    ) : RuangEffect
}

/**
 * A student's Mitra space (web: Ruang/Show): Tugas hari ini and the plan progress (Rencana Belajar, T-031), Ujian,
 * the school's Try Out sessions, Latihan sekolah, and Perkembanganmu.
 */
@HiltViewModel
class RuangViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: RuangRepository,
        private val practice: PracticeRepository,
        plans: StudyPlanRepository,
        local: ExamLocalStore,
    ) : ViewModel() {
        // Read by name (the RuangDestination field), so the view model also runs in unit tests.
        val code: String = checkNotNull(savedStateHandle[ARG_CODE])

        private val _uiState = MutableStateFlow(RuangUiState())
        val uiState: StateFlow<RuangUiState> = _uiState.asStateFlow()

        private val _effects = Channel<RuangEffect>(Channel.BUFFERED)
        val effects: Flow<RuangEffect> = _effects.receiveAsFlow()

        val tasks = TaskActions(viewModelScope, code, plans, local, onChanged = ::refresh)

        private var request: Job? = null

        init {
            load()
        }

        fun retry() = load()

        /**
         * Back from a quiz, a Latihan, or a Try Out, or after a record: the page again, keeping what is on screen
         * meanwhile. A request still on its way is cancelled and sent again, so the page shows the newest state.
         */
        fun refresh() {
            if (_uiState.value.page == null) return
            fetch()
        }

        /** Lanjutkan, Mulai Latihan or Latihan Lagi, and Lihat Pembahasan of a Latihan sekolah card. */
        fun onPractice(
            item: RuangPractice,
            action: PracticeAction,
        ) {
            when (action) {
                PracticeAction.Continue -> {
                    item.continueAttemptId?.let { _effects.trySend(RuangEffect.OpenPractice(it, item.name)) }
                }

                PracticeAction.Review -> {
                    item.lastAttemptId?.let { _effects.trySend(RuangEffect.OpenPracticeResult(it, item.name)) }
                }

                PracticeAction.Start -> {
                    startPractice(item)
                }
            }
        }

        private fun startPractice(item: RuangPractice) {
            if (_uiState.value.startingPracticeId != null) return
            _uiState.update { it.copy(startingPracticeId = item.id, practiceError = null) }
            viewModelScope.launch {
                practice
                    .startInRuang(code, item.id)
                    .onSuccess { attempt ->
                        _uiState.update { it.copy(startingPracticeId = null) }
                        _effects.send(RuangEffect.OpenPractice(attempt.id, item.name))
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                startingPracticeId = null,
                                practiceError = item.id to PracticeCatalogViewModel.startError(error),
                            )
                        }
                    }
            }
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
                                it.copy(
                                    isLoading = false,
                                    loadFailed = false,
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

        companion object {
            const val ARG_CODE = "code"
            private const val FAMILY = "keluarga"
        }
    }

enum class PracticeAction { Continue, Start, Review }
