package com.gotest.app.feature.account

import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import com.gotest.app.feature.account.ui.components.AccountAccessViewModel
import com.gotest.app.feature.account.ui.components.AccountKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/** T-026 required 14: a profile that cannot be read is never taken for an adult account. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdultOnlyTest {
    private var failing = true
    private var managedBy: String? = null

    private val users =
        object : UserRepository {
            override fun observeProfile(): Flow<UserProfile> =
                flow {
                    if (failing) throw IOException("offline")
                    emit(profile(managedBy))
                }

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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aProfileThatFailsToLoadIsAnErrorUntilItIsRead() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = AccountAccessViewModel(users)
            backgroundScope.launch { vm.kind.collect {} }
            assertEquals(AccountKind.Failed, vm.kind.value)

            failing = false
            vm.retry()
            assertEquals(AccountKind.Adult, vm.kind.value)

            managedBy = "Keluarga Permana"
            vm.retry()
            assertEquals(AccountKind.Child, vm.kind.value)
        }

    private fun profile(managedBy: String?) =
        UserProfile(
            id = "1",
            fullName = "Gilang Permana",
            email = "gilangpermana@mail.com",
            avatarUrl = null,
            stats = UserStats(0, 0, 0, 0, 0, 0),
            subscription = Subscription(PlanTier.FreeTrial, "Gratis", null, LocalDate.of(2026, 12, 1), null),
            managedBy = managedBy,
        )
}
