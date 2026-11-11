package com.gotest.app.feature.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.home.domain.NotificationRepository
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

private const val STOP_TIMEOUT_MS = 5_000L

/** "Aktivitas Terkini" shows the three latest attempts; Riwayat has the rest. */
private const val MAX_ACTIVITY_ROWS = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val studyPackageRepository: StudyPackageRepository,
        private val examRepository: ExamRepository,
        private val notificationRepository: NotificationRepository,
    ) : ViewModel() {
        private val retryCount = MutableStateFlow(0)
        private val recommendedType = MutableStateFlow(PackageType.Practice)
        private val activityType = MutableStateFlow(PackageType.Practice)

        val uiState: StateFlow<HomeUiState> =
            retryCount
                .flatMapLatest { attempt ->
                    val state =
                        combine(
                            dashboardData(),
                            notificationRepository.observeUnreadCount(),
                            recommendedType,
                            activityType,
                            ::toUiState,
                        )
                    // Only a retry shows the spinner again; the first load starts from Loading anyway.
                    val withSpinner = if (attempt > 0) state.onStart { emit(HomeUiState.Loading) } else state
                    withSpinner.catch { emit(HomeUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

        fun onEvent(event: HomeUiEvent) {
            when (event) {
                is HomeUiEvent.SelectRecommendedType -> recommendedType.value = event.type
                is HomeUiEvent.SelectActivityType -> activityType.value = event.type
                HomeUiEvent.Retry -> retryCount.update { it + 1 }
            }
        }

        private fun dashboardData(): Flow<DashboardData> =
            combine(
                userRepository.observeProfile(),
                studyPackageRepository.observeInProgress(),
                studyPackageRepository.observeRecommended(PackageType.Practice),
                studyPackageRepository.observeRecommended(PackageType.Tryout),
                examRepository.observeHistory(),
                ::DashboardData,
            )

        private fun toUiState(
            data: DashboardData,
            unreadCount: Int,
            recommendedType: PackageType,
            activityType: PackageType,
        ): HomeUiState {
            val profile =
                HomeProfileSummary(
                    firstName = data.profile.firstName,
                    avatarUrl = data.profile.avatarUrl,
                    hasUnreadNotifications = unreadCount > 0,
                )
            val recommendations = HomeRecommendations(recommendedType, data.practice, data.tryout)
            if (data.inProgress == null && data.history.isEmpty()) {
                return HomeUiState.Empty(profile, recommendations, activityType)
            }
            return HomeUiState.Success(
                profile = profile,
                stats = data.profile.stats,
                inProgress = data.inProgress,
                recommendations = recommendations,
                activity =
                    HomeActivity(
                        selectedType = activityType,
                        attempts = data.history.filter { it.type == activityType }.take(MAX_ACTIVITY_ROWS),
                    ),
            )
        }

        private data class DashboardData(
            val profile: UserProfile,
            val inProgress: StudyPackage?,
            val practice: List<StudyPackage>,
            val tryout: List<StudyPackage>,
            val history: List<AttemptSummary>,
        )
    }
