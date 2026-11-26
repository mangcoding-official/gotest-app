package com.gotest.app.feature.home

import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.home.data.ApiDashboardRepository
import com.gotest.app.feature.home.data.ApiSearchRepository
import com.gotest.app.feature.home.domain.MitraTaskCard
import com.gotest.app.feature.home.domain.SearchTarget
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

/** T-026 A2c-1: Beranda from `GET /dashboard` and Cari from `GET /search`, with the server's JSON shapes. */
class ApiHomeRepositoriesTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var dashboard: ApiDashboardRepository
    private lateinit var search: ApiSearchRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        dashboard = ApiDashboardRepository(retrofit, json)
        search = ApiSearchRepository(retrofit, json)
    }

    @After
    fun tearDown() = server.close()

    private fun reply(body: String) =
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body(body)
                .build(),
        )

    @Test
    fun theDashboardSplitsPackagesIntoTheCatalogRowsOfTheirMode() =
        runTest {
            reply(
                """
                {"schoolSpaces":[{"name":"SMA 1","code":"SMA1","role":"pengajar","nextSession":null}],
                "mitraTasks":[{"organizationCode":"KLG1","organizationName":"Keluarga A","todayCount":2,"lateCount":0}],
                "categories":[{"id":1,"name":"UTBK-SNBT","code":"utbk"}],
                "packages":[
                  {"id":11,"name":"Latihan 1","description":null,"mode":"practice","category_id":1,
                   "category":{"id":1,"name":"UTBK-SNBT"},"continueAttemptId":null,"premium":false,"premiumLocked":false,
                   "allowRetry":true,"lastAttemptId":5,"completed":true,"window":null,"finishedCount":1,
                   "lastFinishedAt":"2026-11-20T10:00:00+07:00","lastScore":80,"lastScoreLabel":"Nilai",
                   "timerLabel":"Waktu total: 10 menit","timerOptional":true,
                   "composition":[{"subject":"Penalaran Umum","questionCount":10,"difficulty":"Sedang"}],
                   "ruleLabel":"…","sections":[],"leaderboard":[]},
                  {"id":21,"name":"Try Out 1","mode":"tryout","category_id":1,"category":{"id":1,"name":"UTBK-SNBT"},
                   "continueAttemptId":44,"premium":true,"premiumLocked":true,"allowRetry":false,"finishedCount":0,
                   "window":{"state":"open","opensAt":null,"closesAt":null},"ruleLabel":"Timer wajib aktif per subtes.",
                   "sections":[{"id":3,"title":"Penalaran Umum","questionCount":20,"durationMinutes":30}],
                   "leaderboard":[{"rank":1,"name":"Sinta","score":700,"submittedAt":null,"durationUsed":100}]}
                ],
                "recentAttempts":[{"id":5,"packageName":"Latihan 1","categoryName":"UTBK-SNBT","mode":"practice",
                  "score":80,"scoreLabel":"Nilai","submittedAt":"2026-11-20T10:00:00+07:00"}],
                "stats":{"questionsDone":10,"accuracy":80,"packagesDone":1},
                "continueAttempt":{"id":44,"mode":"tryout","packageName":"Try Out 1","categoryName":"UTBK-SNBT",
                  "questionCount":20,"answeredCount":5,"durationMinutes":30},
                "leaderboard":{"top":[{"rank":1,"userId":7,"name":"Sinta","points":700}],"current":null},
                "banner":{"title":"Promo","altText":"Promo","href":"/practice","imageUrl":"/storage/banners/a.png"}}
                """.trimIndent(),
            )

            val page = dashboard.dashboard().getOrThrow()

            assertEquals("/api/v1/dashboard", server.takeRequest().url.encodedPath)
            val practice = page.practicePackages.single()
            assertEquals("UTBK-SNBT", practice.categoryName)
            assertEquals(1L, practice.categoryId)
            assertTrue(practice.timerOptional)
            assertEquals("Penalaran Umum", practice.composition.single().subject)
            val tryout = page.tryoutPackages.single()
            assertEquals(44L, tryout.continueAttemptId)
            assertEquals("open", tryout.windowState)
            assertEquals(30, tryout.sections.single().durationMinutes)
            assertEquals(MitraTaskCard("KLG1", "Keluarga A", 2, 0), page.mitraTasks.single())
            assertEquals(false, page.schoolSpaces.single().isStudent)
            assertTrue(page.continueAttempt!!.isTryout)
            assertEquals(
                700,
                page.leaderboard.top
                    .single()
                    .points,
            )
            assertNull(page.leaderboard.current)
            assertEquals("/storage/banners/a.png", page.banner?.imageUrl)
        }

    @Test
    fun searchSendsTheQueryAndReadsWhatEachResultOpens() =
        runTest {
            reply(
                """
                {"query":"utbk","groups":[
                  {"key":"practice","title":"Paket Latihan","items":[{"id":"practice-11","title":"Latihan 1",
                    "href":"https://gotest.id/practice/packages/11","target":{"type":"practice_package","id":11},
                    "actionLabel":"Mulai Latihan","meta":["UTBK-SNBT","20 Soal"]}]},
                  {"key":"help","title":"Artikel & Bantuan","items":[{"id":"help-akun","title":"Akun",
                    "href":"https://gotest.id/bantuan/akun","target":{"type":"help_topic","slug":"akun"},
                    "actionLabel":"Lihat","meta":[]}]}
                ]}
                """.trimIndent(),
            )

            val page = search.search("utbk & snbt").getOrThrow()

            assertEquals("utbk & snbt", server.takeRequest().url.queryParameter("q"))
            assertEquals(
                SearchTarget("practice_package", id = 11),
                page.groups[0]
                    .items
                    .single()
                    .target,
            )
            assertEquals(
                "akun",
                page.groups[1]
                    .items
                    .single()
                    .target
                    ?.slug,
            )
            assertEquals(
                listOf("UTBK-SNBT", "20 Soal"),
                page.groups[0]
                    .items
                    .single()
                    .meta,
            )
        }
}
