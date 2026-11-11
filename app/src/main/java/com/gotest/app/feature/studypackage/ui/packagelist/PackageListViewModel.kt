package com.gotest.app.feature.studypackage.ui.packagelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetRequest
import com.gotest.app.feature.studypackage.ui.components.observePracticeSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Paket Belajar. [com.gotest.app.navigation.PackageListDestination.categoryId] is not applied:
 * [com.gotest.app.core.model.StudyPackage] carries no category, so the list filters by type and level.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PackageListViewModel
    @Inject
    constructor(
        private val repository: StudyPackageRepository,
    ) : ViewModel() {
        private val selectedType = MutableStateFlow(PackageType.Practice)
        private val selectedLevel = MutableStateFlow<String?>(null)
        private val refreshKey = MutableStateFlow(0)
        private val sheetRequest = MutableStateFlow<PracticeSheetRequest?>(null)

        private val _effects = Channel<PackageListEffect>(Channel.BUFFERED)
        val effects: Flow<PackageListEffect> = _effects.receiveAsFlow()

        private val levels: Flow<List<String>> =
            repository
                .observeLevels()
                .onStart { emit(emptyList()) }
                .catch { emit(emptyList()) }

        private val content: Flow<PackageListContent> =
            combine(selectedType, selectedLevel, refreshKey) { type, level, _ -> type to level }
                .flatMapLatest { (type, level) ->
                    repository
                        .observePackages(type, level)
                        .map { packages ->
                            if (packages.isEmpty()) PackageListContent.Empty else PackageListContent.Success(packages)
                        }.onStart { emit(PackageListContent.Loading) }
                        .catch { emit(PackageListContent.Error) }
                }

        val uiState: StateFlow<PackageListUiState> =
            combine(
                selectedType,
                selectedLevel,
                levels,
                content,
                repository.observePracticeSheet(sheetRequest),
            ) { type, level, levels, content, sheet ->
                PackageListUiState(
                    selectedType = type,
                    levels = levels,
                    selectedLevel = level,
                    content = content,
                    sheet = sheet,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PackageListUiState())

        fun onEvent(event: PackageListUiEvent) {
            when (event) {
                is PackageListUiEvent.TypeSelected -> selectedType.value = event.type
                is PackageListUiEvent.LevelSelected -> selectedLevel.value = event.level
                PackageListUiEvent.Retry -> refreshKey.update { it + 1 }
                is PackageListUiEvent.PackageActionClicked -> openPackage(event)
                PackageListUiEvent.SheetDismissed -> sheetRequest.value = null
                PackageListUiEvent.SheetRetry -> sheetRequest.update { it?.copy(attempt = it.attempt + 1) }
                is PackageListUiEvent.SheetStartClicked -> startExam(event.packageId)
            }
        }

        private fun openPackage(event: PackageListUiEvent.PackageActionClicked) {
            when (event.pkg.type) {
                PackageType.Practice -> {
                    sheetRequest.value = PracticeSheetRequest(event.pkg.id)
                }

                PackageType.Tryout -> {
                    viewModelScope.launch {
                        _effects.send(
                            PackageListEffect.OpenTryout(event.pkg.id),
                        )
                    }
                }
            }
        }

        private fun startExam(packageId: String) {
            sheetRequest.value = null
            viewModelScope.launch { _effects.send(PackageListEffect.StartExam(packageId)) }
        }
    }
