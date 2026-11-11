package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.PlayerState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Copy shared by several Main Bareng screens; every word comes from strings_multiplayer.xml.

@Composable
@ReadOnlyComposable
fun gameTypeLabel(type: GameType): String =
    stringResource(
        when (type) {
            GameType.Duel -> R.string.multiplayer_type_duel
            GameType.Room -> R.string.multiplayer_type_room
        },
    )

/** "Room • Penalaran Matematika". */
@Composable
@ReadOnlyComposable
fun gameTitle(
    type: GameType,
    subtest: String,
): String = stringResource(R.string.multiplayer_game_title, gameTypeLabel(type), subtest)

/** "20 soal". */
@Composable
@ReadOnlyComposable
fun questionCountLabel(count: Int): String = pluralStringResource(R.plurals.multiplayer_question_count, count, count)

/** "15 menit". */
@Composable
@ReadOnlyComposable
fun durationLabel(minutes: Int): String = pluralStringResource(R.plurals.multiplayer_duration_minutes, minutes, minutes)

/** "Gilang Permana (Kamu)" for the current user, the plain name otherwise. */
@Composable
@ReadOnlyComposable
fun playerName(
    name: String,
    isCurrentUser: Boolean,
): String = if (isCurrentUser) stringResource(R.string.multiplayer_player_you, name) else name

/** "22 Okt 2026, 19:20". */
@Composable
@ReadOnlyComposable
fun playedAtLabel(dateTime: LocalDateTime): String =
    stringResource(
        R.string.multiplayer_date_time,
        GotestFormat.mediumDate(dateTime.toLocalDate()),
        GotestFormat.time(dateTime),
    )

/** "3 dari 10". */
@Composable
@ReadOnlyComposable
fun countOfLabel(
    count: Int,
    total: Int,
): String = stringResource(R.string.multiplayer_count_of, count, total)

/** Seat subtitle: "Pembuat permainan", "Bergabung 2 menit lalu", "Selesai baru saja", … */
@Composable
@ReadOnlyComposable
fun playerSubtitle(
    player: GamePlayer,
    nowMillis: Long,
): String {
    val finishedAt = player.finishedAtMillis
    return when {
        player.state == PlayerState.Playing -> {
            stringResource(R.string.multiplayer_seat_still_playing)
        }

        player.state == PlayerState.Finished && finishedAt != null -> {
            minutesAgo(
                nowMillis - finishedAt,
                R.string.multiplayer_finished_just_now,
                R.plurals.multiplayer_finished_ago,
            )
        }

        player.state == PlayerState.Finished -> {
            stringResource(R.string.multiplayer_seat_finished)
        }

        player.isHost -> {
            stringResource(R.string.multiplayer_seat_host_subtitle)
        }

        else -> {
            minutesAgo(
                nowMillis - player.joinedAtMillis,
                R.string.multiplayer_joined_just_now,
                R.plurals.multiplayer_joined_ago,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun minutesAgo(
    elapsedMillis: Long,
    justNow: Int,
    plural: Int,
): String {
    val minutes = (elapsedMillis / GameRules.MILLIS_PER_MINUTE).toInt()
    return if (minutes < 1) stringResource(justNow) else pluralStringResource(plural, minutes, minutes)
}

/** "Ardy P.": first name and the initial of the second, as on the ranking table. */
fun shortName(fullName: String): String {
    val parts = fullName.trim().split(' ').filter { it.isNotEmpty() }
    return if (parts.size < 2) fullName else "${parts[0]} ${parts[1].first()}."
}
