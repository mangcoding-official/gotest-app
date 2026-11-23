package com.gotest.app.feature.multiplayer

import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameReview
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.HubAccess
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import com.gotest.app.feature.multiplayer.domain.OptionCategory
import com.gotest.app.feature.multiplayer.domain.OptionSubject
import com.gotest.app.feature.practice.domain.AnswerData
import java.io.IOException

/** Answers what the test sets; every call that changes something is recorded. */
@Suppress("TooManyFunctions")
internal class ScriptedMainBareng : MainBarengRepository {
    var hubReply: Result<HubPage> = Result.success(HubPage(HubAccess("active"), OPTIONS))
    var activeReply: Result<GameState?> = Result.success(null)
    var historyReply: Result<List<GameHistoryItem>> = Result.success(emptyList())
    var createReply: Result<GameState> = offline()
    var lookupReply: Result<GameState> = offline()
    var joinReply: Result<GameState> = offline()
    var stateReply: Result<GameState> = offline()
    var startReply: Result<GameState> = offline()
    var cancelReply: Result<GameState> = offline()
    var leaveReply: Result<Unit> = Result.success(Unit)
    var questionsReply: Result<List<GameQuestion>> = offline()
    var answerReply: (Long) -> Result<Unit> = { Result.success(Unit) }
    var finishReply: Result<GameState> = offline()
    var resultReply: Result<GameReview> = offline()
    var playAgainReply: Result<GameState> = offline()

    val created = mutableListOf<Pair<String?, GameSettings>>()
    val lookups = mutableListOf<String>()
    val answers = mutableListOf<Pair<Long, AnswerData>>()
    var stateCalls = 0
    var finishCalls = 0

    override suspend fun hub(space: String?) = hubReply

    override suspend fun active() = activeReply

    override suspend fun history(space: String?) = historyReply

    override suspend fun create(
        space: String?,
        settings: GameSettings,
    ): Result<GameState> {
        created += space to settings
        return createReply
    }

    override suspend fun lookup(code: String): Result<GameState> {
        lookups += code
        return lookupReply
    }

    override suspend fun join(code: String) = joinReply

    override suspend fun state(code: String): Result<GameState> {
        stateCalls++
        return stateReply
    }

    override suspend fun start(code: String) = startReply

    override suspend fun cancel(code: String) = cancelReply

    override suspend fun leave(code: String) = leaveReply

    override suspend fun questions(code: String) = questionsReply

    override suspend fun answer(
        code: String,
        itemId: Long,
        answer: AnswerData,
    ): Result<Unit> {
        answers += itemId to answer
        return answerReply(itemId)
    }

    override suspend fun finish(code: String): Result<GameState> {
        finishCalls++
        return finishReply
    }

    override suspend fun result(code: String) = resultReply

    override suspend fun playAgain(code: String) = playAgainReply

    companion object {
        const val NOW = "2026-11-23T03:00:00Z"
        val OPTIONS = listOf(OptionCategory(2, "UTBK-SNBT", listOf(OptionSubject(11, "Penalaran Umum", 30))))
        val ME = GamePlayer(1, "Gilang Permana", isHost = true, isYou = true)
        val RIVAL = GamePlayer(2, "Rina Ayu")

        fun <T> offline(): Result<T> = Result.failure(IOException("offline"))

        fun game(
            status: String = GameState.STATUS_WAITING,
            players: List<GamePlayer> = listOf(ME),
            host: Boolean = true,
            organization: com.gotest.app.feature.multiplayer.domain.GameOrganization? = null,
            youFinished: Boolean = false,
            nextGameCode: String? = null,
        ) = GameState(
            code = "ABC123",
            status = status,
            organization = organization,
            subjectName = "Penalaran Umum",
            questionCount = 2,
            durationMinutes = 10,
            hostName = if (host) ME.name else RIVAL.name,
            players = players,
            youAreHost = host,
            youFinished = youFinished,
            lobbyExpiresAt = "2026-11-23T03:05:00Z",
            startedAt = "2026-11-23T03:00:05Z",
            endsAt = "2026-11-23T03:10:05Z",
            serverNow = NOW,
            nextGameCode = nextGameCode,
        )

        fun review(
            game: GameState = game(GameState.STATUS_FINISHED),
            outcome: String? = GameResult.WIN,
        ) = GameReview(game, GameResult(outcome = outcome))
    }
}
