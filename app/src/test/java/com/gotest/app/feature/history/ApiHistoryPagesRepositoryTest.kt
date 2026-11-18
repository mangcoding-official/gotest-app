package com.gotest.app.feature.history

import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.history.data.ApiHistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryMode
import com.gotest.app.feature.history.domain.HistorySort
import kotlinx.coroutines.test.runTest
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

/** Riwayat calls of API module F, with the app's own Json. */
class ApiHistoryPagesRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiHistoryPagesRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiHistoryPagesRepository(retrofit, json)
    }

    @After
    fun tearDown() = server.close()

    private fun reply(body: String) =
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )

    @Test
    fun `the first page with default filters sends no query`() =
        runTest {
            reply("""{"attempts":[],"pagination":{"nextPage":null,"total":0}}""")
            repository.history(HistoryFilters(), page = 1)

            assertEquals("/api/v1/riwayat", server.takeRequest().target)
        }

    @Test
    fun `chosen filters and a later page go in the query`() =
        runTest {
            reply("""{"attempts":[]}""")
            val filters =
                HistoryFilters(q = "  aljabar ", mode = HistoryMode.Practice, category = 4, sort = HistorySort.Oldest)
            repository.history(filters, page = 2)

            val url = server.takeRequest().url
            assertEquals("aljabar", url.queryParameter("q"))
            assertEquals("practice", url.queryParameter("mode"))
            assertEquals("4", url.queryParameter("category"))
            assertEquals("oldest", url.queryParameter("sort"))
            assertEquals("2", url.queryParameter("page"))
        }

    @Test
    fun `a page reads results, stats and the next page`() =
        runTest {
            reply(
                """
                {"categories":[{"id":1,"name":"TKA SMA"}],
                 "filters":{"q":"","mode":"tryout","category":null,"sort":"newest"},
                 "attempts":[{"id":9,"packageName":"Try Out 3","sessionTitle":null,"categoryName":"UTBK-SNBT",
                   "mode":"tryout","submittedAt":"2026-11-17T08:30:00+00:00","durationSeconds":5712,
                   "durationLabel":"95 Menit 12 Detik","accuracy":71,"score":612.5,"ranked":false,"disqualified":true}],
                 "pagination":{"nextPage":2,"total":27},
                 "stats":{"questionsDone":480,"accuracy":68,"packagesDone":19}}
                """.trimIndent(),
            )
            val page = repository.history(HistoryFilters(), 1).getOrThrow()

            assertEquals(HistoryMode.Tryout, page.filters.mode)
            assertEquals(2, page.pagination.nextPage)
            assertEquals(27, page.pagination.total)
            assertEquals(19, page.stats.packagesDone)
            val result = page.attempts.single()
            assertTrue(result.isTryout)
            assertNull(result.sessionTitle)
            assertEquals(false, result.ranked)
            assertEquals(true, result.disqualified)
        }

    @Test
    fun `the detail reads sections and the package top five`() =
        runTest {
            reply(
                """
                {"attempt":{"id":9,"packageName":"Latihan Aljabar","categoryName":"TKA SMA","mode":"practice",
                  "durationLabel":"18 Menit 4 Detik","score":92,"scoreLabel":"Nilai","packageId":21,
                  "sections":[{"name":"Aljabar","correct":9,"wrong":1,"empty":0}]},
                 "ranking":[]}
                """.trimIndent(),
            )
            val detail = repository.detail(9).getOrThrow()

            assertEquals("/api/v1/riwayat/attempts/9", server.takeRequest().target)
            assertFalse(detail.attempt.isTryout)
            assertEquals("Nilai", detail.attempt.scoreLabel)
            assertEquals(
                9,
                detail.attempt.sections
                    .single()
                    .correct,
            )
        }

    @Test
    fun `the export hands back the CSV as it came`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .addHeader("Content-Type", "text/csv")
                    .body("Paket,Skor\nTry Out 3,612\n")
                    .build(),
            )
            val csv = repository.export().getOrThrow().use { it.readBytes().decodeToString() }

            assertEquals("/api/v1/riwayat/export", server.takeRequest().target)
            assertEquals("Paket,Skor\nTry Out 3,612\n", csv)
        }

    @Test
    fun `a server refusal comes back as a failure`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(404).build())

            assertTrue(repository.detail(404).isFailure)
        }
}
