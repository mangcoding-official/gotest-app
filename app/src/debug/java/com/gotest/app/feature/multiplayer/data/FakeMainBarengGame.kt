package com.gotest.app.feature.multiplayer.data

import com.gotest.app.feature.multiplayer.domain.GameOrganization
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.ResultItem
import com.gotest.app.feature.multiplayer.domain.ResultRow
import com.gotest.app.feature.practice.data.FakePracticeQuestions
import com.gotest.app.feature.practice.domain.AnswerData
import java.time.Instant

/** One player of a fake game; the others finish on their own with a fixed score. */
internal class FakePlayer(
    val id: Long,
    val name: String,
    val joinedAt: Long,
    /** Others only: when they finish, after the start, and how many they get right. */
    val finishAfterMs: Long = 0,
    val correctShare: Double = 0.0,
) {
    var finishedAt: Long? = null
    var left = false
    val answers = mutableMapOf<Long, AnswerData>()
}

/**
 * One game of the fake, moved on by the clock on every read: players join, another host starts, players finish,
 * the time runs out, the waiting room expires, and another host makes the next game (MainBarengService rules).
 */
@Suppress("TooManyFunctions")
internal class FakeGame(
    val code: String,
    val settings: GameSettings,
    val organization: GameOrganization?,
    val hostId: Long,
    createdAt: Long,
) {
    val players = mutableListOf<FakePlayer>()
    var status = GameState.STATUS_WAITING
    var cancelReason: String? = null
    var startedAt: Long? = null
    var endsAt: Long? = null
    var closedAt: Long? = null
    var nextGameCode: String? = null
    val lobbyExpiresAt = createdAt + LOBBY_MS

    /** Others still to come, with the time they join. */
    val pendingJoins = mutableListOf<FakePlayer>()

    /** Another host presses Mulai at this time; the next game appears this long after the end. */
    var hostStartsAt: Long? = null
    var nextGameAfterMs: Long? = null

    val maxPlayers: Int get() = if (settings.mode == GameState.MODE_DUEL) 2 else ROOM_MAX
    val inGame: List<FakePlayer> get() = players.filterNot { it.left && status == GameState.STATUS_WAITING }

    val questions: List<Pair<GameQuestion, AnswerData>> by lazy {
        val all = FakePracticeQuestions.all
        List(settings.questionCount) { index ->
            val source = all[index % all.size]
            GameQuestion(
                id = code.hashCode().toLong().and(QUESTION_MASK) * QUESTION_SPACE + index + 1,
                order = index + 1,
                questionType = source.type,
                content = source.content,
            ) to source.correct
        }
    }

    fun player(id: Long): FakePlayer? = players.firstOrNull { it.id == id }

    /** Moves the game on to [now]; returns true when it just ended for the first time. */
    fun advance(now: Long): Boolean {
        when (status) {
            GameState.STATUS_WAITING -> advanceLobby(now)
            GameState.STATUS_RUNNING -> return advanceRunning(now)
        }
        return false
    }

    private fun advanceLobby(now: Long) {
        pendingJoins.filter { it.joinedAt <= now }.forEach { joining ->
            pendingJoins.remove(joining)
            if (players.size < maxPlayers) players += joining
        }
        val hostStart = hostStartsAt
        when {
            now >= lobbyExpiresAt -> cancel(GameState.CANCEL_EXPIRED, lobbyExpiresAt)
            hostStart != null && now >= hostStart && players.count { !it.left } >= 2 -> start(hostStart)
        }
    }

    private fun advanceRunning(now: Long): Boolean {
        val start = startedAt ?: return false
        val end = endsAt ?: return false
        players.filter { it.id != FakeMainBarengData.ME_ID && it.finishedAt == null }.forEach { other ->
            if (start + other.finishAfterMs <= minOf(now, end)) other.finishedAt = start + other.finishAfterMs
        }
        if (now >= end) players.filter { it.finishedAt == null }.forEach { it.finishedAt = end }
        if (players.all { it.finishedAt != null }) {
            status = GameState.STATUS_FINISHED
            closedAt = players.maxOf { it.finishedAt ?: start }
            return true
        }
        return false
    }

    fun start(at: Long) {
        status = GameState.STATUS_RUNNING
        startedAt = at + COUNTDOWN_MS
        endsAt = at + COUNTDOWN_MS + settings.durationMinutes * MINUTE_MS
        pendingJoins.clear()
    }

    fun cancel(
        reason: String,
        at: Long,
    ) {
        status = GameState.STATUS_CANCELLED
        cancelReason = reason
        closedAt = at
    }

    fun state(
        viewer: Long,
        now: Long,
        subjectName: String,
        categoryName: String,
        topicNames: List<String>,
    ): GameState {
        val shown = if (status == GameState.STATUS_WAITING) players.filterNot { it.left } else players
        return GameState(
            code = code,
            mode = settings.mode,
            status = status,
            cancelReason = cancelReason,
            organization = organization,
            categoryName = categoryName,
            subjectName = subjectName,
            topicNames = topicNames,
            questionCount = settings.questionCount,
            durationMinutes = settings.durationMinutes,
            maxPlayers = maxPlayers,
            hostName = player(hostId)?.name.orEmpty(),
            players =
                shown.map { player ->
                    GamePlayer(
                        id = player.id,
                        name = player.name,
                        isHost = player.id == hostId,
                        isYou = player.id == viewer,
                        finished = player.finishedAt != null,
                        joinedAt = iso(player.joinedAt),
                        finishedAt = player.finishedAt?.let(::iso),
                    )
                },
            youAreHost = hostId == viewer,
            youFinished = player(viewer)?.finishedAt != null,
            lobbyExpiresAt = iso(lobbyExpiresAt),
            startedAt = startedAt?.let(::iso),
            endsAt = endsAt?.let(::iso),
            closedAt = closedAt?.let(::iso),
            serverNow = iso(now),
            nextGameCode = nextGameCode,
        )
    }

    /** Most correct first, then the faster total time; players with the same pair share a rank. */
    fun result(viewer: Long): GameResult {
        val rows =
            players.map { player ->
                val (correct, wrong) = score(player)
                ResultRow(
                    rank = 0,
                    playerId = player.id,
                    name = player.name,
                    isYou = player.id == viewer,
                    correct = correct,
                    wrong = wrong,
                    empty = settings.questionCount - correct - wrong,
                    durationSeconds = (((player.finishedAt ?: endsAt ?: 0L) - (startedAt ?: 0L)) / SECOND_MS).toInt(),
                )
            }
        val ranked =
            rows
                .map { row -> row.copy(rank = 1 + rows.count { it.beats(row) }) }
                .sortedWith(compareBy<ResultRow> { it.rank }.thenBy { it.name })
        val you = ranked.firstOrNull { it.isYou }
        val rival = ranked.firstOrNull { !it.isYou }
        val outcome =
            when {
                settings.mode != GameState.MODE_DUEL || you == null || rival == null || ranked.size != 2 -> null
                you.rank < rival.rank -> GameResult.WIN
                you.rank > rival.rank -> GameResult.LOSE
                else -> GameResult.DRAW
            }
        return GameResult(rows = ranked, outcome = outcome, items = items(player(viewer)))
    }

    private fun ResultRow.beats(other: ResultRow) =
        correct > other.correct || (correct == other.correct && durationSeconds < other.durationSeconds)

    private fun score(player: FakePlayer): Pair<Int, Int> {
        if (player.id != FakeMainBarengData.ME_ID) {
            val correct = (settings.questionCount * player.correctShare).toInt()
            return correct to (settings.questionCount - correct) / 2
        }
        val statuses = items(player)
        return statuses.count { it.status == GameResult.CORRECT } to statuses.count { it.status == GameResult.WRONG }
    }

    private fun items(player: FakePlayer?): List<ResultItem> =
        questions.map { (question, key) ->
            val answer = player?.answers?.get(question.id)
            val status =
                when {
                    answer == null || answer.isEmpty() -> "empty"
                    answer == key -> GameResult.CORRECT
                    else -> GameResult.WRONG
                }
            ResultItem(question.order, status)
        }

    private fun AnswerData.isEmpty() =
        selectedOption == null && selectedOptions.isNullOrEmpty() && statementAnswers.orEmpty().all { it == null }

    companion object {
        const val LOBBY_MS = 5 * 60_000L
        const val COUNTDOWN_MS = 5_000L
        const val MINUTE_MS = 60_000L
        const val SECOND_MS = 1_000L
        const val ROOM_MAX = 10
        private const val QUESTION_MASK = 0xFFFFL
        private const val QUESTION_SPACE = 1_000L

        fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()
    }
}
