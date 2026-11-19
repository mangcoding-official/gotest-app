package com.gotest.app.feature.ruang.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.MyExamStatus
import com.gotest.app.feature.ruang.domain.ScheduleState
import com.gotest.app.feature.ruang.ui.components.ExamStatusChips
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.components.PinGuidance
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.components.rememberPinRequest
import com.gotest.app.feature.ruang.ui.components.rememberScreenPinning
import com.gotest.app.feature.ruang.ui.components.serverWindow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExamDetailRoute(
    onBack: () -> Unit,
    onOpenWork: (code: String, attemptId: Long) -> Unit,
    onOpenResult: (code: String, attemptId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinning = rememberScreenPinning()
    val pinRequest = rememberPinRequest(pinning)
    var askPin by remember { mutableStateOf(false) }
    // Pinned for nothing: Mulai failed, or the student left this page other than to work (review 4).
    var toWork by remember { mutableStateOf(false) }
    LifecycleResumeEffect(viewModel) {
        viewModel.load()
        onPauseOrDispose { }
    }
    DisposableEffect(pinning) { onDispose { if (!toWork) pinning.release() } }
    ObserveAsEvents(viewModel.openWork) { attemptId ->
        toWork = true
        onOpenWork(viewModel.code, attemptId)
    }
    ObserveAsEvents(viewModel.startFailed) { pinning.release() }
    ExamDetailScreen(
        uiState = uiState,
        onRetry = viewModel::load,
        onBack = onBack,
        onStart = { if (pinning.isPinned) viewModel.start() else askPin = true },
        onOpenResult = { attemptId -> onOpenResult(viewModel.code, attemptId) },
        modifier = modifier,
        pinGuidance = { PinGuidance(state = pinRequest) },
    )
    if (askPin) {
        GotestDialog(
            title = stringResource(R.string.exam_pin_title),
            message = stringResource(R.string.exam_pin_message),
            confirmText = stringResource(R.string.exam_pin_confirm),
            onConfirm = {
                askPin = false
                // Once pinned, the Ujian starts; one request at a time (review 3).
                pinRequest.request(viewModel::start)
            },
            onDismissRequest = { askPin = false },
            dismissText = stringResource(R.string.ruang_cancel),
        )
    }
}

@Composable
fun ExamDetailScreen(
    uiState: ExamDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
    pinGuidance: @Composable () -> Unit = {},
) {
    val detail = uiState.detail
    RuangScaffold(
        title = detail?.title ?: stringResource(R.string.exam_list_title),
        subtitle = detail?.let { stringResource(R.string.exam_detail_back, it.organization.name) },
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            detail == null -> {
                GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize().padding(padding))
            }

            else -> {
                ExamDetailContent(
                    uiState = uiState,
                    detail = detail,
                    onBack = onBack,
                    onStart = onStart,
                    onOpenResult = onOpenResult,
                    modifier = Modifier.padding(padding),
                    pinGuidance = pinGuidance,
                )
            }
        }
    }
}

@Composable
private fun ExamDetailContent(
    uiState: ExamDetailUiState,
    detail: ExamDetail,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
    pinGuidance: @Composable () -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ExamStatusChips(state = detail.state, myStatus = detail.myStatus)
        ExamFacts(detail)
        detail.instructions?.let { instructions ->
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                CardTitle(stringResource(R.string.exam_detail_instructions))
                RichText(
                    content = instructions,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textPrimary,
                )
            }
        }
        ExamRules(detail.rules)
        ExamDetailNotices(uiState = uiState, detail = detail)
        pinGuidance()
        ExamDetailActions(
            uiState = uiState,
            detail = detail,
            onBack = onBack,
            onStart = onStart,
            onOpenResult = onOpenResult,
        )
    }
}

@Composable
private fun ExamFacts(detail: ExamDetail) {
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        FactRow(
            stringResource(R.string.exam_detail_questions_label),
            stringResource(R.string.exam_detail_questions, detail.questionCount),
        )
        HorizontalDivider(color = GotestTheme.colors.borderSubtle)
        FactRow(
            stringResource(R.string.exam_detail_duration_label),
            stringResource(R.string.exam_detail_duration, detail.durationMinutes),
        )
        HorizontalDivider(color = GotestTheme.colors.borderSubtle)
        FactRow(stringResource(R.string.exam_detail_opens_label), serverWindow(detail.opensAt))
        HorizontalDivider(color = GotestTheme.colors.borderSubtle)
        FactRow(stringResource(R.string.exam_detail_closes_label), serverWindow(detail.closesAt))
    }
}

