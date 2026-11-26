package com.gotest.app.feature.auth

import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.feature.auth.domain.CredentialValidator
import com.gotest.app.feature.auth.domain.SignInOptions
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.ui.forgotpassword.ForgotPasswordUiEvent
import com.gotest.app.feature.auth.ui.forgotpassword.ForgotPasswordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lupa Sandi by email, or by WhatsApp while `password_reset_whatsapp` is on (T-026 A2a, DEC-125). */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    private val sent = mutableListOf<String>()

    private val auth =
        object : AuthRepository {
            override val isSignedIn: StateFlow<Boolean> = MutableStateFlow(false)

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

            override suspend fun requestPasswordReset(email: String): Result<String> {
                sent += email
                return Result.success(WHATSAPP_SENT)
            }

            override suspend fun signOut() = Unit
        }

    private fun TestScope.viewModel(byWhatsApp: Boolean): ForgotPasswordViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val options =
            object : SignInOptionsRepository {
                override suspend fun options() = SignInOptions(passwordResetWhatsApp = byWhatsApp)
            }
        return ForgotPasswordViewModel(auth, CredentialValidator(), options)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun byWhatsAppANumberGoesToTheServerAndItsSentenceShows() =
        runTest {
            val vm = viewModel(byWhatsApp = true)
            runCurrent()
            assertTrue(vm.uiState.value.byWhatsApp)

            vm.onEvent(ForgotPasswordUiEvent.EmailChanged(" 0812 3456 7890 "))
            vm.onEvent(ForgotPasswordUiEvent.Submit)
            runCurrent()

            assertEquals(listOf("0812 3456 7890"), sent)
            assertEquals(WHATSAPP_SENT, vm.uiState.value.sentMessage)
        }

    @Test
    fun byEmailANumberIsStoppedBeforeTheServer() =
        runTest {
            val vm = viewModel(byWhatsApp = false)
            runCurrent()
            assertFalse(vm.uiState.value.byWhatsApp)

            vm.onEvent(ForgotPasswordUiEvent.EmailChanged("0812 3456 7890"))
            vm.onEvent(ForgotPasswordUiEvent.Submit)
            runCurrent()

            assertTrue(sent.isEmpty())
            assertNotNull(vm.uiState.value.emailError)
        }

    private companion object {
        const val WHATSAPP_SENT =
            "Bila akun itu terdaftar dengan nomor WhatsApp, tautan untuk membuat kata sandi baru dikirim ke " +
                "WhatsApp-nya dalam beberapa menit."
    }
}
