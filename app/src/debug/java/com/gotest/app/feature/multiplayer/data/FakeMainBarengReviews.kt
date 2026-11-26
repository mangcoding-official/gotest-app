package com.gotest.app.feature.multiplayer.data

import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameOrganization
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameReview
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.ResultItem
import com.gotest.app.feature.multiplayer.domain.ResultRow

/** "Lihat Hasil" for the games already in the fake history (not the ones played in this session). */
internal object FakeMainBarengReviews {
    private const val BASE_SECONDS = 420
    private const val STEP_SECONDS = 15
    private const val WRONG = 2
    private const val QUESTIONS = 10

    fun of(
        code: String,
        history: List<GameHistoryItem>,
    ): GameReview? {
        // Only the seeded games (codes HX… and SX…); a game played in this session has its own result.
        val seeded = code.startsWith("HX") || code.startsWith("SX")
        val item = history.firstOrNull { it.code == code && it.finished }?.takeIf { seeded } ?: return null
        val rows = rows(item)
        val you = rows.first { it.isYou }
        val game =
            GameState(
                code = item.code,
                mode = item.mode,
                status = GameState.STATUS_FINISHED,
                organization =
                    if (item.code.startsWith("SX")) {
                        GameOrganization(FakeMainBarengData.SCHOOL_NAME, FakeMainBarengData.SCHOOL_CODE)
                    } else {
                        null
                    },
                categoryName = "UTBK-SNBT",
                subjectName = item.subjectName,
                questionCount = item.questionCount,
                durationMinutes = item.durationMinutes,
                maxPlayers = if (item.mode == GameState.MODE_DUEL) 2 else FakeGame.ROOM_MAX,
                // Someone else hosted the seeded games, so "Gabung Lagi" waits for them.
                hostName = rows.first { !it.isYou }.name,
                players = rows.map { GamePlayer(it.playerId, it.name, isYou = it.isYou, finished = true) },
                youAreHost = false,
                youFinished = true,
                closedAt = item.closedAt,
                serverNow = item.closedAt,
            )
        val items =
            List(item.questionCount) { index ->
                val status =
                    when {
                        index < you.correct -> GameResult.CORRECT
                        index < you.correct + you.wrong -> GameResult.WRONG
                        else -> "empty"
                    }
                ResultItem(index + 1, status)
            }
        return GameReview(game, GameResult(rows, item.outcome, items))
    }

    /** Rows that agree with the history line: the outcome of a duel, or the viewer's rank in a room. */
    private fun rows(item: GameHistoryItem): List<ResultRow> {
        val others = FakeMainBarengData.others
        if (item.mode == GameState.MODE_DUEL) {
            val rival = others.first { it.second == item.opponentName }
            val (mine, theirs) =
                when (item.outcome) {
                    GameResult.WIN -> 8 to 6
                    GameResult.LOSE -> 5 to 7
                    else -> 6 to 6
                }
            val me = row(1, FakeMainBarengData.ME_ID, FakeMainBarengData.ME, true, mine)
            val them = row(2, rival.first, rival.second, false, theirs)
            return when (item.outcome) {
                GameResult.WIN -> listOf(me, them.copy(rank = 2))
                GameResult.LOSE -> listOf(them.copy(rank = 1), me.copy(rank = 2))
                else -> listOf(me, them.copy(rank = 1))
            }
        }
        val rank = item.rank ?: 1
        return List(item.playerCount) { index ->
            val position = index + 1
            val correct = item.questionCount - position
            if (position == rank) {
                row(position, FakeMainBarengData.ME_ID, FakeMainBarengData.ME, true, correct)
            } else {
                val other = others[(index - if (position > rank) 1 else 0) % others.size]
                row(position, other.first + index * 10L, other.second, false, correct)
            }
        }
    }

    private fun row(
        rank: Int,
        id: Long,
        name: String,
        isYou: Boolean,
        correct: Int,
    ) = ResultRow(
        rank = rank,
        playerId = id,
        name = name,
        isYou = isYou,
        correct = correct,
        wrong = WRONG,
        empty = (QUESTIONS - correct - WRONG).coerceAtLeast(0),
        durationSeconds = BASE_SECONDS + rank * STEP_SECONDS,
    )
}
