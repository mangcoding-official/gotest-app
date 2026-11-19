package com.gotest.app.feature.account

import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.account.domain.PasswordChange
import com.gotest.app.feature.account.domain.ProfilePage
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfileUpdate
import com.gotest.app.feature.account.domain.ProfileWriteReply
import com.gotest.app.feature.account.ui.deleteaccount.DeleteAccountEffect
import com.gotest.app.feature.account.ui.deleteaccount.DeleteAccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * A2e review 1: an account waiting for, refused, or withdrawn parental approval gets 403 on GET /profil
 * but may still DELETE /profil, so the form shows without /profil. Review ulang: without a connection the
 * page asks to try again instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {
    private class NoProfilePages : ProfilePagesRepository {
        var deletedWith: String? = null
        var pageReply: Result<ProfilePage> = Result.failure(ApiError(ApiError.FORBIDDEN, "", ApiError.GUARDIAN_CONSENT))
        private val refused: Result<Nothing> get() = Result.failure(IOException())

        override suspend fun page(): Result<ProfilePage> = pageReply

        override suspend fun update(update: ProfileUpdate): Result<ProfileWriteReply> = refused

        override suspend fun resendEmailCode(email: String): Result<ProfileWriteReply> = refused

        override suspend fun saveEmail(
            email: String,
            code: String,
        ): Result<ProfileWriteReply> = refused

        override suspend fun uploadPhoto(jpeg: ByteArray): Result<ProfileWriteReply> = refused

        override suspend fun deletePhoto(): Result<ProfileWriteReply> = refused

        override suspend fun changePassword(change: PasswordChange): Result<ProfileWriteReply> = refused

        override suspend fun sendPasswordLink(): Result<ProfileWriteReply> = refused

        override suspend fun sendPhoneCode(phone: String): Result<ProfileWriteReply> = refused

        override suspend fun savePhone(
            phone: String,
            code: String,
        ): Result<ProfileWriteReply> = refused

        override suspend fun removePhone(): Result<ProfileWriteReply> = refused

        override suspend fun signOutOtherDevices(currentPassword: String): Result<ProfileWriteReply> = refused

        override suspend fun deleteAccount(confirmation: String): Result<ProfileWriteReply> {
            deletedWith = confirmation
            return Result.success(ProfileWriteReply())
        }
    }

    private class MemoryStore : KeyValueStore {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private object PlainCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain

        override fun decrypt(encrypted: String): String = encrypted
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theFormShowsAndDeletesWithoutProfil() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val pages = NoProfilePages()
            val session = SessionStore(MemoryStore(), PlainCipher)
            session.save("token")
            val vm = DeleteAccountViewModel(pages, session)
            val effects = mutableListOf<DeleteAccountEffect>()
            backgroundScope.launch { vm.effects.collect { effects += it } }
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.loadFailed)
            assertEquals(emptyList<String>(), vm.uiState.value.soleManagerOf)

            vm.onConfirmationChange("gotest.id")
            vm.delete()
            runCurrent()
            assertEquals("gotest.id", pages.deletedWith)
            assertEquals(listOf<DeleteAccountEffect>(DeleteAccountEffect.AccountDeleted), effects)
            assertNull(session.token)
            vm.viewModelScope.cancel()
        }

    @Test
    fun withoutConnectionThePageAsksToTryAgain() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val pages = NoProfilePages().apply { pageReply = Result.failure(IOException()) }
            val vm = DeleteAccountViewModel(pages, SessionStore(MemoryStore(), PlainCipher))
            runCurrent()
            assertTrue(vm.uiState.value.loadFailed)

            pages.pageReply = Result.success(ProfilePage(soleManagerOf = listOf("Bimbel Cerdas")))
            vm.load()
            runCurrent()
            assertFalse(vm.uiState.value.loadFailed)
            assertEquals(listOf("Bimbel Cerdas"), vm.uiState.value.soleManagerOf)
            vm.viewModelScope.cancel()
        }
}
