package com.gotest.app.feature.account

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.account.data.ApiProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfileUpdate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Profil calls of API module F, with the app's own Json. */
class ApiProfilePagesRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiProfilePagesRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiProfilePagesRepository(retrofit, json)
    }

    @After
    fun tearDown() = server.close()

    private fun reply(
        body: String,
        code: Int = 200,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build(),
    )

    @Test
    fun `the page reads sessions, stats and the link channel`() =
        runTest {
            reply(
                """
                {"googleLinked":false,"phone":"0812-3456-7890","phoneSignIn":true,"emailCode":true,"emailVerified":true,
                 "passwordLinkChannel":"whatsapp","soleManagerOf":["SMP 1 Bantargedung"],
                 "stats":{"questionsDone":120,"accuracy":74,"packagesDone":6,"questionsDoneChange":25,"accuracyChange":-3,
                   "packagesDoneChange":null},
                 "sessions":[{"id":"app-17","kind":"app","device":"Samsung Galaxy A15","ip":null,
                   "lastActiveAt":"2026-11-18T02:10:00+00:00","current":true}]}
                """.trimIndent(),
            )
            val page = repository.page().getOrThrow()

            assertTrue(page.linkByWhatsApp)
            assertEquals(listOf("SMP 1 Bantargedung"), page.soleManagerOf)
            assertEquals(-3, page.stats?.accuracyChange)
            assertTrue(page.sessions.single().isApp)
        }

    @Test
    fun `saving the profile sends snake case and reads where the email code went`() =
        runTest {
            reply("""{"message":null,"emailCodeSentTo":"r***@contoh.id"}""")
            val sent = repository.update(ProfileUpdate("Rina", "rina@contoh.id", "sma")).getOrThrow()

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals("sma", body.getValue("education_level").jsonPrimitive.content)
            assertEquals("r***@contoh.id", sent.emailCodeSentTo)
        }

    @Test
    fun `signing out other devices sends the password in a DELETE body`() =
        runTest {
            reply("""{"message":null,"signed_out":2}""")
            val reply = repository.signOutOtherDevices("rahasia123").getOrThrow()

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/profil/perangkat", request.target)
            assertTrue(request.body!!.utf8().contains("current_password"))
            assertEquals(2, reply.signedOut)
        }

    @Test
    fun `the photo goes as a multipart avatar jpeg`() =
        runTest {
            reply("""{"message":null}""")
            repository.uploadPhoto(byteArrayOf(1, 2, 3)).getOrThrow()

            val request = server.takeRequest()
            assertEquals("/api/v1/profil/foto", request.target)
            val body = request.body!!.utf8()
            assertTrue(body.contains("name=\"photo\"; filename=\"avatar.jpg\""))
        }

    @Test
    fun `a refused delete keeps the confirmation message`() =
        runTest {
            reply(
                """{"message":"Ketik ulang gotest.id untuk menghapus akun.","code":"validation",
                   "errors":{"confirmation":["Ketik ulang gotest.id untuk menghapus akun."]}}""",
                code = 422,
            )
            val error = repository.deleteAccount("gotest").exceptionOrNull() as ApiError

            assertEquals("Ketik ulang gotest.id untuk menghapus akun.", error.fieldError("confirmation"))
        }
}
