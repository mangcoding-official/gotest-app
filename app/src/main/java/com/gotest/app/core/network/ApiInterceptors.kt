package com.gotest.app.core.network

import com.gotest.app.core.session.SessionStore
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
 * Session-wide answers: a 401 on a request that carried a token signs out (back to Masuk), a 426
 * asks to update the app. Other errors are left to the screen that made the request.
 */
class SessionGuardInterceptor(
    private val session: SessionStore,
    private val events: SessionEvents,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        when (response.code) {
            ApiError.UNAUTHORIZED -> {
                if (chain.request().header("Authorization") != null) {
                    session.clear()
                    events.emit(SessionEvent.SignedOut)
                }
            }

            ApiError.UPDATE_REQUIRED -> {
                events.emit(SessionEvent.UpdateRequired)
            }
        }
        return response
    }
}
