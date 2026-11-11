package com.gotest.app.feature.studypackage.ui.studypackage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.StudyCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val RECENT_ACTIVITY_LIMIT = 3
private const val TICK_MILLIS = 1_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudyPackageViewModel
    @Inject
    constructor(
        private val studyPackageRepository: StudyPackageRepository,
        private val examRepository: ExamRepository,
    ) : ViewModel() {
        private val selectedTab = MutableStateFlow(PackageType.Practice)
        private val selectedJenjangId = MutableStateFlow<String?>(null)
        private val refreshKey = MutableStateFlow(0)

        private val content: Flow<StudyPackageContent> =
            combine(selectedTab, refreshKey) { tab, _ -> tab }
                .flatMapLatest(::observeContent)
                .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

        private val countdown: Flow<Countdown?> =
            content
                .map { (it as? StudyPackageContent.Success)?.seasonalTryout?.endsAtEpochMillis }
                .distinctUntilChanged()
                .flatMapLatest { endsAt ->
                    if (endsAt == null) {
                        flowOf(null)
                    } else {
                        clockTicks().map { now -> Countdown.between(nowMillis = now, endMillis = endsAt) }
                    }
                }

        val uiState: StateFlow<StudyPackageUiState> =
            combine(selectedTab, selectedJenjangId, content, countdown) { tab, jenjangId, content, countdown ->
                val categories = (content as? StudyPackageContent.Success)?.categories.orEmpty()
                StudyPackageUiState(
                    selectedTab = tab,
                    jenjangOptions = categories,
                    selectedJenjangId = jenjangId,
                    content =
                        if (content is StudyPackageContent.Success) {
                            content.copy(categories = categories.filterByJenjang(jenjangId))
                        } else {
                            content
                        },
                    countdown = countdown,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudyPackageUiState())

        fun onEvent(event: StudyPackageUiEvent) {
            when (event) {
                is StudyPackageUiEvent.TabSelected -> selectedTab.value = event.tab
                is StudyPackageUiEvent.JenjangSelected -> selectedJenjangId.value = event.categoryId
                StudyPackageUiEvent.Retry -> refreshKey.update { it + 1 }
            }
        }

        private fun observeContent(tab: PackageType): Flow<StudyPackageContent> {
            val categories = studyPackageRepository.observeCategories(tab)
            val content =
                when (tab) {
                    PackageType.Practice -> {
                        combine(categories, examRepository.observeHistory()) { items, history ->
                            contentOf(items, recentActivities = history.take(RECENT_ACTIVITY_LIMIT))
                        }
                    }

                    PackageType.Tryout -> {
                        combine(categories, studyPackageRepository.observeSeasonalTryout()) { items, seasonal ->
                            contentOf(items, seasonalTryout = seasonal)
                        }
                    }
                }
            return content
                .onStart { emit(StudyPackageContent.Loading) }
                .catch { emit(StudyPackageContent.Error) }
        }

        private fun contentOf(
            categories: List<StudyCategory>,
            recentActivities: List<AttemptSummary> = emptyList(),
            seasonalTryout: SeasonalTryout? = null,
        ): StudyPackageContent =
            if (categories.isEmpty()) {
                StudyPackageContent.Empty
            } else {
                StudyPackageContent.Success(categories, recentActivities, seasonalTryout)
            }

        /** Emits the wall clock once a second while collected. */
        private fun clockTicks(): Flow<Long> =
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(System.currentTimeMillis())
                    delay(TICK_MILLIS)
                }
            }
    }

/** The dropdown narrows the list to one jenjang; an unknown id (stale filter) shows everything. */
private fun List<StudyCategory>.filterByJenjang(categoryId: String?): List<StudyCategory> =
    if (categoryId == null) this else filter { it.id == categoryId }.ifEmpty { this }
