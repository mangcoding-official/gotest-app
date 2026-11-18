package com.gotest.app.feature.ruang

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.ruang.data.ApiExamRepository
import com.gotest.app.feature.ruang.domain.ExamAnswerUpload
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The Retrofit calls against API-CONTRACTS "API Android modul G", with the app's own Json. */
class ApiExamRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiExamRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiExamRepository(retrofit, json)
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
    fun startReadsTheNewAttemptAndItsDeviceToken() =
        runTest {
            reply(201, """{"attempt_id":9,"device_token":"Hk3","status":"in_progress"}""")
            val started = repository.start("SMP1BTG", 41).getOrThrow()
            assertEquals(9L, started.attemptId)
            assertEquals("Hk3", started.deviceToken)
            assertEquals("/api/v1/ruang/SMP1BTG/ujian/41/mulai", server.takeRequest().target)
        }

    @Test
    fun aClearedAnswerIsSentAsNullWithTheDeviceToken() =
        runTest {
            reply(
                200,
                """{"saved":[55],"stale":[],"rejected":[],"last_client_seq":3,""" +
                    """"server_now":"2026-12-01T01:31:00+00:00",""" +
                    """"deadline_at":"2026-12-01T02:30:00+00:00","status":"in_progress"}""",
            )
            val saved =
                repository
                    .saveAnswers(9, "Hk3", listOf(ExamAnswerUpload(55, JsonNull, 3, "2026-12-01T01:30:59Z")))
                    .getOrThrow()
            assertEquals(3L, saved.lastClientSeq)
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/ujian/pengerjaan/9/jawaban", request.target)
            assertEquals("Hk3", request.headers["X-Exam-Device"])
            val item =
                json.parseToJsonElement(request.body!!.utf8()).jsonObject["items"]!!.let {
                    (it as kotlinx.serialization.json.JsonArray)[0].jsonObject
                }
            assertEquals(JsonNull, item["answer"])
            assertEquals(3L, item["client_seq"]!!.jsonPrimitive.long)
        }

    @Test
    fun takeOverSendsNoDeviceToken() =
        runTest {
            reply(200, """{"device_token":"Baru"}""")
            assertEquals("Baru", repository.takeOver(9).getOrThrow())
            assertNull(server.takeRequest().headers["X-Exam-Device"])
        }

    @Test
    fun anotherDeviceIsAnApiErrorWithItsCode() =
        runTest {
            reply(409, """{"message":"Ujian ini sedang dibuka di perangkat lain.","code":"other_device"}""")
            val error =
                repository
                    .reportViolation(
                        9,
                        "lama",
                        ExamViolationRequest("page_left", "2026-12-01T01:40:00Z", 5_000),
                    ).exceptionOrNull()
            assertTrue(error is ApiError)
            error as ApiError
            assertEquals("other_device", error.code)
            assertEquals("Ujian ini sedang dibuka di perangkat lain.", error.message)
        }
}
