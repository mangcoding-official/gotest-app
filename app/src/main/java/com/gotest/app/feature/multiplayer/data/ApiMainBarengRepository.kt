package com.gotest.app.feature.multiplayer.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameReview
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerRequest
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions") // One function per route of the contract.
@Singleton
class ApiMainBarengRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : MainBarengRepository {
        private val api = retrofit.create(MainBarengApi::class.java)

        override suspend fun hub(space: String?): Result<HubPage> =
            apiCall(json) { if (space == null) api.hub() else api.ruangHub(space) }

        override suspend fun active(): Result<GameState?> = apiCall(json) { api.active().game }

        override suspend fun history(space: String?): Result<List<GameHistoryItem>> =
            apiCall(json) { (if (space == null) api.history() else api.ruangHistory(space)).games }

        override suspend fun create(
            space: String?,
            settings: GameSettings,
        ): Result<GameState> =
            apiCall(json) { (if (space == null) api.create(settings) else api.ruangCreate(space, settings)).game }

        override suspend fun lookup(code: String): Result<GameState> = apiCall(json) { api.lookup(code).game }

        override suspend fun join(code: String): Result<GameState> = apiCall(json) { api.join(code).game }

        override suspend fun state(code: String): Result<GameState> = apiCall(json) { api.state(code).game }

        override suspend fun start(code: String): Result<GameState> = apiCall(json) { api.start(code).game }

        override suspend fun cancel(code: String): Result<GameState> = apiCall(json) { api.cancel(code).game }

        override suspend fun leave(code: String): Result<Unit> = apiCall(json) { api.leave(code) }

        override suspend fun questions(code: String): Result<List<GameQuestion>> =
            apiCall(json) { api.questions(code).questions }

        override suspend fun answer(
            code: String,
            itemId: Long,
            answer: AnswerData,
        ): Result<Unit> = apiCall(json) { api.answer(code, itemId, AnswerRequest(answer)) }

        override suspend fun finish(code: String): Result<GameState> = apiCall(json) { api.finish(code).game }

        override suspend fun result(code: String): Result<GameReview> = apiCall(json) { api.result(code) }

        override suspend fun playAgain(code: String): Result<GameState> = apiCall(json) { api.playAgain(code).game }
    }
