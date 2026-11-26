package com.gotest.app.core.network

import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.TokenCipher
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

class ApiNetworkTest {
    private val server = MockWebServer()
    private val session = SessionStore(MemoryStore(), PlainCipher())
    private val signals = SessionSignals()
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client =
            OkHttpClient
                .Builder()
                .addInterceptor(ApiHeadersInterceptor(session, appVersionCode = 3, apiBaseUrl = server.url("/")))
                .addInterceptor(SessionGuardInterceptor(session, signals))
                .build()
    }

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
    fun `a 401 on a signed-in request signs out and leaves Masuk to open`() {
        session.save("12|kedaluwarsa")
        server.enqueue(MockResponse.Builder().code(401).build())
        call()

        assertFalse(session.isSignedIn.value)
        assertTrue(signals.expired.value)
    }

    @Test
    fun `a 401 while signed out, such as a wrong password, is left to the screen`() {
        server.enqueue(MockResponse.Builder().code(401).build())
        call()

        assertFalse(signals.expired.value)
    }

    @Test
    fun `a 403 guardian_consent opens the waiting page and keeps the session`() {
        session.save("12|menunggu")
        server.enqueue(
            MockResponse
                .Builder()
                .code(403)
                .body("""{"message":"Tidak boleh.","code":"forbidden"}""")
                .build(),
        )
        server.enqueue(
            MockResponse
                .Builder()
                .code(403)
                .body(CONSENT_BODY)
                .build(),
        )
        call()
        assertFalse(signals.awaitingConsent.value)

        val body = call()
        assertTrue(session.isSignedIn.value)
        assertTrue(signals.awaitingConsent.value)
        assertEquals(CONSENT_BODY, body)
    }

    @Test
    fun `a 426 asks to update the app`() {
        server.enqueue(MockResponse.Builder().code(426).build())
        call()

        assertTrue(signals.updateRequired.value)
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

    @Test
    fun `the token goes only to the Gotest API, not to another address`() {
        session.save("12|rahasia")
        val other = MockWebServer().apply { start() }
        other.enqueue(MockResponse.Builder().code(200).build())
        client.newCall(Request.Builder().url(other.url("/gambar.png")).build()).execute().close()

        assertNull(other.takeRequest().headers["Authorization"])
        other.close()
    }

    @Test
    fun `a late 401 for a token already replaced leaves the new session`() {
        server.enqueue(MockResponse.Builder().code(401).build())
        // The request went out with the old token; the account signed in again before its answer arrived.
        val oldRequest =
            Request
                .Builder()
                .url(server.url("/api/v1/akun"))
                .header("Authorization", "Bearer 12|lama")
                .build()
        session.save("13|baru")
        OkHttpClient
            .Builder()
            .addInterceptor(SessionGuardInterceptor(session, signals))
            .build()
            .newCall(oldRequest)
            .execute()
            .close()

        assertEquals("13|baru", session.token)
        assertFalse(signals.expired.value)
    }

    @Test
    fun `a 403 email_verification_pending opens its waiting page and keeps the session`() {
        session.save("12|menunggu-email")
        server.enqueue(
            MockResponse
                .Builder()
                .code(403)
                .body("""{"message":"Akun ini menunggu verifikasi email.","code":"email_verification_pending"}""")
                .build(),
        )
        call()

        assertTrue(session.isSignedIn.value)
        assertTrue(signals.awaitingEmailVerification.value)
        assertFalse(signals.awaitingConsent.value)
    }

    @Test
    fun `the whole error body and Retry-After stay with the error`() {
        val json = Json { ignoreUnknownKeys = true }
        val error =
            ApiError.from(
                status = 422,
                body =
                    """{"message":"Nomor ini belum terdaftar.","code":"phone_not_registered",""" +
                        """"registration_token":"rt-1","phone":"081234567890","name":null,"min_version":5}""",
                json = json,
                fallbackMessage = "Terjadi kesalahan.",
                retryAfter = " 42 ",
            )

        assertEquals("rt-1", error.detail("registration_token"))
        assertEquals("081234567890", error.detail("phone"))
        assertNull(error.detail("name"))
        assertEquals(5, error.minVersion)
        assertEquals(42L, error.retryAfterSeconds)
        assertNull(ApiError.from(429, "{}", json, "x", retryAfter = "Wed, 21 Oct 2026 07:28:00 GMT").retryAfterSeconds)
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
