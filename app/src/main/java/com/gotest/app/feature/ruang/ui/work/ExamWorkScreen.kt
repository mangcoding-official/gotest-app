package com.gotest.app.feature.ruang.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.ExamWorkPage

@Composable
fun ExamWorkScreen(
    uiState: ExamWorkUiState,
    onEvent: (ExamWorkEvent) -> Unit,
    modifier: Modifier = Modifier,
    pinGate: @Composable () -> Unit = {},
) {
    val page = uiState.page
    Scaffold(
        modifier = modifier,
        topBar = { WorkHeader(page = page, uiState = uiState, onExit = { onEvent(ExamWorkEvent.OpenExit) }) },
        bottomBar = { if (page != null && !uiState.awaitingPin) WorkFooter(uiState = uiState, onEvent = onEvent) },
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(GotestTheme.spacing.none),
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(
                    onRetry = { onEvent(ExamWorkEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message =
                        (uiState.loadProblem as? WorkLoadProblem.Server)?.message
                            ?: stringResource(R.string.state_error_message),
                )
            }

            // The paper stays covered until the screen is pinned (API-K8 A, T-026 A2b review 1).
            uiState.awaitingPin -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = GotestTheme.spacing.screenHorizontal,
                                vertical = GotestTheme.spacing.lg,
                            ),
                ) {
                    pinGate()
                }
            }

            else -> {
                WorkBody(uiState = uiState, page = page, onEvent = onEvent, modifier = Modifier.padding(padding))
            }
        }
    }
    WorkDialogs(uiState = uiState, onEvent = onEvent)
}

/** Kicker "Ujian", the title, the server's clock, and "Keluar" (ExamRunnerShell). */
@Composable
private fun WorkHeader(
    page: ExamWorkPage?,
    uiState: ExamWorkUiState,
    onExit: () -> Unit,
) {
    val spacing = GotestTheme.spacing
    Surface(color = GotestTheme.colors.bgApp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (page?.isQuiz == true) R.string.quiz_kicker else R.string.work_kicker),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textSecondary,
                )
                Text(
                    text = page?.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                    maxLines = 2,
                )
            }
            if (page != null && !page.isQuiz) {
                GotestChip(
                    text = stringResource(R.string.work_time_left, examClock(uiState.secondsLeft)),
                    tone = if (uiState.timeUp) GotestChipTone.Red else GotestChipTone.Violet,
                )
            }
            GotestButton(
                text = stringResource(R.string.work_exit),
                onClick = onExit,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
    }
}

@Composable
private fun WorkBody(
    uiState: ExamWorkUiState,
    page: ExamWorkPage,
    onEvent: (ExamWorkEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val scroll = rememberScrollState()
    // A new block starts at the top, as on the web.
    LaunchedEffect(uiState.blockIndex) { scroll.scrollTo(0) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        WorkBanners(uiState = uiState)
        uiState.currentBlock?.let { block ->
            WorkBlock(
                block = block,
                uiState = uiState,
                onAnswer = { itemId, answer -> onEvent(ExamWorkEvent.Answer(itemId, answer)) },
            )
        }
        if (!page.isQuiz) ViolationPanel(violations = uiState.violations, limit = uiState.violationLimit)
        QuestionNavigator(uiState = uiState, onGoTo = { onEvent(ExamWorkEvent.GoToItem(it)) })
    }
}

/** "Sebelumnya", and "Selanjutnya" or "Kumpulkan" on the last block. */
@Composable
private fun WorkFooter(
    uiState: ExamWorkUiState,
    onEvent: (ExamWorkEvent) -> Unit,
) {
    val spacing = GotestTheme.spacing
    Surface(color = GotestTheme.colors.bgApp) {
        Column {
            HorizontalDivider(color = GotestTheme.colors.borderSubtle)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                GotestButton(
                    text = stringResource(R.string.work_previous),
                    onClick = { onEvent(ExamWorkEvent.Previous) },
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    enabled = uiState.blockIndex > 0,
                )
                if (uiState.isLastBlock) {
                    GotestButton(
                        text = stringResource(R.string.work_submit),
                        onClick = { onEvent(ExamWorkEvent.OpenSubmit) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.locked,
                    )
                } else {
                    GotestButton(
                        text = stringResource(R.string.work_next),
                        onClick = { onEvent(ExamWorkEvent.Next) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** "MM:SS" with the minutes not capped at two digits, "125:07" (the web's `formatExamDuration`). */
fun examClock(seconds: Long): String = "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)

private const val SECONDS_PER_MINUTE = 60

@Preview(showBackground = true)
@Composable
private fun ExamWorkScreenLoadingPreview() {
    GotestTheme { ExamWorkScreen(uiState = ExamWorkUiState(), onEvent = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ExamWorkScreenErrorPreview() {
    GotestTheme {
        ExamWorkScreen(
            uiState = ExamWorkUiState(isLoading = false, loadProblem = WorkLoadProblem.Server("Data tidak ditemukan.")),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ExamWorkScreenPreview() {
    GotestTheme { ExamWorkScreen(uiState = previewWorkState(), onEvent = {}) }
}
