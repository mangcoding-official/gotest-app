package com.gotest.app.feature.multiplayer.domain

import com.gotest.app.feature.practice.domain.AnswerData

/**
 * Main Bareng (API module E, web GameClient). [space] is the code of the Mitra whose space the hub is in, or null
 * for Gotest.id; creating and the history use it, the steps of a game are the same everywhere. A refusal is an
 * [com.gotest.app.core.network.ApiError] whose `code` is the web's `kind` (`full`, `busy`, `not_member`, …).
 */
@Suppress("TooManyFunctions") // One function per route of the contract.
interface MainBarengRepository {
    suspend fun hub(space: String?): Result<HubPage>

    /** The game the viewer is in right now (waiting or running), in any space. */
    suspend fun active(): Result<GameState?>

    suspend fun history(space: String?): Result<List<GameHistoryItem>>

    suspend fun create(
        space: String?,
        settings: GameSettings,
    ): Result<GameState>

    suspend fun lookup(code: String): Result<GameState>

    suspend fun join(code: String): Result<GameState>

    suspend fun state(code: String): Result<GameState>

    suspend fun start(code: String): Result<GameState>

    suspend fun cancel(code: String): Result<GameState>

    suspend fun leave(code: String): Result<Unit>

    suspend fun questions(code: String): Result<List<GameQuestion>>

    suspend fun answer(
        code: String,
        itemId: Long,
        answer: AnswerData,
    ): Result<Unit>

    suspend fun finish(code: String): Result<GameState>

    /** The result with its game: after playing, and for "Lihat Hasil" in the history. */
    suspend fun result(code: String): Result<GameReview>

    /** "Main Lagi" (host) and "Gabung Lagi" (the others): the next game with the same settings. */
    suspend fun playAgain(code: String): Result<GameState>
}
