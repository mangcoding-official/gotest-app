package com.gotest.app.feature.tryout.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerRequest
import com.gotest.app.feature.tryout.domain.FinishSectionRequest
import com.gotest.app.feature.tryout.domain.FlagRequest
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.TryoutAttempt
import com.gotest.app.feature.tryout.domain.TryoutCatalogPage
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutResult
import com.gotest.app.feature.tryout.domain.TryoutSessionPage
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions") // One function per route of the contract.
@Singleton
class ApiTryoutRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : TryoutRepository {
        private val api = retrofit.create(TryoutApi::class.java)

        override suspend fun catalog(): Result<TryoutCatalogPage> = apiCall(json) { api.catalog() }

        override suspend fun start(packageId: Long): Result<TryoutAttempt> =
            apiCall(json) { api.start(packageId).attempt }

        override suspend fun attempt(attemptId: Long): Result<TryoutAttempt> =
            apiCall(json) { api.attempt(attemptId).attempt }

        override suspend fun begin(attemptId: Long): Result<TryoutAttempt> =
            apiCall(json) { api.begin(attemptId).attempt }

        override suspend fun answer(
            attemptId: Long,
            itemId: Long,
            answer: AnswerData,
        ): Result<Unit> = apiCall(json) { api.answer(attemptId, itemId, AnswerRequest(answer)) }

        override suspend fun flag(
            attemptId: Long,
            itemId: Long,
            flagged: Boolean,
        ): Result<Unit> = apiCall(json) { api.flag(attemptId, itemId, FlagRequest(flagged)) }

        override suspend fun finishSection(
            attemptId: Long,
            sectionId: Long?,
        ): Result<TryoutAttempt> =
            apiCall(json) { api.finishSection(attemptId, FinishSectionRequest(sectionId)).attempt }

        override suspend fun submit(attemptId: Long): Result<TryoutAttempt> =
            apiCall(json) { api.submit(attemptId).attempt }

        override suspend fun result(attemptId: Long): Result<TryoutResult> =
            apiCall(json) { api.result(attemptId).result }

        override suspend fun session(sessionId: Long): Result<TryoutSessionPage> =
            apiCall(json) { api.session(sessionId) }

        override suspend fun register(sessionId: Long): Result<String?> =
            apiCall(json) { api.register(sessionId).message }

        override suspend fun unregister(sessionId: Long): Result<String?> =
            apiCall(json) { api.unregister(sessionId).message }

        override suspend fun startSession(sessionId: Long): Result<Long> =
            apiCall(json) {
                api.startSession(sessionId).attempt.id
            }

        override suspend fun sessionLeaderboard(sessionId: Long): Result<SessionLeaderboard> =
            apiCall(json) { api.sessionLeaderboard(sessionId) }
    }
