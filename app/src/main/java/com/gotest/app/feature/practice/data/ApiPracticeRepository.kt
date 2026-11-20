package com.gotest.app.feature.practice.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerRequest
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.PracticeResult
import com.gotest.app.feature.practice.domain.StartAccess
import com.gotest.app.feature.practice.domain.StartRequest
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiPracticeRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : PracticeRepository {
        private val api = retrofit.create(PracticeApi::class.java)

        override suspend fun catalog(): Result<PracticeCatalogPage> = apiCall(json) { api.catalog() }

        override suspend fun startAccess(): Result<StartAccess> = apiCall(json) { api.account() }

        override suspend fun start(
            packageId: Long,
            timerEnabled: Boolean?,
        ): Result<PracticeAttempt> = apiCall(json) { api.start(packageId, StartRequest(timerEnabled)).attempt }

        override suspend fun startInRuang(
            code: String,
            packageId: Long,
        ): Result<PracticeAttempt> = apiCall(json) { api.startInRuang(code, packageId).attempt }

        override suspend fun attempt(attemptId: Long): Result<PracticeAttempt> =
            apiCall(json) { api.attempt(attemptId).attempt }

        override suspend fun activate(
            attemptId: Long,
            itemId: Long,
        ): Result<ActiveQuestion> = apiCall(json) { api.activate(attemptId, itemId).question }

        override suspend fun answer(
            attemptId: Long,
            questionId: Long,
            answer: AnswerData,
        ): Result<Unit> = apiCall(json) { api.answer(attemptId, questionId, AnswerRequest(answer)) }

        override suspend fun submit(attemptId: Long): Result<PracticeAttempt> =
            apiCall(json) { api.submit(attemptId).attempt }

        override suspend fun result(attemptId: Long): Result<PracticeResult> =
            apiCall(json) { api.result(attemptId).result }
    }
