package com.gotest.app.feature.multiplayer.domain

import com.gotest.app.core.model.QuestionAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Main Bareng games. Bound to [com.gotest.app.feature.multiplayer.data.FakeMultiplayerRepository]
 * until the realtime API exists.
 */
interface MultiplayerRepository {
    /** False while answers cannot reach the server. */
    val connection: Flow<Boolean>

    /** Finished and cancelled games of the current user, newest first. */
    fun observeHistory(): Flow<List<GameHistoryItem>>

    /** The game the current user created or joined and has not left yet, if any. */
    fun observeActiveGame(): Flow<Game?>

    suspend fun findGame(code: String): JoinLookup

    suspend fun joinGame(code: String): Result<Unit>

    /** Categories, subtests and topics with their question stock, for "Buat permainan". */
    fun observeCatalog(): Flow<List<GameCategory>>

    /** Creates a game hosted by the current user and returns its code. */
    suspend fun createGame(settings: GameSettings): Result<String>

    /** Live lobby and game state; players join, start, finish and cancel over time. */
    fun observeGame(code: String): Flow<Game>

    suspend fun startGame(code: String): Result<Unit>

    suspend fun cancelGame(code: String)

    suspend fun leaveGame(code: String)

    suspend fun loadSession(code: String): GameSession

    suspend fun saveAnswer(
        code: String,
        questionId: String,
        answer: QuestionAnswer,
    )

    /** Sends the current user's answers; they cannot be changed afterwards. */
    suspend fun submitAnswers(code: String)

    fun observeResult(code: String): Flow<GameResult>
}

/** "Buat Ulang" / "Main Lagi": a new game hosted by the current user with the settings of [code]. */
suspend fun MultiplayerRepository.recreateGame(code: String): Result<String> {
    val settings = observeGame(code).first().settings
    return createGame(settings)
}
