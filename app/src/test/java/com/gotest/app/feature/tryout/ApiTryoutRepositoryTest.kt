package com.gotest.app.feature.tryout

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.tryout.data.ApiTryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutAttempt
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * The Try Out routes of API module D with the app's Json: answers and ragu-ragu by item id, `section_id`, the session
 * routes, and the error codes the screens act on (Ardy's review of A2c, item 7).
 */
class ApiTryoutRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiTryoutRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiTryoutRepository(retrofit, json)
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

    private fun take(): Pair<String, String> {
        val request = server.takeRequest()
        return "${request.method} ${request.url.encodedPath}" to request.body?.utf8().orEmpty()
    }

    @Test
    fun answersAndFlagsGoByItemId() =
        runTest {
            reply(200, """{"answer":{"attempt_question_item_id":11,"answered_at":"2026-11-20T03:00:00Z"}}""")
            reply(200, """{"item":{"id":11,"is_flagged":true}}""")

            repository.answer(1401, 11, AnswerData(selectedOptions = listOf(1, 2))).getOrThrow()
            repository.flag(1401, 11, true).getOrThrow()

            assertEquals(
                "PATCH /api/v1/tryout/attempts/1401/answers/11" to """{"answer_data":{"selected_options":[1,2]}}""",
                take(),
            )
            assertEquals("PATCH /api/v1/tryout/attempts/1401/questions/11/flag" to """{"flagged":true}""", take())
        }

    @Test
    fun theRunnerReadsTheRunningSection() =
        runTest {
            reply(200, ATTEMPT)

            val attempt = repository.begin(1401).getOrThrow()

            assertEquals("POST /api/v1/tryout/attempts/1401/begin", take().first)
            val section = attempt.activeSection!!
            assertEquals("2026-11-20T03:05:00Z", section.deadlineAt)
            assertEquals(listOf(11L, 12L), section.items.map { it.id })
            assertEquals(true, section.items[1].isFlagged)
            assertEquals("multiple_select", section.items[1].question.questionType)
            assertEquals(2, attempt.sections.size)
            assertEquals(true, attempt.inSession)
        }

    @Test
    fun finishSectionNamesTheSectionWhenKnown() =
        runTest {
            reply(200, ATTEMPT)
            reply(200, ATTEMPT)

            repository.finishSection(1401, 1).getOrThrow()
            repository.finishSection(1401, null).getOrThrow()

            assertEquals("POST /api/v1/tryout/attempts/1401/finish-section" to """{"section_id":1}""", take())
            assertEquals("POST /api/v1/tryout/attempts/1401/finish-section" to "{}", take())
        }

    @Test
    fun endingBeforeTheFirstSectionReadsCancelled() =
        runTest {
            reply(200, """{"attempt":{"id":1401,"status":"cancelled","in_session":false,"sections":[]}}""")

            val attempt = repository.submit(1401).getOrThrow()

            assertEquals("POST /api/v1/tryout/attempts/1401/submit", take().first)
            assertEquals(TryoutAttempt.STATUS_CANCELLED, attempt.status)
            assertNull(attempt.activeSection)
        }

    @Test
    fun sessionRoutesReadTheServerSentenceAndTheAttempt() =
        runTest {
            reply(200, """{"message":"Kamu terdaftar di sesi ini."}""")
            reply(200, """{"message":"Pendaftaran sesi dibatalkan."}""")
            reply(201, ATTEMPT)

            assertEquals("Kamu terdaftar di sesi ini.", repository.register(411).getOrThrow())
            assertEquals("Pendaftaran sesi dibatalkan.", repository.unregister(411).getOrThrow())
            assertEquals(1401L, repository.startSession(411).getOrThrow())

            assertEquals("POST /api/v1/tryout/sesi/411/daftar", take().first)
            assertEquals("DELETE /api/v1/tryout/sesi/411/daftar", take().first)
            assertEquals("POST /api/v1/tryout/sesi/411/attempts", take().first)
        }

    @Test
    fun theErrorCodesKeepTheServerSentence() =
        runTest {
            reply(410, """{"message":"$CANCELLED","code":"cancelled"}""")
            reply(403, """{"message":"$NOT_MEMBER","code":"not_member"}""")
            reply(403, """{"message":"$QUOTA","code":"quota_used"}""")
            reply(429, """{"message":"$BUSY","code":"busy"}""")
            reply(
                422,
                """{"message":"Data tidak valid.","code":"validation",""" +
                    """"errors":{"answer":["Section soal ini sudah dikunci."]}}""",
            )

            val cancelled = repository.attempt(1401).exceptionOrNull() as ApiError
            assertEquals(
                Triple(410, "cancelled", CANCELLED),
                Triple(cancelled.status, cancelled.code, cancelled.message),
            )

            val notMember = repository.finishSection(1401, 1).exceptionOrNull() as ApiError
            assertEquals(
                Triple(403, "not_member", NOT_MEMBER),
                Triple(notMember.status, notMember.code, notMember.message),
            )

            val quota = repository.register(411).exceptionOrNull() as ApiError
            assertEquals("quota_used" to QUOTA, quota.code to quota.message)

            val busy = repository.start(301).exceptionOrNull() as ApiError
            assertEquals(Triple(429, "busy", BUSY), Triple(busy.status, busy.code, busy.message))

            val locked = repository.answer(1401, 11, AnswerData(selectedOption = 0)).exceptionOrNull() as ApiError
            assertEquals("Section soal ini sudah dikunci.", locked.fieldError("answer"))
        }

    private companion object {
        const val CANCELLED = "Pengerjaan ini dibatalkan oleh tim Gotest. Kamu bisa memulai paket ini lagi."
        const val NOT_MEMBER = "Kamu bukan lagi siswa Mitra ini, sehingga Try Out tidak bisa dilanjutkan."
        const val QUOTA = "Kuota Try Out gratis bulan ini sudah habis. Coba lagi bulan depan atau gunakan Premium."
        const val BUSY = "Permintaan sebelumnya masih diproses. Coba lagi sebentar."
        val ATTEMPT =
            """
            {"attempt":{"id":1401,"status":"started","in_session":true,"server_now":"2026-11-20T03:00:00Z",
            "active_section":{"id":1,"title":"Penalaran Umum","order_number":1,"status":"started",
            "duration_seconds":300,"deadline_at":"2026-11-20T03:05:00Z","question_items":[
            {"id":11,"question_id":101,"display_order":1,"is_flagged":false,"selected_options":[],
            "statement_answers":[],"question":{"question_type":"multiple_choice","content":{}}},
            {"id":12,"question_id":102,"display_order":2,"is_flagged":true,"selected_options":[],
            "statement_answers":[],"question":{"question_type":"multiple_select","content":{}}}]},
            "next_section":null,
            "sections":[{"id":1,"title":"Penalaran Umum","order_number":1,"status":"started"},
            {"id":2,"title":"Pengetahuan Kuantitatif","order_number":2,"status":"pending"}]}}
            """.trimIndent()
    }
}
