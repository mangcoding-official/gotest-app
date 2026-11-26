package com.gotest.app.core.data.api

import com.gotest.app.core.network.NetworkModule
import com.gotest.app.feature.auth.domain.SignInOptions
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** T-026 required 12: the Nomor HP tab, Google, and "Kode lewat email" follow `GET /referensi`. */
class ApiSignInOptionsTest {
    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private lateinit var repository: ApiSignInOptionsRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule
                .provideRetrofit(OkHttpClient(), json)
                .newBuilder()
                .baseUrl(server.url("/"))
                .build()
        repository = ApiSignInOptionsRepository(retrofit, json)
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

    private fun reference(
        phone: Boolean,
        google: Boolean,
        emailCode: Boolean,
        resetWhatsApp: Boolean = false,
    ) = """
        {"education_levels":[{"value":"sma","label":"SMA"}],"trial_period":null,
        "phone_sign_in":$phone,"google_sign_in":$google,"email_code":$emailCode,
        "password_reset_whatsapp":$resetWhatsApp,"min_app_version":null}
        """.trimIndent()

    @Test
    fun theOptionsFollowTheServerAndAreReadOnce() =
        runTest {
            reply(200, reference(phone = true, google = false, emailCode = true))

            val first = repository.options()
            val second = repository.options()

            assertEquals(SignInOptions(phone = true, google = false, emailCode = true), first)
            assertEquals(first, second)
            assertEquals("/api/v1/referensi", server.takeRequest().url.encodedPath)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun googleShowsOnceTheServerOffersIt() =
        runTest {
            reply(200, reference(phone = false, google = true, emailCode = false))

            assertEquals(SignInOptions(google = true), repository.options())
        }

    @Test
    fun lupaSandiByWhatsAppFollowsTheServer() =
        runTest {
            reply(200, reference(phone = false, google = false, emailCode = false, resetWhatsApp = true))

            assertEquals(SignInOptions(passwordResetWhatsApp = true), repository.options())
        }

    @Test
    fun aFailedReadShowsOnlyEmailAndPasswordAndIsTriedAgain() =
        runTest {
            reply(503, """{"message":"Gotest sedang dalam perbaikan.","code":"unavailable"}""")
            reply(200, reference(phone = true, google = false, emailCode = false))

            assertEquals(SignInOptions(), repository.options())
            assertEquals(SignInOptions(phone = true), repository.options())
            assertEquals(2, server.requestCount)
        }
}
