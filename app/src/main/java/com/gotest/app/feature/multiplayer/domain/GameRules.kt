package com.gotest.app.feature.multiplayer.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** Outcome of a duel from the current user's side ("MB/Duel Result Banner"). */
enum class DuelOutcome { Win, Lose, Draw }

/** Rules shown on the Main Bareng page, in one place so screens and the fake agree. */
object GameRules {
    const val CODE_LENGTH = 6
    const val MIN_QUESTIONS = 5
    const val MAX_QUESTIONS = 30
    const val DEFAULT_QUESTIONS = 20
    const val DEFAULT_DURATION_MINUTES = 15

    /** A game needs more than this many seconds per question. */
    const val MIN_SECONDS_PER_QUESTION = 30

    const val MILLIS_PER_SECOND = 1_000L
    const val MILLIS_PER_MINUTE = 60_000L
    const val SECONDS_PER_MINUTE = 60

    /** An unstarted game cancels itself after five minutes. */
    const val AUTO_CANCEL_MILLIS = 5 * MILLIS_PER_MINUTE

    val durationOptionsMinutes: List<Int> = listOf(10, 15, 20, 30, 45, 60)

    /** Keeps only letters and digits, upper-cased, at most [CODE_LENGTH] characters. */
    fun normalizeCode(raw: String): String =
        raw
            .uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(CODE_LENGTH)

    fun isDurationAllowed(
        durationMinutes: Int,
        questionCount: Int,
    ): Boolean = durationMinutes * SECONDS_PER_MINUTE > questionCount * MIN_SECONDS_PER_QUESTION

    /** Most correct answers first; a tie goes to the faster total time. */
    fun rank(standings: List<PlayerStanding>): List<PlayerStanding> =
        standings
            .sortedWith(compareByDescending<PlayerStanding> { it.correct }.thenBy { it.totalSeconds })
            .mapIndexed { index, standing -> standing.copy(rank = index + 1) }

    fun duelOutcome(result: GameResult): DuelOutcome? {
        val me = result.me
        val opponent = result.standings.firstOrNull { !it.isCurrentUser }
        return when {
            me == null || opponent == null -> null
            me.correct == opponent.correct && me.totalSeconds == opponent.totalSeconds -> DuelOutcome.Draw
            me.rank < opponent.rank -> DuelOutcome.Win
            else -> DuelOutcome.Lose
        }
    }

    /** Whole seconds from [nowMillis] until [targetMillis], never negative. */
    fun secondsUntil(
        targetMillis: Long,
        nowMillis: Long,
    ): Int = ((targetMillis - nowMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt().coerceAtLeast(0)
}

/** Emits the wall clock once per second while collected; drives countdowns and the fake's simulation. */
fun clockTicks(periodMillis: Long = GameRules.MILLIS_PER_SECOND): Flow<Long> =
    flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(periodMillis)
        }
    }
