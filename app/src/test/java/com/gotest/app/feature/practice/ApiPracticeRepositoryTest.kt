package com.gotest.app.feature.practice

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.practice.data.ApiPracticeRepository
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.answerPayload
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * The Latihan routes of API module C with the app's Json: the request bodies the server expects
 * (`timer_enabled`, `answer_data` in three shapes) and the error codes the screens act on (Ardy's review of A2c,
 * item 7).
 */
class ApiPracticeRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiPracticeRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiPracticeRepository(retrofit, json)
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

    private fun takeBody(): Pair<String, String> {
        val request = server.takeRequest()
        return "${request.method} ${request.url.encodedPath}" to request.body!!.utf8()
    }

    @Test
    fun startSendsTheTimerChoiceOnlyWhenThereIsOne() =
        runTest {
            reply(201, ATTEMPT)
            reply(201, ATTEMPT)

            val attempt = repository.start(102, timerEnabled = false).getOrThrow()
            repository.start(103, timerEnabled = null).getOrThrow()

            assertEquals("POST /api/v1/packages/102/attempts" to """{"timer_enabled":false}""", takeBody())
            assertEquals("POST /api/v1/packages/103/attempts" to "{}", takeBody())
            assertEquals(1201L, attempt.id)
            assertEquals("linear", attempt.navigation)
            assertEquals(2, attempt.items.size)
        }

    @Test
    fun anAnswerGoesByQuestionIdInTheShapeOfItsType() =
        runTest {
            repeat(3) { reply(200, """{"answer":{"attemptQuestionItemId":1,"answeredAt":"2026-11-20T03:00:00Z"}}""") }

            repository.answer(1201, 101, answerPayload("multiple_choice", AnswerValue.Single(2))).getOrThrow()
            repository.answer(1201, 102, answerPayload("multiple_select", AnswerValue.Multiple(listOf(0, 3))))
            repository.answer(1201, 103, answerPayload("statement_table", AnswerValue.Statement(listOf(1, null))))

            assertEquals(
                "PATCH /api/v1/attempts/1201/answers/101" to """{"answer_data":{"selected_option":2}}""",
                takeBody(),
            )
            assertEquals(
                "PATCH /api/v1/attempts/1201/answers/102" to """{"answer_data":{"selected_options":[0,3]}}""",
                takeBody(),
            )
            assertEquals(
                "PATCH /api/v1/attempts/1201/answers/103" to """{"answer_data":{"statement_answers":[1,null]}}""",
                takeBody(),
            )
        }

    @Test
    fun activateReadsTheQuestionOfTheItem() =
        runTest {
            reply(
                200,
                """{"question":{"attemptQuestionItemId":2,"questionId":102,"displayOrder":2,""" +
                    """"questionType":"multiple_choice","content":{}},"server_now":"2026-11-20T03:00:00Z"}""",
            )

            val question = repository.activate(1201, 2).getOrThrow()

            val request = server.takeRequest()
            assertEquals(
                "POST /api/v1/attempts/1201/questions/2/activate",
                "${request.method} ${request.url.encodedPath}",
            )
            assertEquals(2L, question.itemId)
            assertEquals(102L, question.questionId)
        }

    @Test
    fun theErrorCodesKeepTheServerSentence() =
        runTest {
            reply(410, """{"message":"$CANCELLED","code":"cancelled"}""")
            reply(403, """{"message":"$NOT_MEMBER","code":"not_member"}""")
            reply(403, """{"message":"Paket ini khusus Premium.","code":"premium_only"}""")
            reply(429, """{"message":"$BUSY","code":"busy"}""")
            reply(
                422,
                """{"message":"Data tidak valid.","code":"validation",""" +
                    """"errors":{"package":["Paket ini tidak dapat dikerjakan ulang."]}}""",
            )

            val cancelled = repository.attempt(1201).exceptionOrNull() as ApiError
            assertEquals(
                Triple(410, "cancelled", CANCELLED),
                Triple(cancelled.status, cancelled.code, cancelled.message),
            )

            val notMember = repository.submit(1201).exceptionOrNull() as ApiError
            assertEquals(
                Triple(403, "not_member", NOT_MEMBER),
                Triple(notMember.status, notMember.code, notMember.message),
            )

            val premium = repository.start(104, null).exceptionOrNull() as ApiError
            assertEquals("premium_only" to "Paket ini khusus Premium.", premium.code to premium.message)

            val busy = repository.startInRuang("SMP1BTG", 601).exceptionOrNull() as ApiError
            assertEquals(Triple(429, "busy", BUSY), Triple(busy.status, busy.code, busy.message))

            val refused = repository.start(105, null).exceptionOrNull() as ApiError
            assertEquals("Paket ini tidak dapat dikerjakan ulang.", refused.fieldError("package"))
        }

    @Test
    fun noConnectionIsNotAnApiError() =
        runTest {
            server.close()

            val error = repository.attempt(1201).exceptionOrNull()

            assertTrue(error != null && error !is ApiError)
        }

    private companion object {
        const val CANCELLED = "Pengerjaan ini dibatalkan oleh tim Gotest. Kamu bisa memulai paket ini lagi."
        const val NOT_MEMBER = "Kamu bukan lagi siswa Mitra ini, sehingga Latihan tidak bisa dilanjutkan."
        const val BUSY = "Permintaan sebelumnya masih diproses. Coba lagi sebentar."
        val ATTEMPT =
            """
            {"attempt":{"id":1201,"status":"started","server_now":"2026-11-20T03:00:00.000+00:00",
            "effective_timer_mode":"per_question","timer_enabled":true,"navigation":"linear",
            "active_question_item_id":1,"question_items":[
            {"id":1,"question_id":101,"display_order":1,"question_type":"multiple_choice","selected_options":[],
            "statement_answers":[],"timed_out":false,"locked":false},
            {"id":2,"question_id":102,"display_order":2,"question_type":"multiple_select","selected_options":[],
            "statement_answers":[],"timed_out":false,"locked":false}]}}
            """.trimIndent()
    }
}
