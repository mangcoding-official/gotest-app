package com.gotest.app.core.data.api

import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.network.NetworkModule
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.auth.domain.AuthFlowError
import com.gotest.app.feature.auth.domain.FamilyChild
import com.gotest.app.feature.auth.domain.GoogleSignIn
import com.gotest.app.feature.auth.domain.PhoneVerification
import com.gotest.app.feature.auth.domain.SignUpResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** T-026 A2a step 4: kode email, Nomor HP, Masuk Anak, and resetting the password against the API. */
class ApiAuthFlowTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private val session = SessionStore(MemoryStore(), PlainCipher())
    private val drafts = SignUpDraftStore(MemoryStore())
    private lateinit var flows: ApiAuthFlowRepository
    private lateinit var emailCodes: ApiEmailCodeSignInRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        val acceptor = SignInAcceptor(session, SessionSignals(), AccountChanges(), drafts)
        flows = ApiAuthFlowRepository(retrofit, json, acceptor, drafts)
        emailCodes = ApiEmailCodeSignInRepository(retrofit, json, acceptor)
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

    private val signedIn =
        """
        {"token":"5|hp","token_type":"Bearer","user":{"id":4,"name":"Sinta"},"is_child":false,
        "consent":"not_needed"}
        """.trimIndent()

    @Test
    fun aNewNumberKeepsItsTokenAndSignsUpWithTheMitraLink() =
        runTest {
            drafts.mitraLink = "/undangan/abc"
            reply(
                422,
                """
                {"message":"Nomor ini belum punya akun. Lanjutkan dengan mengisi nama.","code":"phone_not_registered",
                "registration_token":"pt-1","phone":"0812-3456-7890"}
                """.trimIndent(),
            )
            reply(201, signedIn)

            val verified = flows.verifyPhoneCode("6281234567890", "123456").getOrThrow()
            server.takeRequest()
            val result = flows.completePhoneRegistration("Sinta", EducationLevel.Kuliah, guardian = null).getOrThrow()

            assertEquals(PhoneVerification.NeedsRegistration("0812-3456-7890"), verified)
            val sent = body()
            assertEquals("pt-1", sent.getValue("registration_token").jsonPrimitive.content)
            assertEquals("/undangan/abc", sent.getValue("mitra_link").jsonPrimitive.content)
            assertEquals("kuliah", sent.getValue("education_level").jsonPrimitive.content)
            assertEquals(SignUpResult.SignedIn, result)
            assertEquals("5|hp", session.token)
            assertNull(drafts.phoneToken)
            assertNull(drafts.mitraLink)
        }

    @Test
    fun aNewGoogleAccountFinishesWithItsNameAndTheMitraLink() =
        runTest {
            drafts.mitraLink = "/gabung/KLS123"
            reply(
                422,
                """
                {"message":"Akun Google ini belum terdaftar. Lanjutkan dengan mengisi data diri.",
                "code":"google_not_registered","registration_token":"gt-1","name":"Gilang","email":"gilang@mail.com"}
                """.trimIndent(),
            )
            reply(201, signedIn)

            val picked = flows.signInWithGoogle("id-token").getOrThrow()
            assertEquals("id-token", body().getValue("id_token").jsonPrimitive.content)
            val result =
                flows
                    .completeGoogleRegistration(
                        "Gilang P",
                        EducationLevel.Kuliah,
                        guardian = null,
                    ).getOrThrow()

            assertEquals(GoogleSignIn.NeedsRegistration("Gilang", "gilang@mail.com"), picked)
            val sent = body()
            assertEquals("gt-1", sent.getValue("registration_token").jsonPrimitive.content)
            assertEquals("Gilang P", sent.getValue("name").jsonPrimitive.content)
            assertEquals("/gabung/KLS123", sent.getValue("mitra_link").jsonPrimitive.content)
            assertEquals(SignUpResult.SignedIn, result)
            assertEquals("5|hp", session.token)
            assertNull(drafts.googleToken)
        }

    @Test
    fun aKnownGoogleAccountSignsIn() =
        runTest {
            reply(200, signedIn.replace("\"consent\"", "\"linked\":true,\"consent\""))

            assertEquals(GoogleSignIn.SignedIn, flows.signInWithGoogle("id-token").getOrThrow())
            assertEquals("/api/v1/auth/google", server.takeRequest().url.encodedPath)
            assertEquals("5|hp", session.token)
        }

    @Test
    fun aGoogleTokenTheServerRefusesSaysSoUnderItsField() =
        runTest {
            reply(
                422,
                """
                {"message":"Gagal masuk dengan Google. Silakan coba lagi.","code":"validation",
                "errors":{"id_token":["Gagal masuk dengan Google. Silakan coba lagi."]}}
                """.trimIndent(),
            )

            val error = flows.signInWithGoogle("bad").exceptionOrNull() as AuthFlowError

            assertEquals("id_token", error.field)
            assertNull(session.token)
        }

    @Test
    fun masukAnakSendsTheChildAsANumberAndKeepsTheToken() =
        runTest {
            reply(200, """{"children":[{"id":12,"name":"Raka"}]}""")
            reply(200, signedIn.replace("\"is_child\":false", "\"is_child\":true"))

            val children = flows.findFamily("KLG123").getOrThrow()
            server.takeRequest()
            flows.signInChild("KLG123", children.first().id, "1234").getOrThrow()

            assertEquals(listOf(FamilyChild("12", "Raka")), children)
            assertEquals("12", body().getValue("child_id").jsonPrimitive.content)
            assertEquals("5|hp", session.token)
        }

    @Test
    fun theEmailCodeSignsInWithTheEmailItWasSentTo() =
        runTest {
            reply(200, """{"message":null,"email_code_sent_to":"g***@mail.com","resend_in":60}""")
            reply(200, signedIn)

            assertEquals("g***@mail.com", emailCodes.sendCode(" gilang@mail.com ").getOrThrow())
            server.takeRequest()
            emailCodes.verify("123456").getOrThrow()

            val sent = body()
            assertEquals("gilang@mail.com", sent.getValue("email").jsonPrimitive.content)
            assertEquals("123456", sent.getValue("code").jsonPrimitive.content)
            assertEquals("5|hp", session.token)
        }

    @Test
    fun aRefusalKeepsTheServersTextUnderItsField() =
        runTest {
            reply(
                422,
                """
                {"message":"Kode salah.","code":"validation",
                "errors":{"code":["Kode salah. Sisa 4 kali percobaan."]}}
                """.trimIndent(),
            )
            reply(401, """{"message":"Akunmu diblokir karena kecurangan.","code":"account_blocked"}""")

            val wrong = flows.verifyPhoneCode("6281234567890", "000000").exceptionOrNull() as AuthFlowError
            val blocked = flows.signInChild("KLG123", "12", "1234").exceptionOrNull() as AuthFlowError

            assertEquals("code" to "Kode salah. Sisa 4 kali percobaan.", wrong.field to wrong.message)
            assertEquals("form" to "Akunmu diblokir karena kecurangan.", blocked.field to blocked.message)
            assertNull(session.token)
        }

    @Test
    fun noConnectionSaysSoInTheAppsWords() {
        val error = IOException("Unable to resolve host").toAuthFlowError()

        assertEquals("Koneksi terputus. Periksa internetmu lalu coba lagi.", error.message)
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
