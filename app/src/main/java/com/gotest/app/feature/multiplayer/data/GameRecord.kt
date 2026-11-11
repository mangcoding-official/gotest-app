package com.gotest.app.feature.multiplayer.data

import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.isAnsweredCorrectly
import com.gotest.app.feature.multiplayer.domain.CancelReason
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.PlayerStanding
import com.gotest.app.feature.multiplayer.domain.PlayerState
import com.gotest.app.feature.multiplayer.domain.QuestionOutcome
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** A player's seat. A [joinedAt] in the future is a friend who has not arrived yet. */
internal data class Seat(
    val profile: PlayerProfile,
    val joinedAt: Long,
    val finishedAt: Long? = null,
) {
    val id: String get() = profile.id
}

/** What happens when the student joins a seeded lobby. */
internal data class JoinScript(
    /** Seat the student takes over. */
    val seatId: String,
    val autoCancelInMillis: Long,
    /** The (simulated) host presses "Mulai" this long after the student joins. */
    val hostStartsInMillis: Long,
)

/**
 * Stored state of one game. Everything time-based (friends arriving, the host starting, the
 * auto-cancel, players finishing) is a timestamp, and [snapshot] derives what is true "now".
 */
internal data class GameRecord(
    val code: String,
    val settings: GameSettings,
    val hostId: String,
    val seats: List<Seat>,
    val createdAt: Long,
    val autoCancelAt: Long?,
    val startedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancelReason: CancelReason? = null,
    /** Seat id of the signed-in student; null when they are not in this game. */
    val currentUserId: String? = null,
    val playedAt: LocalDateTime? = null,
    /** Seeded games that never change status. */
    val fixedStatus: GameStatus? = null,
    val seededResult: GameResult? = null,
    val joinScript: JoinScript? = null,
) {
    val endsAt: Long? get() = startedAt?.let { it + settings.durationMinutes * GameRules.MILLIS_PER_MINUTE }

    fun seatsAt(now: Long): List<Seat> = seats.filter { it.joinedAt <= now }

    fun statusAt(now: Long): GameStatus {
        fixedStatus?.let { return it }
        val start = startedAt
        val end = endsAt
        return when {
            cancelledAt != null -> {
                GameStatus.Cancelled
            }

            start != null && end != null && start <= now -> {
                val allDone = seatsAt(now).all { seat -> seat.finishedAt?.let { it <= now } == true }
                if (allDone || now >= end) GameStatus.Finished else GameStatus.Started
            }

            autoCancelAt != null && now >= autoCancelAt -> {
                GameStatus.Cancelled
            }

            else -> {
                GameStatus.Waiting
            }
        }
    }

    fun snapshot(now: Long): Game {
        val status = statusAt(now)
        val reason =
            when {
                status != GameStatus.Cancelled -> null
                cancelledAt != null -> cancelReason
                fixedStatus == null -> CancelReason.AutoCancel
                else -> null
            }
        return Game(
            code = code,
            settings = settings,
            status = status,
            players = seatsAt(now).map { it.toPlayer(status, now) },
            createdAtMillis = createdAt,
            autoCancelAtMillis = autoCancelAt,
            startedAtMillis = startedAt?.takeIf { it <= now },
            cancelReason = reason,
            cancelledAtMillis = if (status == GameStatus.Cancelled) cancelledAt ?: autoCancelAt else null,
        )
    }

    private fun Seat.toPlayer(
        status: GameStatus,
        now: Long,
    ): GamePlayer {
        val isHost = id == hostId
        val done = finishedAt?.let { it <= now } == true
        val state =
            when (status) {
                GameStatus.Waiting, GameStatus.Cancelled -> if (isHost) PlayerState.Host else PlayerState.Joined
                GameStatus.Started -> if (done) PlayerState.Finished else PlayerState.Playing
                GameStatus.Finished -> PlayerState.Finished
            }
        return GamePlayer(
            id = id,
            name = profile.name,
            avatarUrl = profile.avatarUrl,
            state = state,
            isHost = isHost,
            isCurrentUser = id == currentUserId,
            joinedAtMillis = joinedAt,
            finishedAtMillis = finishedAt?.takeIf { it <= now },
        )
    }

    /** Final ranking: the student's real answers, simulated scores for everybody else. */
    fun result(
        now: Long,
        questions: List<Question>,
        answers: Map<String, QuestionAnswer>,
    ): GameResult {
        seededResult?.let { return it }
        val outcomes = questions.map { it.outcomeFor(answers[it.id]) }
        val start = startedAt ?: createdAt
        val durationSeconds = settings.durationMinutes * GameRules.SECONDS_PER_MINUTE
        val standings =
            seatsAt(now)
                .map { seat ->
                    if (seat.id == currentUserId) {
                        val finished = seat.finishedAt ?: endsAt ?: now
                        seat.standing(
                            correct = outcomes.count { it == QuestionOutcome.Correct },
                            totalSeconds = ((finished - start) / GameRules.MILLIS_PER_SECOND).toInt(),
                            isCurrentUser = true,
                        )
                    } else {
                        val performance =
                            MultiplayerDummyData.performance[seat.id] ?: MultiplayerDummyData.defaultPerformance
                        seat.standing(
                            correct = (settings.questionCount * performance.correctShare).roundToInt(),
                            totalSeconds = (durationSeconds * performance.timeShare).roundToInt(),
                            isCurrentUser = false,
                        )
                    }
                }.map { it.copy(totalSeconds = it.totalSeconds.coerceIn(0, durationSeconds)) }
        return GameResult(
            code = code,
            settings = settings,
            playedAt = playedAt ?: LocalDateTime.now(),
            standings = GameRules.rank(standings),
            myOutcomes = outcomes,
        )
    }

    private fun Seat.standing(
        correct: Int,
        totalSeconds: Int,
        isCurrentUser: Boolean,
    ) = PlayerStanding(
        rank = 0,
        playerId = id,
        name = profile.name,
        avatarUrl = profile.avatarUrl,
        correct = correct,
        totalSeconds = totalSeconds,
        isCurrentUser = isCurrentUser,
    )
}

private fun Question.outcomeFor(answer: QuestionAnswer?): QuestionOutcome =
    when {
        answer == null || answer.isEmpty -> QuestionOutcome.Empty
        isAnsweredCorrectly(answer) -> QuestionOutcome.Correct
        else -> QuestionOutcome.Wrong
    }
