package com.gotest.app.core.data.api

import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.apiCall
import com.gotest.app.core.session.SessionStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Masuk with email and password, Lupa sandi, and Keluar against the API (module A). A refusal is an ApiError
 * with the web's sentence: 401 `account_blocked` carries the reason the account was blocked (T-026 required 2).
 */
@Singleton
class ApiAuthRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
        private val session: SessionStore,
        private val acceptor: SignInAcceptor,
    ) : AuthRepository {
        private val api = retrofit.create(AuthApi::class.java)

        override val isSignedIn: StateFlow<Boolean> = session.isSignedIn

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<Unit> =
            apiCall(json) { api.signIn(SignInRequest(email, password, acceptor.deviceName)) }
                .map { reply -> acceptor.accept(reply) }
                .map { }

        /** Signing up goes through SignUpRepository (`auth/daftar`), never here. */
        override suspend fun register(
            fullName: String,
            email: String,
            password: String,
        ): Result<Unit> = Result.failure(UnsupportedOperationException("register"))

        /** The server's sentence, the same whether the account exists or not. */
        override suspend fun requestPasswordReset(email: String): Result<String> =
            apiCall(json) { api.forgotPassword(EmailRequest(email)) }.map { it.message.orEmpty() }

        /** Keluar removes this phone's token on the server; the app forgets it even when the server is out of reach. */
        override suspend fun signOut() {
            apiCall(json) { api.signOut() }
            session.clear()
        }
    }
