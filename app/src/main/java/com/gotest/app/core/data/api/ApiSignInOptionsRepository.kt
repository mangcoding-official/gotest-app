package com.gotest.app.core.data.api

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.auth.domain.SignInOptions
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sign-in options from `GET /referensi`, read once per app run; a failed read is tried again the next
 * time a sign-in screen opens.
 */
@Singleton
class ApiSignInOptionsRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : SignInOptionsRepository {
        private val api = retrofit.create(AuthApi::class.java)
        private val lock = Mutex()
        private var cached: SignInOptions? = null

        override suspend fun options(): SignInOptions =
            lock.withLock {
                cached ?: apiCall(json) { api.reference() }
                    .map { it.toOptions() }
                    .onSuccess { cached = it }
                    .getOrDefault(SignInOptions())
            }

        companion object {
            /**
             * Google sign-in needs the app's Google client ID, which the app does not have yet; until then the
             * button stays hidden even when the server offers Google, so it never shows a button that does nothing.
             */
            const val GOOGLE_CLIENT_READY = false
        }
    }

internal fun Reference.toOptions(): SignInOptions =
    SignInOptions(
        phone = phoneSignIn,
        google = googleSignIn && ApiSignInOptionsRepository.GOOGLE_CLIENT_READY,
        emailCode = emailCode,
        passwordResetWhatsApp = passwordResetWhatsApp,
    )
