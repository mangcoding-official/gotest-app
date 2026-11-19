package com.gotest.app.feature.ruang.ui.tasks

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.feature.ruang.domain.RoomTask
import com.gotest.app.feature.ruang.ui.components.formatGrade
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// The words and dates of lib/studyPlan.ts. Plan and task dates are plain calendar dates (Y-m-d).

private val indonesian = Locale.forLanguageTag("id-ID")
private val short = DateTimeFormatter.ofPattern("d MMM", indonesian)
private val withDay = DateTimeFormatter.ofPattern("EEE, d MMM", indonesian)
private val long = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian)

/** "2 Des". */
fun shortDate(date: String): String = short.format(LocalDate.parse(date))

/** "Rab, 2 Des". */
fun dayDate(date: String): String = withDay.format(LocalDate.parse(date))

/** "2 Desember 2026" (the web's formatDateWib for a plan's exam date). */
fun longDate(date: String): String = long.format(LocalDate.parse(date))

/** The result of a done task: "8 dari 10", "Nilai 82,5", or done without a score (K-RB10). */
@Composable
fun resultText(
    status: String,
    score: Double?,
    scoreLabel: String?,
): String? =
    when {
        scoreLabel != null -> scoreLabel
        score != null -> stringResource(R.string.task_score, formatGrade(score))
        status == RoomTask.STATUS_DONE -> stringResource(R.string.task_done_without_score)
        else -> null
    }

@StringRes
fun taskTypeLabel(type: String): Int =
    when (type) {
        RoomTask.TYPE_QUIZ -> R.string.task_type_quiz
        RoomTask.TYPE_PRACTICE -> R.string.task_type_practice
        else -> R.string.task_type_external
    }

fun taskStatus(status: String): Pair<Int, GotestChipTone> =
    when (status) {
        "in_progress" -> R.string.task_status_in_progress to GotestChipTone.Blue
        RoomTask.STATUS_DONE -> R.string.task_status_done to GotestChipTone.Green
        "late" -> R.string.task_status_late to GotestChipTone.Red
        else -> R.string.task_status_not_started to GotestChipTone.Grey
    }

fun planState(state: String): Pair<Int, GotestChipTone> =
    when (state) {
        "running" -> R.string.plan_state_running to GotestChipTone.Green
        "finished" -> R.string.plan_state_finished to GotestChipTone.Grey
        "archived" -> R.string.plan_state_archived to GotestChipTone.Grey
        else -> R.string.plan_state_upcoming to GotestChipTone.Blue
    }

fun weekStatus(status: String): Pair<Int, GotestChipTone> =
    when (status) {
        "on_track" -> R.string.week_on_track to GotestChipTone.Green
        "slipping" -> R.string.week_slipping to GotestChipTone.Yellow
        "behind" -> R.string.week_behind to GotestChipTone.Red
        "upcoming" -> R.string.week_upcoming to GotestChipTone.Grey
        else -> R.string.week_nothing_due to GotestChipTone.Grey
    }

fun topicColor(color: String): Pair<Int, GotestChipTone> =
    when (color) {
        "red" -> R.string.topic_red to GotestChipTone.Red
        "yellow" -> R.string.topic_yellow to GotestChipTone.Yellow
        "green" -> R.string.topic_green to GotestChipTone.Green
        else -> R.string.topic_gray to GotestChipTone.Grey
    }
