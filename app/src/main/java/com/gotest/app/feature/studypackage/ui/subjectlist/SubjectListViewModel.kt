package com.gotest.app.feature.studypackage.ui.subjectlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.Subject
import com.gotest.app.navigation.SubjectListDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubjectListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: StudyPackageRepository,
    ) : ViewModel() {
        /** Starts at the tapped category; the jenjang dropdown can switch it. */
        private val selectedCategoryId =
            MutableStateFlow(savedStateHandle.toRoute<SubjectListDestination>().categoryId)
        private val refreshKey = MutableStateFlow(0)

        val uiState: StateFlow<SubjectListUiState> =
            combine(selectedCategoryId, refreshKey) { categoryId, _ -> categoryId }
                .flatMapLatest(::observeState)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = SubjectListUiState.Loading,
                )

        fun onEvent(event: SubjectListUiEvent) {
            when (event) {
                is SubjectListUiEvent.CategorySelected -> selectedCategoryId.value = event.categoryId
                SubjectListUiEvent.Retry -> refreshKey.update { it + 1 }
            }
        }

        private fun observeState(categoryId: String): Flow<SubjectListUiState> =
            combine(
                repository.observeCategory(categoryId),
                repository.observeSubjects(categoryId),
                repository.observeCategories(PackageType.Practice),
                ::stateOf,
            ).onStart { emit(SubjectListUiState.Loading) }
                .catch { emit(SubjectListUiState.Error) }

        private fun stateOf(
            category: StudyCategory,
            subjects: List<Subject>,
            categories: List<StudyCategory>,
        ): SubjectListUiState {
            val header = SubjectListHeader(category = category, categories = categories)
            return if (subjects.isEmpty()) {
                SubjectListUiState.Empty(header)
            } else {
                SubjectListUiState.Success(header, subjects)
            }
        }
    }
