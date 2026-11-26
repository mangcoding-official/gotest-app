package com.gotest.app.core.data.api

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.ApiError
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.core.session.SignUpJoin
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.SignInOptions
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpForm
import com.gotest.app.feature.auth.domain.SignUpResult
import com.gotest.app.feature.ruang.domain.JoinedMitra
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

/** T-026 A2a step 3: Daftar with `mitra_link` (required 4) and the email code kept outside memory (required 10). */
class ApiSignUpTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private val signals = SessionSignals()
    private val session = SessionStore(MemoryStore(), PlainCipher())
    private val draftValues = MemoryStore()
    private var now = 1_000_000L
    private val drafts = SignUpDraftStore(draftValues) { now }
    private val join = SignUpJoin()
    private lateinit var repository: ApiSignUpRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        val acceptor = SignInAcceptor(session, signals, AccountChanges(), drafts, join)
        val options =
            object : SignInOptionsRepository {
                override suspend fun options() = SignInOptions(emailCode = true)
            }
        repository = ApiSignUpRepository(retrofit, json, acceptor, drafts, options, signals, join)
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

    private fun body(): JsonObject = json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    private val form =
        SignUpForm(
            fullName = "Raka",
            email = "raka@contoh.id",
            password = "rahasia123",
            level = EducationLevel.Smp,
            guardian = GuardianAnswer(adult = false, channel = GuardianChannel.WhatsApp, contact = "081234567890"),
        )

    private fun signedIn(
        consent: String,
        joined: String = "null",
    ) = """
        {"token":"9|baru","token_type":"Bearer","expires_at":null,"user":{"id":3,"name":"Raka"},
        "is_child":false,"consent":"$consent","joined":$joined}
        """.trimIndent()

    @Test
    fun theMitraLinkGoesToTheInfoAndTheSignUp() =
        runTest {
            drafts.mitraLink = "/gabung/KLS123"
            reply(
                200,
                """
                {"guardian_question":null,"invited_email":null,
                "join_notice":{"kind":"class","organization":"SMP Harapan","label":"8A"}}
                """.trimIndent(),
            )
            reply(201, signedIn(consent = "not_needed", joined = """{"code":"SMPH","role":"siswa","class":"8A"}"""))

            val info = repository.registrationInfo().getOrThrow()
            val sent = body()
            val result = repository.register(form.copy(guardian = null)).getOrThrow()

            assertEquals("/gabung/KLS123", sent.getValue("mitra_link").jsonPrimitive.content)
            assertTrue(info.joinNotice!!.isClass)
            assertEquals("8A", info.joinNotice.label)
            assertTrue(info.emailCode)
            val signUp = body()
            assertEquals("/gabung/KLS123", signUp.getValue("mitra_link").jsonPrimitive.content)
            assertEquals("smp", signUp.getValue("education_level").jsonPrimitive.content)
            assertEquals(SignUpResult.SignedIn, result)
            assertEquals("9|baru", session.token)
            assertNull(drafts.mitraLink)
            // A2a-1: the joined Mitra opens next, and the held link is not opened again (once).
            assertEquals(JoinedMitra("SMPH", "siswa", "8A"), join.joined.value)
            assertEquals("SMP Harapan", join.mitraName)
            assertTrue(join.takeUsedLink("/gabung/KLS123"))
            assertFalse(join.takeUsedLink("/gabung/KLS123"))
        }

    @Test
    fun aSignUpWithoutALinkLeavesTheNextLinkToOpen() =
        runTest {
            reply(201, signedIn(consent = "not_needed"))

            repository.register(form.copy(guardian = null)).getOrThrow()

            assertNull(join.joined.value)
            assertFalse(join.takeUsedLink("/gabung/KLS123"))
        }

    @Test
    fun aSignUpPageThatCannotBeReadSaysWhyInsteadOfLeavingOutItsQuestions() =
        runTest {
            reply(503, """{"message":"Gotest sedang dalam perbaikan.","code":"unavailable"}""")

            val error = repository.registrationInfo().exceptionOrNull() as ApiError

            assertEquals("Gotest sedang dalam perbaikan.", error.message)
        }

    @Test
    fun anUnderAgeAnswerSendsTheParentAndWaitsForThem() =
        runTest {
            reply(201, signedIn(consent = "pending"))

            val result = repository.register(form).getOrThrow()

            val sent = body()
            assertEquals("false", sent.getValue("adult").jsonPrimitive.content)
            assertEquals("whatsapp", sent.getValue("guardian_channel").jsonPrimitive.content)
            assertEquals("081234567890", sent.getValue("guardian_phone").jsonPrimitive.content)
            assertFalse(sent.containsKey("guardian_email"))
            assertEquals(SignUpResult.AwaitingConsent, result)
            assertTrue(signals.awaitingConsent.value)
        }

    @Test
    fun theEmailCodeSurvivesTheAppBeingClosedForFifteenMinutes() =
        runTest {
            reply(
                202,
                """
                {"message":null,"code":"email_code_sent","registration_token":"rt-1","sent_to":"r***@contoh.id",
                "resend_in":60}
                """.trimIndent(),
            )
            reply(201, signedIn(consent = "not_needed"))

            val sent = repository.register(form.copy(guardian = null)).getOrThrow()
            assertEquals(SignUpResult.EmailCodeSent("r***@contoh.id", 60), sent)
            assertNull(session.token)
            server.takeRequest()

            // Android closed the app: a new store over the same preferences still has the sign-up.
            val reopened = SignUpDraftStore(draftValues) { now }
            assertEquals("r***@contoh.id", reopened.emailCode?.sentTo)
            assertEquals(SignUpResult.EmailCodeSent("r***@contoh.id", 0), repository.pendingEmailCode())

            assertEquals(SignUpResult.SignedIn, repository.confirmEmailCode("123456").getOrThrow())
            val confirm = body()
            assertEquals("rt-1", confirm.getValue("registration_token").jsonPrimitive.content)
            assertEquals("123456", confirm.getValue("code").jsonPrimitive.content)
            assertNull(drafts.emailCode)
        }

    @Test
    fun aSignUpPastFifteenMinutesStartsOver() =
        runTest {
            drafts.saveEmailCode("rt-1", "r***@contoh.id")
            now += 15 * 60 * 1000L

            assertNull(repository.pendingEmailCode())
            val error = repository.confirmEmailCode("123456").exceptionOrNull() as ApiError
            assertTrue(error.fieldError("registration_token")!!.startsWith("Waktu untuk memasukkan kode sudah habis."))
            assertEquals(0, server.requestCount)
        }

    @Test
    fun aTokenTheServerNoLongerKnowsIsDropped() =
        runTest {
            drafts.saveEmailCode("rt-1", "r***@contoh.id")
            reply(
                422,
                """{"message":"Habis.","code":"validation","errors":{"registration_token":["Habis."]}}""",
            )

            assertTrue(repository.resendEmailCode().isFailure)
            assertNull(drafts.emailCode)
        }

    @Test
    fun aCodeThatCouldNotGoOutMakesTheAccountWaitForItsEmail() =
        runTest {
            drafts.saveEmailCode("rt-1", "r***@contoh.id")
            reply(
                201,
                """
                {"message":null,"code":"email_verification_pending","token":"9|baru","user":{"id":3,"name":"Raka"},
                "consent":"not_needed","sent_to":"r***@contoh.id"}
                """.trimIndent(),
            )

            repository.resendEmailCode().getOrThrow()

            assertEquals("9|baru", session.token)
            assertTrue(signals.awaitingEmailVerification.value)
            assertNull(drafts.emailCode)
        }

    @Test
    fun theWaitingPageEndsOnceTheParentApproved() =
        runTest {
            signals.markAwaitingConsent()
            reply(
                200,
                """
                {"waiting":false,"status":"approved","contact":"0812-****-890","channel":"whatsapp",
                "channels":["whatsapp","email"],"delivery":"sent","expiresAt":"2026-12-01T10:00:00+07:00"}
                """.trimIndent(),
            )

            val waiting = repository.refreshConsent().getOrThrow()
            val state = repository.observeConsent().filterNotNull().first()

            assertEquals(listOf(GuardianChannel.WhatsApp, GuardianChannel.Email), state.channels)
            assertEquals("2026-12-01T03:00:00Z", state.expiresAt.toString())
            assertFalse(waiting)
            assertFalse(signals.awaitingConsent.value)
        }

    private class MemoryStore : KeyValueStore {
        private val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private class PlainCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain

        override fun decrypt(encrypted: String): String = encrypted
    }
}
