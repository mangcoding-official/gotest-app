package com.gotest.app.feature.home

import androidx.compose.runtime.snapshots.Snapshot
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.home.domain.DashboardRepository
import com.gotest.app.feature.home.domain.SearchPage
import com.gotest.app.feature.home.domain.SearchRepository
import com.gotest.app.feature.home.ui.home.HomePackage
import com.gotest.app.feature.home.ui.home.HomeUiEvent
import com.gotest.app.feature.home.ui.home.HomeUiState
import com.gotest.app.feature.home.ui.home.HomeViewModel
import com.gotest.app.feature.home.ui.search.SearchUiEvent
import com.gotest.app.feature.home.ui.search.SearchUiState
import com.gotest.app.feature.home.ui.search.SearchViewModel
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.tryout.domain.TryoutPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** T-026 A2c-1: Beranda keeps what it showed when a reload fails, and Cari waits for the typing to stop. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val replies = ArrayDeque<Result<DashboardPage>>()
    private val searched = mutableListOf<String>()

    private val dashboard =
        object : DashboardRepository {
            override suspend fun dashboard() = replies.removeFirst()
        }

    private val users =
        object : UserRepository {
            override fun observeProfile(): Flow<UserProfile> = emptyFlow()

            override suspend fun updateProfile(
                fullName: String,
                email: String,
            ) = Result.success(Unit)

            override suspend fun updateAvatar(avatarUrl: String) = Result.success(Unit)

            override suspend fun changePassword(
                currentPassword: String,
                newPassword: String,
            ) = Result.success(Unit)

            override suspend fun deleteAccount() = Result.success(Unit)
        }

    private fun TestScope.viewModel(): HomeViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return HomeViewModel(users, dashboard, SessionNotices())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aFirstLoadThatFailsShowsTheErrorAndRetryLoadsAgain() =
        runTest {
            replies += Result.failure(IOException())
            replies += Result.success(DashboardPage())
            val vm = viewModel()
            backgroundScope.launch { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(HomeUiState.Error, vm.uiState.value)

            vm.onEvent(HomeUiEvent.Retry)
            advanceUntilIdle()

            assertTrue(vm.uiState.value is HomeUiState.Ready)
        }

    @Test
    fun aReloadThatFailsKeepsThePageShown() =
        runTest {
            val first = DashboardPage(practicePackages = listOf(PracticePackage(1, "Latihan 1")))
            replies += Result.success(first)
            replies += Result.failure(IOException())
            val vm = viewModel()
            backgroundScope.launch { vm.uiState.collect {} }
            advanceUntilIdle()

            vm.onEvent(HomeUiEvent.Refresh)
            vm.onEvent(HomeUiEvent.SelectRecommendedType(PackageType.Tryout))
            advanceUntilIdle()

            val ready = vm.uiState.value as HomeUiState.Ready
            assertEquals(first, ready.page)
            assertEquals(PackageType.Tryout, ready.packageType)
        }

    @Test
    fun theCardButtonFollowsTheWebsRules() {
        fun practice(item: PracticePackage) = HomePackage.Practice(item).actionLabel

        fun tryout(item: TryoutPackage) = HomePackage.Tryout(item).actionLabel

        assertEquals(
            R.string.home_package_continue_practice,
            practice(PracticePackage(1, "A", continueAttemptId = 9, premiumLocked = true)),
        )
        assertEquals(R.string.practice_premium_only, practice(PracticePackage(1, "A", premiumLocked = true)))
        assertEquals(
            R.string.home_package_review,
            practice(PracticePackage(1, "A", finishedCount = 1, allowRetry = false, premiumLocked = true)),
        )
        assertEquals(R.string.home_package_retry_practice, practice(PracticePackage(1, "A", finishedCount = 2)))
        assertEquals(R.string.home_package_detail, tryout(TryoutPackage(1, "B", finishedCount = 1, allowRetry = false)))
        assertEquals(R.string.home_package_start_tryout, tryout(TryoutPackage(1, "B")))
    }

    @Test
    fun searchAsksOnceTheTypingStops() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm =
                SearchViewModel(
                    object : SearchRepository {
                        override suspend fun search(query: String): Result<SearchPage> {
                            searched += query
                            return Result.failure(IOException())
                        }
                    },
                )
            backgroundScope.launch { vm.uiState.collect {} }

            // The field's text is Compose state; the app's frames tell snapshotFlow about each change.
            vm.onEvent(SearchUiEvent.QueryChange("ut"))
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()
            vm.onEvent(SearchUiEvent.QueryChange(" utbk "))
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            assertEquals(listOf("ut", "utbk"), searched)
            // A failed search shows no groups: "Tidak ada hasil yang cocok.", as on the web.
            assertEquals(SearchUiState.Results(query = "utbk"), vm.uiState.value)
        }
}
