package com.gotest.app.feature.support

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.support.data.ApiSupportRepository
import com.gotest.app.feature.support.domain.NewTicket
import com.gotest.app.feature.support.domain.TicketStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Bantuan calls of API module F, with the app's own Json. */
class ApiSupportRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiSupportRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiSupportRepository(retrofit, json)
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
    fun `the page keeps the kinds of problem in the server's order`() =
        runTest {
            reply(
                """
                {"tickets":[{"id":31,"subject":"Hasil","category":"Kendala teknis","message":"Tidak terbuka.",
                  "status":"in_progress","statusLabel":"Ditangani","createdAt":null,
                  "replies":[{"id":90,"fromTeam":true,"author":"Tim Gotest","body":"Kami cek.","createdAt":null}]}],
                 "ticketCategories":{"teknis":"Kendala teknis","akun":"Akun dan masuk","lainnya":"Lainnya"}}
                """.trimIndent(),
            )
            val page = repository.page().getOrThrow()

            assertEquals(listOf("teknis", "akun", "lainnya"), page.ticketCategories.keys.toList())
            assertEquals(TicketStatus.InProgress, page.tickets.single().status)
            assertEquals(
                true,
                page.tickets
                    .single()
                    .replies
                    .single()
                    .fromTeam,
            )
        }

    @Test
    fun `a new ticket sends the category key and reads the server's message`() =
        runTest {
            reply("""{"message":"Tiket terkirim. Tim Gotest membalas di halaman ini.","id":32}""", code = 201)
            val sent = repository.sendTicket(NewTicket("Hasil", "teknis", "Tidak terbuka sejak kemarin.")).getOrThrow()

            val request = server.takeRequest()
            assertEquals("/api/v1/bantuan/tiket", request.target)
            val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals("teknis", body.getValue("category").jsonPrimitive.content)
            assertEquals("Tiket terkirim. Tim Gotest membalas di halaman ini.", sent.message)
        }

    @Test
    fun `a refused reply keeps the field message`() =
        runTest {
            reply(
                """{"message":"Tulis pesanmu.","code":"validation","errors":{"body":["Tulis pesanmu."]}}""",
                code = 422,
            )
            val error = repository.reply(31, "").exceptionOrNull() as ApiError

            assertEquals("/api/v1/bantuan/tiket/31/balas", server.takeRequest().target)
            assertEquals("Tulis pesanmu.", error.fieldError("body"))
        }

    @Test
    fun `a topic is read from its slug`() =
        runTest {
            reply(
                """
                {"topic":{"slug":"akun-profil","title":"Akun & Profil","description":"Kelola Akun, profile, dan keamanan",
                 "sections":[{"id":"akun","title":"Akun","body":"Pengaturan akun."}]}}
                """.trimIndent(),
            )
            val topic = repository.topic("akun-profil").getOrThrow()

            assertEquals("/api/v1/bantuan/akun-profil", server.takeRequest().target)
            assertEquals("akun", topic.sections.single().id)
        }
}
