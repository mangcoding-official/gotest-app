package com.gotest.app.feature.ruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.ruang.data.parseServerTime
import com.gotest.app.feature.ruang.domain.MyExamStatus
import com.gotest.app.feature.ruang.domain.ScheduleState
import java.text.NumberFormat
import java.util.Locale

/** A Ruang or Ujian page: back button, title, and the subtle background of the student pages. */
@Composable
fun RuangScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        bottomBar = bottomBar,
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(GotestTheme.spacing.none),
        content = content,
    )
}

@Composable
fun scheduleStateLabel(state: ScheduleState): String =
    stringResource(
        when (state) {
            ScheduleState.Scheduled -> R.string.exam_state_scheduled
            ScheduleState.Running -> R.string.exam_state_running
            ScheduleState.Closed -> R.string.exam_state_closed
        },
    )

@Composable
fun myStatusLabel(status: MyExamStatus): String =
    stringResource(
        when (status) {
            MyExamStatus.NotStarted -> R.string.exam_status_not_started
            MyExamStatus.InProgress -> R.string.exam_status_in_progress
            MyExamStatus.Submitted -> R.string.exam_status_submitted
        },
    )

/** The schedule state and this student's status, side by side (Ruang/Exams/Index, Show). */
@Composable
fun ExamStatusChips(
    state: ScheduleState,
    myStatus: MyExamStatus,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestChip(
            text = scheduleStateLabel(state),
            tone =
                when (state) {
                    ScheduleState.Running -> GotestChipTone.Violet
                    ScheduleState.Scheduled -> GotestChipTone.Blue
                    ScheduleState.Closed -> GotestChipTone.Grey
                },
        )
        GotestChip(
            text = myStatusLabel(myStatus),
            tone =
                when (myStatus) {
                    MyExamStatus.Submitted -> GotestChipTone.Green
                    MyExamStatus.InProgress -> GotestChipTone.Yellow
                    MyExamStatus.NotStarted -> GotestChipTone.Grey
                },
        )
    }
}

/** A server time in WIB, "17 Nov 2026, 10.49 WIB" (the web's `formatWindowInstant`). */
fun serverWindow(iso: String): String = GotestFormat.windowInstant(parseServerTime(iso))

/** "82,5", "76": Indonesian, at most one decimal (the web's `formatGrade`). */
fun formatGrade(grade: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply { maximumFractionDigits = 1 }.format(grade)

enum class NoticeTone { Warning, Danger, Info }

/** A boxed message: a server refusal (red), a caution (yellow), or a note (blue). */
@Composable
fun NoticeBox(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.Warning,
) {
    val colors = GotestTheme.colors
    val (container: Color, content: Color) =
        when (tone) {
            NoticeTone.Warning -> colors.statusWarningSoft to colors.textPrimary
            NoticeTone.Danger -> colors.statusDangerSoft to colors.statusDanger
            NoticeTone.Info -> colors.bgBlueSoft to colors.bgBlueDark
        }
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .background(container, MaterialTheme.shapes.small)
                .padding(GotestTheme.spacing.md),
        style = MaterialTheme.typography.bodySmall,
        color = content,
    )
}
