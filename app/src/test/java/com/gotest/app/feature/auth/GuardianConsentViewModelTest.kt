package com.gotest.app.feature.auth

import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.auth.domain.ConsentState
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.RegistrationInfo
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpRepository
import com.gotest.app.feature.auth.domain.SignUpResult
import com.gotest.app.feature.auth.ui.consent.GuardianConsentEffect
import com.gotest.app.feature.auth.ui.consent.GuardianConsentUiEvent
import com.gotest.app.feature.auth.ui.consent.GuardianConsentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** T-026 required 13: the waiting page reads the server again and lets the account go once the parent approved. */
@OptIn(ExperimentalCoroutinesApi::class)
class GuardianConsentViewModelTest {
    private val waitingReplies = ArrayDeque<Result<Boolean>>()
    private var reads = 0

    private val repository =
        object : SignUpRepository {
            override suspend fun registrationInfo() =
                Result.success(RegistrationInfo(null, emailCode = false, joinNotice = null))

            override suspend fun dropJoinCode() = Unit

            override suspend fun pendingEmailCode(): SignUpResult.EmailCodeSent? = null

            override suspend fun register(form: SignUpForm) = Result.success<SignUpResult>(SignUpResult.SignedIn)

            override suspend fun confirmEmailCode(code: String) = Result.success<SignUpResult>(SignUpResult.SignedIn)

            override suspend fun resendEmailCode() = Result.success(0)

            override fun observeConsent(): Flow<ConsentState?> = flowOf(null)

            override suspend fun refreshConsent(): Result<Boolean> {
                reads++
                return waitingReplies.removeFirst()
            }

            override suspend fun resendConsent() = Result.success("")

            override suspend fun changeConsentContact(
                channel: GuardianChannel,
                contact: String,
            ) = Result.success("")
        }

    private val auth =
        object : AuthRepository {
            override val isSignedIn: StateFlow<Boolean> = MutableStateFlow(true)

            override suspend fun signIn(
                email: String,
                password: String,
            ) = Result.success(Unit)

            override suspend fun register(
                fullName: String,
                email: String,
                password: String,
            ) = Result.success(Unit)

            override suspend fun requestPasswordReset(email: String) = Result.success("")

            override suspend fun signOut() = Unit
        }

    private fun TestScope.viewModel(): GuardianConsentViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return GuardianConsentViewModel(repository, auth, SessionNotices()).also { runCurrent() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun anApprovalFoundOnReturningOpensBeranda() =
        runTest {
            waitingReplies += Result.success(true)
            waitingReplies += Result.success(false)
            val vm = viewModel()

            vm.onEvent(GuardianConsentUiEvent.Refresh)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            vm.onEvent(GuardianConsentUiEvent.Refresh)
            runCurrent()

            assertEquals(2, reads)
            assertEquals(GuardianConsentEffect.Approved, vm.effects.first())
        }

    @Test
    fun aFailedReadKeepsTheAccountWaiting() =
        runTest {
            waitingReplies += Result.failure(java.io.IOException("offline"))
            val vm = viewModel()

            vm.onEvent(GuardianConsentUiEvent.Refresh)
            runCurrent()

            assertEquals(1, reads)
            assertFalse(vm.uiState.value.isRefreshing)
        }
}
