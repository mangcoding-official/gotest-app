package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.ruang.data.parseServerTime
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTime =
    DateTimeFormatter
        .ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60

/** Web `modeLabel`: "Duel" or "Room". */
@Composable
fun modeLabel(mode: String): String =
    stringResource(if (mode == GameState.MODE_DUEL) R.string.multiplayer_type_duel else R.string.multiplayer_type_room)

/** "Duel • Penalaran Umum". */
@Composable
fun gameTitle(
    mode: String,
    subject: String,
): String = stringResource(R.string.multiplayer_game_title, modeLabel(mode), subject)

@Composable
fun playerName(
    name: String,
    isYou: Boolean,
): String = if (isYou) stringResource(R.string.multiplayer_player_you, name) else name

/** Web `formatClock`: minutes and seconds, minutes not capped at an hour ("60:00"). */
fun formatClock(seconds: Long): String =
    "%02d:%02d".format(Locale.ROOT, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

/** Web `formatDateTime`: "23 Nov 2026, 14:30" in WIB. */
fun formatDateTime(at: String?): String =
    at?.let { runCatching { dateTime.format(parseServerTime(it)) }.getOrNull() }.orEmpty()

/** Web `timeAgo`, counted from the server's clock of the state. */
@Composable
fun timeAgo(
    at: String?,
    serverNow: String?,
): String {
    if (at == null || serverNow == null) return ""
    val minutes =
        runCatching { Duration.between(parseServerTime(at), parseServerTime(serverNow)).toMinutes() }.getOrDefault(0)
    return when {
        minutes < 1 -> stringResource(R.string.multiplayer_just_now)
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.multiplayer_minutes_ago, minutes.toInt())
        else -> stringResource(R.string.multiplayer_hours_ago, (minutes / MINUTES_PER_HOUR).toInt())
    }
}

/** The sentence of a refused request: the server's for a rule of the game, the web's own otherwise. */
@Composable
fun problemText(problem: GameProblem): String =
    when (problem) {
        is GameProblem.Server -> problem.message
        is GameProblem.Invalid -> problem.message
        GameProblem.NotFound -> stringResource(R.string.multiplayer_problem_not_found)
        GameProblem.ShortCode -> stringResource(R.string.multiplayer_problem_short_code)
        GameProblem.Connection -> stringResource(R.string.multiplayer_problem_connection)
    }
