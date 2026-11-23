package com.gotest.app.feature.multiplayer.domain

/** Outcome of a duel from the viewer's side ("MB/Duel Result Banner"). */
enum class DuelOutcome { Win, Lose, Draw }

/** The rules the screens show, as the web's MainBareng `types.ts` (DEC-085, DEC-086). */
object GameRules {
    const val CODE_LENGTH = 6
    const val MAX_QUESTIONS = 30
    const val DEFAULT_QUESTIONS = 20
    const val DEFAULT_DURATION_MINUTES = 15
    const val MIN_SECONDS_PER_QUESTION = 30
    const val LOBBY_MINUTES = 5
    const val ROOM_MAX_PLAYERS = 10
    private const val SECONDS_PER_MINUTE = 60

    val durationChoices: List<Int> = listOf(10, 15, 20, 30, 45, 60)

    /** Web `shortestDuration`: the first choice with at least 30 seconds per question. */
    fun shortestDuration(questionCount: Int): Int {
        val needed = (questionCount * MIN_SECONDS_PER_QUESTION + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
        return durationChoices.firstOrNull { it >= needed } ?: durationChoices.last()
    }

    /** Web CodeInput: letters and digits only, upper-cased, at most six. */
    fun normalizeCode(raw: String): String =
        raw
            .uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(CODE_LENGTH)

    fun duelOutcome(outcome: String?): DuelOutcome? =
        when (outcome) {
            GameResult.WIN -> DuelOutcome.Win
            GameResult.LOSE -> DuelOutcome.Lose
            GameResult.DRAW -> DuelOutcome.Draw
            else -> null
        }
}
