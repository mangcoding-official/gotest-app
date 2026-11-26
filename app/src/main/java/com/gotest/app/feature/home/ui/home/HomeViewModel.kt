package com.gotest.app.feature.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.home.domain.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Beranda (T-026 A2c-1 = a): `GET /dashboard`, read when the page opens and again each time the student comes back
 * to it, as the web reloads the page; the name and photo in the greeting come from `/akun`.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        userRepository: UserRepository,
        private val repository: DashboardRepository,
        notices: SessionNotices,
    ) : ViewModel() {
        /** Why the sign-up's Mitra link was dropped, right after the email was verified (DEC-154); shown once. */
        val emailVerifiedReason: String? = notices.takeEmailVerified()

        private val page = MutableStateFlow<PageLoad>(PageLoad.Loading)
        private val packageType = MutableStateFlow(PackageType.Practice)
        private val activityType = MutableStateFlow(PackageType.Practice)
        private var request: Job? = null

        private val profile =
            userRepository
                .observeProfile()
                .map<_, HomeProfileSummary?> {
                    HomeProfileSummary(
                        firstName = it.firstName,
                        avatarUrl = it.avatarUrl,
                        hasUnreadNotifications = false,
                    )
                }.onStart { emit(null) }
                // The greeting waits for `/akun`; Beranda itself does not.
                .catch { emit(null) }

        val uiState: StateFlow<HomeUiState> =
            combine(profile, page, packageType, activityType) { profile, load, packages, activity ->
                when (load) {
                    PageLoad.Loading -> {
                        HomeUiState.Loading
                    }

                    PageLoad.Failed -> {
                        HomeUiState.Error
                    }

                    is PageLoad.Loaded -> {
                        HomeUiState.Ready(
                            profile = profile ?: HomeProfileSummary("", null, false),
                            page = load.page,
                            packageType = packages,
                            activityType = activity,
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

        init {
            fetch()
        }

        fun onEvent(event: HomeUiEvent) {
            when (event) {
                is HomeUiEvent.SelectRecommendedType -> packageType.value = event.type
                is HomeUiEvent.SelectActivityType -> activityType.value = event.type
                HomeUiEvent.Retry -> retry()
                HomeUiEvent.Refresh -> if (page.value is PageLoad.Loaded) fetch()
            }
        }

        private fun retry() {
            page.value = PageLoad.Loading
            fetch()
        }

        /** A failed reload keeps the page already shown; only a first load that fails shows the error. */
        private fun fetch() {
            request?.cancel()
            request =
                viewModelScope.launch {
                    repository
                        .dashboard()
                        .onSuccess { loaded -> page.value = PageLoad.Loaded(loaded) }
                        .onFailure { page.update { if (it is PageLoad.Loaded) it else PageLoad.Failed } }
                }
        }

        private sealed interface PageLoad {
            data object Loading : PageLoad

            data object Failed : PageLoad

            data class Loaded(
                val page: DashboardPage,
            ) : PageLoad
        }
    }
