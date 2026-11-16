package com.gotest.app.core.network

import com.gotest.app.core.session.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Headers on every request (API preflight 4.1): `Accept: application/json`, `X-App-Version` with the
 * version code, and `Authorization: Bearer …` while signed in.
 */
class ApiHeadersInterceptor(
    private val session: SessionStore,
    private val appVersionCode: Int,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("Accept", "application/json")
                .header("X-App-Version", appVersionCode.toString())
                .apply { session.token?.let { header("Authorization", "Bearer $it") } }
                .build()
        return chain.proceed(request)
    }
}

/**
 * Session-wide answers: a 401 on a request that carried a token signs out (back to Masuk), a 403
 * `guardian_consent` opens the waiting page of an account the parent has not approved (API-K6 B), and
 * a 426 asks to update the app. Other errors are left to the screen that made the request.
 */
class SessionGuardInterceptor(
    private val session: SessionStore,
    private val events: SessionEvents,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val signedIn = chain.request().header("Authorization") != null
        when (response.code) {
            ApiError.UNAUTHORIZED -> {
                if (signedIn) {
                    session.clear()
                    events.emit(SessionEvent.SignedOut)
                }
            }

            ApiError.FORBIDDEN -> {
                if (signedIn &&
                    response.errorCode() == ApiError.GUARDIAN_CONSENT
                ) {
                    events.emit(SessionEvent.AwaitingConsent)
                }
            }

            ApiError.UPDATE_REQUIRED -> {
                events.emit(SessionEvent.UpdateRequired)
            }
        }
        return response
    }

    /** The `code` of the shared error body, read without consuming it for the screen. */
    private fun Response.errorCode(): String? =
        ApiError.from(code, peekBody(MAX_ERROR_BYTES).string(), json, fallbackMessage = "").code

    private companion object {
        const val MAX_ERROR_BYTES = 16_384L
        val json = Json { ignoreUnknownKeys = true }
    }
}
