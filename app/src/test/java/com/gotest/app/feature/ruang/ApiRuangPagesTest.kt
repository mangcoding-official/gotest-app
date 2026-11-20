package com.gotest.app.feature.ruang

import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.practice.data.ApiPracticeRepository
import com.gotest.app.feature.ruang.data.ApiRuangRepository
import com.gotest.app.feature.ruang.data.ApiStudyPlanRepository
import com.gotest.app.feature.ruang.domain.RoomTaskAction
import com.gotest.app.feature.ruang.domain.RoomTaskTarget
import com.gotest.app.feature.ruang.domain.RuangPractice
import com.gotest.app.feature.tryout.domain.TryoutSession
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
import retrofit2.Retrofit

/**
 * `GET /ruang/{code}` with Try Out, Latihan sekolah, and progress (API module G, A2c), the `target` of a task action
 * in `GET /ruang/{code}/rencana` (T-031), and `POST /ruang/{code}/latihan/{id}` (module C), with the app's Json.
 */
class ApiRuangPagesTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var retrofit: Retrofit

    @Before
    fun setUp() {
        server.start()
        retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
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
    fun ruangReadsSessionsPracticeAndProgress() =
        runTest {
            reply(200, RUANG)
            val page = ApiRuangRepository(retrofit, json).ruang("SMP1BTG").getOrThrow()

            assertEquals("/api/v1/ruang/SMP1BTG", server.takeRequest().url.encodedPath)
            val session = page.sessions.single()
            assertEquals(TryoutSession.CLOSED, session.state)
            assertEquals(953L, session.attemptId)
            assertTrue(session.attemptDone)
            assertEquals(4, session.rank)
            assertEquals("SMP1BTG", session.partner?.code)

            val (open, closed) = page.practice
            assertEquals(RuangPractice.TIMER_PACKAGE, open.timerMode)
            assertEquals(600, open.durationSeconds)
            assertTrue(open.canStart)
            assertEquals(60.5, open.lastScore)
            assertTrue(closed.closed)
            assertNull(closed.lastScore)
            assertFalse(closed.canStart)

            assertEquals(listOf("latihan", "simulasi"), page.progress.attempts.map { it.kind })
            assertEquals(65, page.progress.attempts[1].percent)
            assertEquals(
                "Matematika",
                page.progress.subjects
                    .single()
                    .name,
            )
        }

    @Test
    fun ruangWithoutTheNewKeysStillReads() =
        runTest {
            reply(200, """{"organization":{"name":"Bimbel Cerdas","code":"BCERDAS"},"former":true}""")
            val page = ApiRuangRepository(retrofit, json).ruang("BCERDAS").getOrThrow()

            assertTrue(page.sessions.isEmpty())
            assertTrue(page.practice.isEmpty())
            assertTrue(page.progress.attempts.isEmpty())
        }

    @Test
    fun planReadsTheTargetOfEveryTaskAction() =
        runTest {
            reply(200, PLAN)
            val page = ApiStudyPlanRepository(retrofit, json).plan("SMP1BTG").getOrThrow()

            val tasks =
                page.plans
                    .single()
                    .weeks
                    .single()
                    .tasks
            val (start, practiceResult, quizResult) = tasks.map { it.action }
            assertEquals(RoomTaskAction.CONTINUE, start?.kind)
            assertEquals("study_task_start", start?.target?.type)
            assertEquals(214L, start?.target?.id)
            assertEquals(RoomTaskAction.RESULT, practiceResult?.kind)
            assertEquals(RoomTaskTarget.PRACTICE_RESULT, practiceResult?.target?.type)
            assertEquals(1207L, practiceResult?.target?.id)
            assertEquals(RoomTaskTarget.QUIZ_RESULT, quizResult?.target?.type)
            assertEquals(90L, quizResult?.target?.id)
            assertNull(tasks[3].action)
            assertEquals("Tugas ini dibuka 24 November 2026.", tasks[3].blockedReason)
        }

    @Test
    fun startInRuangPostsToTheMitraAddressAndReadsTheAttempt() =
        runTest {
            reply(201, """{"attempt":{"id":1301,"status":"started","server_now":"2026-11-20T03:00:00.000+00:00"}}""")
            val attempt = ApiPracticeRepository(retrofit, json).startInRuang("SMP1BTG", 601).getOrThrow()

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/ruang/SMP1BTG/latihan/601", request.url.encodedPath)
            assertEquals(1301L, attempt.id)
        }

    private companion object {
        val RUANG =
            """
            {"organization":{"name":"SMP 1 Bantargedung","code":"SMP1BTG"},"former":false,
             "today":{"plan":null,"tasks":[]},"planProgress":null,
             "sessions":[{"id":413,"title":"Try Out Tengah Semester","description":null,"packageId":301,
               "packageName":"Try Out UTBK-SNBT 1","state":"closed","startsAt":"2026-10-11T01:00:00+00:00",
               "endsAt":"2026-10-11T03:00:00+00:00","capacity":null,"registeredCount":32,"isFull":false,
               "registered":true,"attemptId":953,"attemptStatus":"submitted","attemptDone":true,"score":640,
               "submittedAt":"2026-10-11T02:40:00+00:00","rank":4,"totalParticipants":31,
               "partner":{"name":"SMP 1 Bantargedung","code":"SMP1BTG"}}],
             "practice":[
               {"id":602,"name":"Latihan Aljabar Kelas 8","description":null,"questionCount":5,"timerMode":"package",
                "durationSeconds":600,"closed":false,"continueAttemptId":null,"canStart":true,"finishedCount":2,
                "lastAttemptId":903,"lastScore":60.5},
               {"id":603,"name":"Latihan Bilangan Bulat","description":null,"questionCount":5,"timerMode":"none",
                "durationSeconds":null,"closed":true,"continueAttemptId":null,"canStart":false,"finishedCount":0,
                "lastAttemptId":null,"lastScore":null}],
             "progress":{"attempts":[
               {"id":904,"kind":"latihan","title":"Latihan Bilangan Bulat","submittedAt":"2026-10-02T08:10:00+00:00",
                "correct":2,"total":5,"percent":40,"score":40},
               {"id":953,"kind":"simulasi","title":"Try Out Tengah Semester","submittedAt":"2026-10-11T02:40:00+00:00",
                "correct":13,"total":20,"percent":65,"score":640}],
              "subjects":[{"name":"Matematika","correct":8,"total":15,"percent":53}]}}
            """.trimIndent()

        private fun task(
            id: Int,
            type: String,
            action: String,
            blocked: String = "null",
        ) = """{"id":$id,"planId":31,"title":"Tugas $id","type":"$type","workOn":"2026-11-20",
            |"dueOn":"2026-11-21","status":"in_progress","action":$action,"blockedReason":$blocked}
            """.trimMargin()

        val PLAN =
            """
            {"organization":{"name":"SMP 1 Bantargedung","code":"SMP1BTG"},"former":false,"primaryPlanId":31,
             "plans":[{"id":31,"title":"Persiapan PTS Semester 1","startsOn":"2026-11-09","examDate":"2026-12-06",
               "state":"running","weekCount":4,"weeks":[{"number":2,"startsOn":"2026-11-16","endsOn":"2026-11-22","tasks":[
                ${task(
                214,
                "gotest_practice",
                """{"kind":"continue","url":"/ruang/SMP1BTG/tugas/214/mulai","target":{"type":"study_task_start","id":214}}""",
            )},
                ${task(
                215,
                "gotest_practice",
                """{"kind":"result","url":"/practice/attempts/1207/result","target":{"type":"practice_result","id":1207}}""",
            )},
                ${task(
                216,
                "family_quiz",
                """{"kind":"result","url":"/ruang/SMP1BTG/kuis/hasil/90","target":{"type":"quiz_result","id":90}}""",
            )},
                ${task(217, "external", "null", "\"Tugas ini dibuka 24 November 2026.\"")}
               ]}]}],
             "planProgress":null}
            """.trimIndent()
    }
}
