package com.gotest.app.feature.multiplayer

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.multiplayer.data.ApiMainBarengRepository
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.savedValue
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerValue
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
import retrofit2.Retrofit

/** The Main Bareng routes of API module E with the app's Json: addresses per space, bodies, and `kind` refusals. */
class ApiMainBarengRepositoryTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiMainBarengRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiMainBarengRepository(retrofit, json)
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
    fun theHubAndHistoryOfASpaceUseItsAddress() =
        runTest {
            reply(200, HUB)
            reply(
                200,
                """{"games":[{"code":"HX1A2B","mode":"duel","status":"finished","subjectName":"PU",""" +
                    """"questionCount":10,"durationMinutes":10,"playerCount":2,""" +
                    """"opponentName":"Rina","outcome":"win",""" +
                    """"rank":null,"closedAt":"2026-11-22T12:40:00+07:00"}]}""",
            )
            reply(200, """{"game":null}""")

            val hub = repository.hub("SMP1BTG").getOrThrow()
            val history = repository.history("SMP1BTG").getOrThrow()
            val active = repository.active().getOrThrow()

            assertEquals("GET /api/v1/ruang/SMP1BTG/main-bareng", take().first)
            assertEquals("GET /api/v1/ruang/SMP1BTG/main-bareng/riwayat", take().first)
            assertEquals("GET /api/v1/main-bareng/aktif", take().first)
            assertTrue(hub.access.active)
            assertEquals("SMP1BTG", hub.organization?.code)
            assertEquals(
                30,
                hub.options
                    .single()
                    .subjects
                    .single()
                    .questionCount,
            )
            assertEquals("win", history.single().outcome)
            assertNull(active)
        }

    @Test
    fun creatingSendsTheWebFieldsAndReadsTheGame() =
        runTest {
            reply(201, GAME)
            reply(201, GAME)

            val game = repository.create(null, GameSettings("room", 2, 11, listOf(111), 20, 15)).getOrThrow()
            repository.create("SMP1BTG", GameSettings("duel", 2, 11, emptyList(), 5, 10)).getOrThrow()

            assertEquals(
                "POST /api/v1/main-bareng" to
                    """{"mode":"room","category_id":2,"subject_id":11,"topic_ids":[111],"question_count":20,""" +
                    """"duration_minutes":15}""",
                take(),
            )
            assertEquals("POST /api/v1/ruang/SMP1BTG/main-bareng", take().first)
            assertEquals(GameState.STATUS_WAITING, game.status)
            assertEquals(listOf(true, false), game.players.map { it.isHost })
            assertEquals("2026-11-23T10:05:00+07:00", game.lobbyExpiresAt)
        }

    @Test
    fun theStepsOfAGameUseItsCode() =
        runTest {
            reply(200, GAME)
            reply(200, GAME)
            reply(200, """{"ok":true}""")
            reply(200, """{"ok":true}""")
            reply(200, GAME)
            reply(
                200,
                """{"game":$GAME_BODY,"result":{"rows":[],"outcome":"draw","items":[{"order":1,"status":"empty"}]}}""",
            )

            repository.lookup("ABC123").getOrThrow()
            repository.join("ABC123").getOrThrow()
            repository.answer("ABC123", 901, AnswerData(selectedOptions = listOf(0, 2))).getOrThrow()
            repository.leave("ABC123").getOrThrow()
            repository.playAgain("ABC123").getOrThrow()
            val review = repository.result("ABC123").getOrThrow()

            assertEquals("GET /api/v1/main-bareng/kode/ABC123", take().first)
            assertEquals("POST /api/v1/main-bareng/kode/ABC123/gabung", take().first)
            assertEquals(
                "PUT /api/v1/main-bareng/ABC123/jawaban/901" to """{"answer_data":{"selected_options":[0,2]}}""",
                take(),
            )
            assertEquals("POST /api/v1/main-bareng/ABC123/keluar", take().first)
            assertEquals("POST /api/v1/main-bareng/ABC123/main-lagi", take().first)
            assertEquals("GET /api/v1/main-bareng/ABC123/hasil", take().first)
            assertEquals("draw", review.result.outcome)
        }

    @Test
    fun questionsComeWithTheSavedAnswerInItsShape() =
        runTest {
            reply(
                200,
                """{"questions":[""" +
                    question(1, "multiple_choice", "2") + "," +
                    question(2, "multiple_select", "[0,3]") + "," +
                    question(3, "statement_table", "[1,null]") + "," +
                    question(4, "multiple_choice", "null") + "]}",
            )

            val questions = repository.questions("ABC123").getOrThrow()

            assertEquals("GET /api/v1/main-bareng/ABC123/soal", take().first)
            assertEquals(
                listOf(
                    AnswerValue.Single(2),
                    AnswerValue.Multiple(listOf(0, 3)),
                    AnswerValue.Statement(listOf(1, null)),
                    null,
                ),
                questions.map { it.savedValue() },
            )
        }

    @Test
    fun aRuleOfTheGameKeepsItsKindAndSentence() =
        runTest {
            reply(422, """{"message":"Duel ini sudah ada 2 pemain.","code":"full","kind":"full"}""")
            reply(403, """{"message":"Akses ditolak.","code":"forbidden"}""")

            val full = repository.join("PENUH1").exceptionOrNull() as ApiError
            val forbidden = repository.start("ABC123").exceptionOrNull() as ApiError

            assertEquals(
                Triple(422, "full", "Duel ini sudah ada 2 pemain."),
                Triple(full.status, full.code, full.message),
            )
            assertEquals("POST /api/v1/main-bareng/kode/PENUH1/gabung", take().first)
            assertEquals(403 to "forbidden", forbidden.status to forbidden.code)
        }

    private fun question(
        id: Int,
        type: String,
        answer: String,
    ) = """{"id":$id,"order":$id,"questionType":"$type","stimulusId":null,"content":{},"answer":$answer}"""

    private companion object {
        const val HUB =
            """{"access":{"status":"active","label":"Akses aktif"},""" +
                """"options":[{"id":2,"name":"UTBK-SNBT","subjects":""" +
                """[{"id":11,"name":"Penalaran Umum","questionCount":30,"topics":[]}]}],""" +
                """"organization":{"name":"SMP 1 Bantargedung","code":"SMP1BTG","role":"siswa"}}"""
        const val GAME_BODY =
            """{"code":"ABC123","mode":"room","status":"waiting","cancelReason":null,"organization":null,""" +
                """"categoryName":"UTBK-SNBT","subjectName":"Penalaran Umum","topicNames":[],"questionCount":20,""" +
                """"durationMinutes":15,"maxPlayers":10,"hostName":"Gilang","players":[""" +
                """{"id":1,"name":"Gilang","isHost":true,"isYou":true,"finished":false,""" +
                """"joinedAt":"2026-11-23T10:00:00+07:00",""" +
                """"finishedAt":null},{"id":2,"name":"Rina","isHost":false,"isYou":false,"finished":false,""" +
                """"joinedAt":"2026-11-23T10:00:30+07:00","finishedAt":null}],""" +
                """"youAreHost":true,"youFinished":false,""" +
                """"lobbyExpiresAt":"2026-11-23T10:05:00+07:00","startedAt":null,"endsAt":null,"closedAt":null,""" +
                """"serverNow":"2026-11-23T10:01:00+07:00","nextGameCode":null}"""
        const val GAME = """{"game":$GAME_BODY}"""
    }
}
