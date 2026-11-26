package com.gotest.app.core.network

import com.gotest.app.core.session.SessionStore
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Headers on every request (API preflight 4.1): `Accept: application/json`, `X-App-Version` with the
 * version code, and `Authorization: Bearer …` while signed in, only to the Gotest API itself ([apiBaseUrl]:
 * the same scheme, host, and port), never to another address (T-026 required 6).
 */
class ApiHeadersInterceptor(
    private val session: SessionStore,
    private val appVersionCode: Int,
    private val apiBaseUrl: HttpUrl,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val ownServer =
            original.url.scheme == apiBaseUrl.scheme &&
                original.url.host == apiBaseUrl.host &&
                original.url.port == apiBaseUrl.port
        val request =
            original
                .newBuilder()
                .header("Accept", "application/json")
                .header("X-App-Version", appVersionCode.toString())
                .apply { if (ownServer) session.token?.let { header("Authorization", "Bearer $it") } }
                .build()
        return chain.proceed(request)
    }
}

/**
 * Session-wide answers: a 401 on a request that carried the current token signs out (back to Masuk), a 403
 * `guardian_consent` opens the waiting page of an account the parent has not approved (API-K6 B), a 403
 * `email_verification_pending` the waiting page of an account whose email is not proven (DEC-154), and a 426
 * asks to update the app. Other errors are left to the screen that made the request.
 */
class SessionGuardInterceptor(
    private val session: SessionStore,
    private val signals: SessionSignals,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val sentToken = chain.request().header("Authorization")?.removePrefix("Bearer ")
        val signedIn = sentToken != null
        when (response.code) {
            ApiError.UNAUTHORIZED -> {
                // A 401 for a token already replaced (signed in again meanwhile) leaves the new session alone.
                if (signedIn && sentToken == session.token) {
                    session.clear()
                    signals.markExpired()
                }
            }

            ApiError.FORBIDDEN -> {
                when (if (signedIn) response.errorCode() else null) {
                    ApiError.GUARDIAN_CONSENT -> signals.markAwaitingConsent()
                    ApiError.EMAIL_VERIFICATION_PENDING -> signals.markAwaitingEmailVerification()
                }
            }

            ApiError.UPDATE_REQUIRED -> {
                signals.markUpdateRequired()
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
