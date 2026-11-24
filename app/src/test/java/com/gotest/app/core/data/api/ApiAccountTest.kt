package com.gotest.app.core.data.api

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.core.session.TokenCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.security.ProviderException

/** T-026 A2a step 1: Masuk with email, Keluar, `/akun`, and the token kept safely (required 2, 3, 5). */
class ApiAccountTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private val signals = SessionSignals()
    private val changes = AccountChanges()
    private val session = SessionStore(MemoryStore(), PlainCipher())
    private lateinit var auth: ApiAuthRepository
    private lateinit var users: ApiUserRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit: Retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        val acceptor = SignInAcceptor(session, signals, changes, SignUpDraftStore(MemoryStore()))
        auth = ApiAuthRepository(retrofit, json, session, acceptor)
        users = ApiUserRepository(retrofit, json, session, changes)
    }

    @After
    fun tearDown() = server.close()

    private fun reply(
        code: Int,
        body: String,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    @Test
    fun signingInSendsTheDeviceAndKeepsTheToken() =
        runTest {
            reply(200, signIn(consent = "not_needed"))

            auth.signIn("gilang@contoh.id", "rahasia123").getOrThrow()

            val request = server.takeRequest()
            assertEquals("/api/v1/auth/masuk", request.url.encodedPath)
            val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals("gilang@contoh.id", body.getValue("email").jsonPrimitive.content)
            assertTrue(
                body
                    .getValue("device_name")
                    .jsonPrimitive.content
                    .isNotBlank(),
            )
            assertEquals("7|token", session.token)
            assertFalse(signals.awaitingConsent.value)
            assertEquals(1, changes.changes.value)
        }

    @Test
    fun aLockedConsentOrAnUnprovenEmailOpensItsWaitingPageFromTheReply() =
        runTest {
            reply(200, signIn(consent = "pending"))
            auth.signIn("anak@contoh.id", "rahasia123").getOrThrow()
            assertTrue("T-026 required 3: no waiting for the first 403", signals.awaitingConsent.value)

            val acceptor = SignInAcceptor(session, signals, changes, SignUpDraftStore(MemoryStore()))
            acceptor.accept(SignInReply(token = "8|token", code = SignInReply.EMAIL_VERIFICATION_PENDING))
            assertTrue(signals.awaitingEmailVerification.value)
            assertFalse(acceptor.accept(SignInReply(code = SignInReply.EMAIL_CODE_SENT, registrationToken = "rt")))
        }

    @Test
    fun aBlockedAccountSaysWhy() =
        runTest {
            reply(401, """{"message":"Akunmu diblokir karena melanggar aturan.","code":"account_blocked"}""")

            val error = auth.signIn("gilang@contoh.id", "rahasia123").exceptionOrNull() as ApiError

            assertEquals("account_blocked", error.code)
            assertEquals("Akunmu diblokir karena melanggar aturan.", error.message)
            assertNull(session.token)
        }

    @Test
    fun signingOutForgetsTheTokenEvenOffline() =
        runTest {
            session.save("7|token")
            server.close()

            auth.signOut()

            assertFalse(session.isSignedIn.value)
        }

    @Test
    fun theAccountReadsAkunAndAgainAfterAWrite() =
        runTest {
            session.save("7|token")
            reply(200, ACCOUNT)

            val profile = users.observeProfile().first()

            assertEquals("/api/v1/akun", server.takeRequest().url.encodedPath)
            assertEquals("Rina Ayu", profile.fullName)
            assertEquals(PlanTier.Premium, profile.subscription.tier)
            assertEquals("Premium", profile.subscription.planName)
            assertEquals(EducationLevel.Smp, profile.educationLevel)
            assertEquals("Keluarga Ayu", profile.managedBy)
            assertTrue(profile.isChild)
        }

    @Test
    fun aKeystoreThatFailsAsksToSignInAgainInsteadOfStopping() {
        val store = MemoryStore().apply { putString("token", "sealed") }
        val broken =
            object : TokenCipher {
                override fun encrypt(plain: String): String = throw ProviderException("keystore")

                override fun decrypt(encrypted: String): String? = throw ProviderException("keystore")
            }

        val reopened = SessionStore(store, broken)
        assertFalse(reopened.isSignedIn.value)
        assertNull(store.getString("token"))

        reopened.save("9|token")
        assertTrue("Kept for this run only (T-026 required 5)", reopened.isSignedIn.value)
        assertNull(store.getString("token"))
    }

    private fun signIn(consent: String) =
        """{"token":"7|token","token_type":"Bearer","expires_at":"2027-02-21T03:00:00+07:00",""" +
            """"user":{"id":7,"name":"Gilang","avatar_url":null,"email":"gilang@contoh.id","phone":null,""" +
            """"education_level":"sma"},"is_child":false,"consent":"$consent"}"""

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

    private class PlainCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain

        override fun decrypt(encrypted: String): String = encrypted
    }

    private companion object {
        const val ACCOUNT =
            """{"user":{"id":9,"name":"Rina Ayu","avatar_url":null,"email":null,"phone":null,""" +
                """"education_level":"smp"},""" +
                """"is_child":true,"consent":"approved",""" +
                """"access":{"status":"active","label":"Akses aktif","plan":"Premium","startsAt":null,""" +
                """"endsAt":"2026-12-31T16:59:59+00:00","remainingDays":30,"source":"mitra",""" +
                """"partner":"Keluarga Ayu"},""" +
                """"free_plan":null,"trial_period":null,""" +
                """"organizations":[{"code":"KAYU01","name":"Keluarga Ayu","type":"keluarga","role":"siswa"}],""" +
                """"token":{"name":"Pixel 8","expires_at":null}}"""
    }
}
