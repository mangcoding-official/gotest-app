package com.gotest.app.feature.leaderboard

import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.leaderboard.data.ApiLeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardPeriod
import com.gotest.app.feature.leaderboard.domain.LeaderboardScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Leaderboard calls of API module F, with the app's own Json. */
class ApiLeaderboardPagesRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiLeaderboardPagesRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiLeaderboardPagesRepository(retrofit, json)
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
    fun `the filters go in the query and an empty category or subject is left out`() =
        runTest {
            reply(
                """
                {"filters":{"scope":"package","category":null,"subject":null,"package":null,"period":"7d","limit":40}}
                """.trimIndent(),
            )
            val page =
                repository
                    .leaderboard(
                        LeaderboardFilters(
                            scope = LeaderboardScope.Package,
                            period = LeaderboardPeriod.Week,
                            limit = 40,
                        ),
                    ).getOrThrow()

            val url = server.takeRequest().url
            assertEquals("package", url.queryParameter("scope"))
            assertEquals("7d", url.queryParameter("period"))
            assertEquals("40", url.queryParameter("limit"))
            assertNull(url.queryParameter("category"))
            assertNull(url.queryParameter("subject"))
            assertEquals(LeaderboardScope.Package, page.filters.scope)
        }

    @Test
    fun `a board reads the account row, cards and the taxonomy`() =
        runTest {
            reply(
                """
                {"filters":{"scope":"global","category":7,"subject":null,"package":null,"period":"all","limit":20},
                 "options":{"categories":[{"id":7,"name":"UTBK-SNBT"}],"subjects":[{"id":1,"name":"Penalaran Umum"}],"packages":[]},
                 "top":[{"rank":1,"userId":5,"name":"Rina","attempts":3,"points":912,"change":0,"isCurrentUser":false}],
                 "rows":[],
                 "current":{"rank":42,"userId":9,"name":"Gilang","attempts":1,"points":500,"change":-2,"isCurrentUser":true},
                 "packageCards":[{"id":21,"name":"Latihan Aljabar","categoryName":"TKA SMA","questionCount":20,"durationMinutes":0,
                   "timeLabel":"Tanpa batas waktu","rank":null,"totalParticipants":0,"points":null,"pointsLabel":"Nilai",
                   "accuracy":null,"change":null,"completed":false,"href":"https://gotest.id/leaderboard/latihan/21"}],
                 "taxonomyHierarchy":{"groups":[],"branches":[{"id":1,"category_id":7,"subject_id":1,"group_id":null,
                   "status":"active","order_number":1,"master_status":"active","category_status":"active"}],"topicLinks":[]},
                 "totalParticipants":137,"hasMore":true,"updatedAt":"2026-11-18T07:05:00+00:00"}
                """.trimIndent(),
            )
            val page = repository.leaderboard(LeaderboardFilters()).getOrThrow()

            assertEquals(42, page.current?.rank)
            assertEquals(-2, page.current?.change)
            assertEquals("Tanpa batas waktu", page.packageCards.single().timeLabel)
            assertNull(page.packageCards.single().change)
            assertEquals(
                1L,
                page.taxonomyHierarchy.branches
                    .single()
                    .subjectId,
            )
            assertTrue(page.hasMore)
        }

    @Test
    fun `Try Out and Latihan boards use their own addresses`() =
        runTest {
            reply(
                """
                {"package":{"id":9,"name":"Try Out 1","questionCount":160,"durationMinutes":195,"rankingOpensAt":null},
                 "top":[]}
                """.trimIndent(),
            )
            reply(
                """
                {"mode":"practice","package":{"id":21,"name":"Latihan Aljabar","questionCount":20,"durationMinutes":0,
                 "timeLabel":"10 Menit","pointsLabel":"Nilai","rankingOpensAt":null},
                 "top":[{"rank":1,"userId":5,"name":"Rina","points":95,"submittedAt":"2026-11-10T03:00:00+00:00","change":0,"isCurrentUser":false}]}
                """.trimIndent(),
            )

            val tryout = repository.tryoutBoard(9).getOrThrow()
            val practice = repository.practiceBoard(21).getOrThrow()

            assertEquals("/api/v1/leaderboard/packages/9", server.takeRequest().target)
            assertEquals("/api/v1/leaderboard/latihan/21", server.takeRequest().target)
            assertEquals(false, tryout.isPractice)
            assertTrue(practice.isPractice)
            assertEquals("10 Menit", practice.board.timeLabel)
        }
}
