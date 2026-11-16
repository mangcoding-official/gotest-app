package com.gotest.app.core.network

import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.TokenCipher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiNetworkTest {
    private val server = MockWebServer()
    private val session = SessionStore(MemoryStore(), PlainCipher())
    private val events = SessionEvents()
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor(ApiHeadersInterceptor(session, appVersionCode = 3))
            .addInterceptor(SessionGuardInterceptor(session, events))
            .build()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    @Test
    fun `every request says it wants JSON and which app version asks`() {
        server.enqueue(MockResponse.Builder().code(200).build())
        call()

        val request = server.takeRequest()
        assertEquals("application/json", request.headers["Accept"])
        assertEquals("3", request.headers["X-App-Version"])
        assertNull(request.headers["Authorization"])
    }

    @Test
    fun `a signed-in request carries the token`() {
        session.save("12|rahasia")
        server.enqueue(MockResponse.Builder().code(200).build())
        call()

        assertEquals("Bearer 12|rahasia", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `a 401 on a signed-in request signs out`() =
        runTest {
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.flow.toList(seen) }
            session.save("12|kedaluwarsa")
            server.enqueue(MockResponse.Builder().code(401).build())
            call()

            assertFalse(session.isSignedIn.value)
            assertEquals(listOf<SessionEvent>(SessionEvent.SignedOut), seen)
        }

    @Test
    fun `a 401 while signed out, such as a wrong password, is left to the screen`() =
        runTest {
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.flow.toList(seen) }
            server.enqueue(MockResponse.Builder().code(401).build())
            call()

            assertTrue(seen.isEmpty())
        }

    @Test
    fun `a 403 guardian_consent opens the waiting page and keeps the session`() =
        runTest {
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.flow.toList(seen) }
            session.save("12|menunggu")
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(403)
                    .body(CONSENT_BODY)
                    .build(),
            )
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(403)
                    .body("""{"message":"Tidak boleh.","code":"forbidden"}""")
                    .build(),
            )
            val first = call()
            call()

            assertTrue(session.isSignedIn.value)
            assertEquals(listOf<SessionEvent>(SessionEvent.AwaitingConsent), seen)
            assertEquals(CONSENT_BODY, first)
        }

    @Test
    fun `a 426 asks to update the app`() =
        runTest {
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.flow.toList(seen) }
            server.enqueue(MockResponse.Builder().code(426).build())
            call()

            assertEquals(listOf<SessionEvent>(SessionEvent.UpdateRequired), seen)
        }

    @Test
    fun `the shared error body is read, with field errors`() {
        val json = Json { ignoreUnknownKeys = true }
        val error =
            ApiError.from(
                status = 422,
                body =
                    """{"message":"$WRONG_LOGIN","code":"validation","errors":{"email":["$WRONG_LOGIN"]}}""",
                json = json,
                fallbackMessage = "Terjadi kesalahan.",
            )
        assertEquals("Email atau kata sandi tidak sesuai.", error.message)
        assertEquals("validation", error.code)
        assertEquals("Email atau kata sandi tidak sesuai.", error.fieldError("email"))

        val notJson = ApiError.from(status = 500, body = "<html>", json = json, fallbackMessage = "Terjadi kesalahan.")
        assertEquals("Terjadi kesalahan.", notJson.message)
        assertNull(notJson.code)
    }

    private companion object {
        const val WRONG_LOGIN = "Email atau kata sandi tidak sesuai."
        const val CONSENT_BODY = """{"message":"Akunmu menunggu persetujuan orang tua.","code":"guardian_consent"}"""
    }

    /** Returns the body, which the session guard must leave for the screen. */
    private fun call(): String =
        client.newCall(Request.Builder().url(server.url("/api/v1/akun")).build()).execute().use {
            it.body.string()
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

    private class PlainCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain

        override fun decrypt(encrypted: String): String = encrypted
    }
}
