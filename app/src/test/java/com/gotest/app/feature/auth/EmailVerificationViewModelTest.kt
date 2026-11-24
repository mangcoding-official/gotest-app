package com.gotest.app.feature.auth

import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.SessionNotices
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationState
import com.gotest.app.feature.auth.domain.EmailVerified
import com.gotest.app.feature.auth.ui.emailverification.CodeProblem
import com.gotest.app.feature.auth.ui.emailverification.EmailVerificationEffect
import com.gotest.app.feature.auth.ui.emailverification.EmailVerificationViewModel
import com.gotest.app.feature.ruang.domain.JoinedMitra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** "Menunggu verifikasi email" (DEC-154, T-042): resend with its wait, the code, and where the account goes next. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {
    private val signals = SessionSignals().apply { markAwaitingEmailVerification() }
    private val notices = SessionNotices()
    private var stateReply: Result<EmailVerificationState> = Result.success(WAITING)
    private var resendReply: Result<EmailVerificationState> = Result.success(WAITING.copy(resendIn = 60))
    private var verifyReply: Result<EmailVerified> = Result.success(EmailVerified())
    private val verified = mutableListOf<String>()
    private var signedOut = false

    private val repository =
        object : EmailVerificationRepository {
            override suspend fun state() = stateReply

            override suspend fun resend() = resendReply

            override suspend fun verify(code: String): Result<EmailVerified> {
                verified += code
                return verifyReply
            }
        }

    private val auth =
        object : AuthRepository {
            override val isSignedIn: StateFlow<Boolean> = MutableStateFlow(true)

            override suspend fun signIn(
                email: String,
                password: String,
            ) = Result.success(Unit)

            override suspend fun signInWithGoogle() = Result.success(Unit)

            override suspend fun register(
                fullName: String,
                email: String,
                password: String,
            ) = Result.success(Unit)

            override suspend fun requestPasswordReset(email: String) = Result.success(Unit)

            override suspend fun signOut() {
                signedOut = true
            }
        }

    private fun TestScope.viewModel(): EmailVerificationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return EmailVerificationViewModel(repository, auth, signals, notices).also { runCurrent() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theRightCodeOpensTheAccountAndItsMitra() =
        runTest {
            verifyReply = Result.success(EmailVerified(joined = JoinedMitra("SMP1BTG", "siswa")))
            val vm = viewModel()
            assertEquals(WAITING, vm.uiState.value.page)

            vm.onCode("12a3 456")
            assertEquals("123456", vm.uiState.value.code)
            vm.verify()
            runCurrent()

            assertEquals(listOf("123456"), verified)
            assertEquals(EmailVerificationEffect.Verified("SMP1BTG"), vm.effects.first())
            assertFalse(signals.awaitingEmailVerification.value)
        }

    @Test
    fun whyTheMitraLinkWasDroppedIsKeptForTheNextScreen() =
        runTest {
            val reason = "Tautan undangan sudah tidak berlaku, jadi akunmu belum bergabung ke Mitra."
            verifyReply = Result.success(EmailVerified(message = reason))
            val vm = viewModel()
            vm.onCode("123456")
            vm.verify()
            runCurrent()

            // Beranda (or the parent's approval page) shows "Emailmu sudah terverifikasi. {alasan}" once.
            assertEquals(reason, notices.takeEmailVerified())
            assertEquals(null, notices.takeEmailVerified())
        }

    @Test
    fun aParentsApprovalAskedNowOpensItsWaitingPageWithTheReason() =
        runTest {
            verifyReply =
                Result.success(
                    EmailVerified(message = "Kode kelas sudah ditutup.", consent = EmailVerified.CONSENT_PENDING),
                )
            val vm = viewModel()
            vm.onCode("123456")
            vm.verify()
            runCurrent()

            assertFalse(signals.awaitingEmailVerification.value)
            assertTrue(signals.awaitingConsent.value)
            assertEquals("Kode kelas sudah ditutup.", notices.takeEmailVerified())
        }

    @Test
    fun aWrongCodeSaysTheServerSentenceAndEmptiesTheField() =
        runTest {
            val wrong = "Kode salah. Sisa 4 kali percobaan."
            verifyReply =
                Result.failure(ApiError(422, "Data tidak valid.", "validation", mapOf("code" to listOf(wrong))))
            val vm = viewModel()
            vm.onCode("000000")
            vm.verify()
            runCurrent()

            assertEquals(CodeProblem.Server(wrong), vm.uiState.value.problem)
            assertEquals("", vm.uiState.value.code)
            assertTrue(signals.awaitingEmailVerification.value)

            verifyReply = Result.failure(IOException())
            vm.onCode("111111")
            vm.verify()
            runCurrent()
            assertEquals(CodeProblem.Offline, vm.uiState.value.problem)
        }

    @Test
    fun resendingWaitsItsSecondsBeforeItCanGoAgain() =
        runTest {
            val vm = viewModel()
            vm.resend()
            runCurrent()
            assertEquals(60, vm.uiState.value.resendIn)

            vm.resend()
            advanceTimeBy(60_100)
            runCurrent()
            assertEquals(0, vm.uiState.value.resendIn)
        }

    @Test
    fun anEmailVerifiedMeanwhileOpensTheAccount() =
        runTest {
            stateReply = Result.success(WAITING.copy(waiting = false))
            val vm = viewModel()

            assertEquals(EmailVerificationEffect.Verified(null), vm.effects.first())
            assertFalse(signals.awaitingEmailVerification.value)
        }

    @Test
    fun signingOutEndsTheWait() =
        runTest {
            val vm = viewModel()
            vm.signOut()
            runCurrent()

            assertTrue(signedOut)
            assertFalse(signals.awaitingEmailVerification.value)
            assertEquals(EmailVerificationEffect.SignedOut, vm.effects.first())
        }

    private companion object {
        val WAITING = EmailVerificationState(waiting = true, email = "gilangpermana@mail.com", sentTo = "g***@mail.com")
    }
}