@Composable
private fun FactRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
    }
}

@Composable
private fun ExamRules(rules: List<String>) {
    if (rules.isEmpty()) return
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        CardTitle(stringResource(R.string.exam_detail_rules))
        rules.forEachIndexed { index, rule ->
            Text(
                text = "${index + 1}. $rule",
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
}

@Composable
private fun ExamDetailNotices(
    uiState: ExamDetailUiState,
    detail: ExamDetail,
) {
    if (detail.myStatus == MyExamStatus.NotStarted && detail.canStart) {
        NoticeBox(text = stringResource(R.string.exam_detail_stay), tone = NoticeTone.Info)
    }
    detail.timeNotice?.let { NoticeBox(text = it) }
    uiState.signInEndsSoon?.let { endsAt ->
        NoticeBox(text = stringResource(R.string.exam_login_expiry, dateAtTime(endsAt)))
    }
    val problem =
        when (val current = uiState.problem) {
            is StartProblem.Server -> current.message
            StartProblem.Offline -> stringResource(R.string.state_error_message)
            null -> detail.blockedReason
        }
    problem?.let { NoticeBox(text = it, tone = NoticeTone.Danger) }
}

@Composable
private fun ExamDetailActions(
    uiState: ExamDetailUiState,
    detail: ExamDetail,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenResult: (Long) -> Unit,
) {
    val attempt = detail.attemptId
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        when {
            detail.myStatus == MyExamStatus.Submitted && attempt != null -> {
                GotestButton(
                    text = stringResource(R.string.exam_action_result),
                    onClick = { onOpenResult(attempt) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            detail.myStatus == MyExamStatus.InProgress && attempt != null -> {
                GotestButton(
                    text = stringResource(R.string.exam_detail_continue),
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            detail.canStart -> {
                GotestButton(
                    text =
                        stringResource(
                            if (uiState.isStarting) R.string.exam_detail_starting else R.string.exam_detail_start,
                        ),
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isStarting,
                )
            }
        }
        GotestButton(
            text = stringResource(R.string.exam_detail_back_to_list),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

/** "18 November 2026 pukul 10.00 WIB", the web's date and time (A2b-7). */
@Composable
private fun dateAtTime(instant: Instant): String =
    stringResource(R.string.result_at, dateWib.format(instant), timeWib.format(instant))

private val jakarta = ZoneId.of("Asia/Jakarta")
private val dateWib = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID")).withZone(jakarta)
private val timeWib = DateTimeFormatter.ofPattern("HH.mm 'WIB'", Locale.forLanguageTag("id-ID")).withZone(jakarta)

private val previewDetail =
    ExamDetail(
        organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
        scheduleId = 41,
        title = "PTS Matematika Kelas 8",
        instructions = "<p>Kerjakan dengan jujur.</p>",
        questionCount = 6,
        durationMinutes = 90,
        opensAt = "2026-12-01T00:30:00+00:00",
        closesAt = "2026-12-01T03:30:00+00:00",
        state = ScheduleState.Running,
        rules = listOf("Soal dan pilihan jawaban diacak untuk setiap siswa."),
        myStatus = MyExamStatus.NotStarted,
        canStart = true,
    )

@Preview(showBackground = true)
@Composable
private fun ExamDetailScreenPreview() {
    GotestTheme {
        ExamDetailScreen(
            uiState = ExamDetailUiState(isLoading = false, detail = previewDetail),
            onRetry = {},
            onBack = {},
            onStart = {},
            onOpenResult = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamDetailScreenBlockedPreview() {
    GotestTheme {
        ExamDetailScreen(
            uiState =
                ExamDetailUiState(
                    isLoading = false,
                    detail =
                        previewDetail.copy(
                            state = ScheduleState.Scheduled,
                            canStart = false,
                            blockedCode = "not_open",
                            blockedReason = "Ujian belum dibuka. Dibuka 2 Desember 2026 pukul 07.30 WIB.",
                        ),
                ),
            onRetry = {},
            onBack = {},
            onStart = {},
            onOpenResult = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamDetailScreenLoadingPreview() {
    GotestTheme {
        ExamDetailScreen(uiState = ExamDetailUiState(), onRetry = {}, onBack = {}, onStart = {}, onOpenResult = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ExamDetailScreenErrorPreview() {
    GotestTheme {
        ExamDetailScreen(uiState = ExamDetailUiState(isLoading = false), onRetry = {
        }, onBack = {}, onStart = {}, onOpenResult = {})
    }
}
