package com.gotest.app.feature.history.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.SectionResult
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.ui.components.HistoryRankingCard
import com.gotest.app.feature.history.ui.components.HistorySummaryCard
import java.time.LocalDate

@Composable
fun HistoryDetailRoute(
    onBack: () -> Unit,
    onOpenDiscussion: (attemptId: String) -> Unit,
    onOpenPackageLeaderboard: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HistoryDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onShare = { result -> context.shareResult(result) },
        onOpenDiscussion = onOpenDiscussion,
        onOpenPackageLeaderboard = onOpenPackageLeaderboard,
        modifier = modifier,
    )
}

/** Hands a short summary to the system share sheet (ACTION_SEND, plain text). */
private fun Context.shareResult(result: AttemptResult) {
    val score = GotestFormat.number(result.score)
    val rank = result.rank
    val participants = result.participants
    val text =
        if (rank != null && participants != null) {
            getString(
                R.string.history_detail_share_text_ranked,
                score,
                result.title,
                GotestFormat.number(rank),
                GotestFormat.number(participants),
            )
        } else {
            getString(R.string.history_detail_share_text, score, result.title)
        }
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startActivity(Intent.createChooser(send, getString(R.string.history_detail_share_chooser)))
}

@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUiState,
    onEvent: (HistoryDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onShare: (AttemptResult) -> Unit,
    onOpenDiscussion: (attemptId: String) -> Unit,
    onOpenPackageLeaderboard: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val success = uiState as? HistoryDetailUiState.Success
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.history_detail_title),
            subtitle = success?.result?.let { subtitleFor(it) },
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                if (success != null) {
                    GotestIconButton(
                        icon = GotestIcons.Linear.Export,
                        contentDescription = stringResource(R.string.history_detail_cd_share),
                        onClick = { onShare(success.result) },
                    )
                }
            },
        )
        val stateModifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        when (uiState) {
            HistoryDetailUiState.Loading -> {
                GotestLoadingView(modifier = stateModifier)
            }

            HistoryDetailUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(HistoryDetailUiEvent.Retry) }, modifier = stateModifier)
            }

            HistoryDetailUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.Directbox,
                    title = stringResource(R.string.history_detail_not_found_title),
                    message = stringResource(R.string.history_detail_not_found_message),
                    modifier = stateModifier,
                )
            }

            is HistoryDetailUiState.Success -> {
                HistoryDetailContent(
                    state = uiState,
                    onOpenPackageLeaderboard = { onOpenPackageLeaderboard(uiState.result.packageId) },
                    modifier = Modifier.weight(1f),
                )
                HistoryDetailActions(
                    onClose = onBack,
                    onOpenDiscussion = { onOpenDiscussion(uiState.result.attemptId) },
                )
            }
        }
    }
}

/** "Simulasi UTBK-SNBT" for tryouts, "Latihan UTBK-SNBT" for practice. */
@Composable
private fun subtitleFor(result: AttemptResult): String =
    stringResource(
        when (result.type) {
            PackageType.Tryout -> R.string.history_detail_subtitle_tryout
            PackageType.Practice -> R.string.history_detail_subtitle_practice
        },
        result.examLabel,
    )

@Composable
private fun HistoryDetailContent(
    state: HistoryDetailUiState.Success,
    onOpenPackageLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        HistorySummaryCard(
            title = state.result.title,
            description = state.result.description,
            sections = state.result.sections,
            score = state.result.score,
            modifier = Modifier.fillMaxWidth(),
        )
        HistoryRankingCard(
            rows = state.ranking,
            onSeeMore = onOpenPackageLeaderboard,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** White bottom bar with "Tutup" and "Buka Pembahasan". */
@Composable
private fun HistoryDetailActions(
    onClose: () -> Unit,
    onOpenDiscussion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp),
    ) {
        GotestDivider()
        Row(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GotestButton(
                text = stringResource(R.string.history_detail_close),
                onClick = onClose,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text = stringResource(R.string.history_detail_open_discussion),
                onClick = onOpenDiscussion,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val previewResult =
    AttemptResult(
        attemptId = "att_tryout_utbk_1",
        packageId = "pkg_tryout_utbk_sept_1",
        title = "Simulasi UTBK-SNBT September Session 1",
        description =
            "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit. " +
                "Skor merupakan hasil simulasi, bukan skor resmi UTBK-SNBT.",
        examLabel = "UTBK-SNBT",
        type = PackageType.Tryout,
        score = 140,
        rank = 12,
        participants = 12_512,
        correct = 12,
        wrong = 5,
        empty = 3,
        topicAccuracy = emptyList(),
        weakestTopic = null,
        sections =
            listOf(
                "Penalaran Umum",
                "Pengetahuan dan Pemahaman Umum",
                "Pemahaman Bacaan dan Menulis",
                "Pengetahuan Kuantitatif",
                "Literasi Bahasa Indonesia",
                "Literasi Bahasa Inggris",
                "Penalaran Matematika",
            ).map { SectionResult(it, correct = 20, wrong = 20, empty = 5) },
        completedOn = LocalDate.of(2026, 9, 19),
        durationSeconds = 1_232,
    )

private val previewRanking =
    listOf(
        HistoryRankingRow(1, "John Stuwart", null, 7_805, 424, false),
        HistoryRankingRow(2, "Michael T.", null, 8_621, 324, false),
        HistoryRankingRow(3, "Emily Carter", null, 9_612, 172, false),
        HistoryRankingRow(4, "Alex Thompson", null, 9_161, 124, false),
        HistoryRankingRow(5, "Mia Johnson", null, 9_310, 121, false),
        HistoryRankingRow(12, "Gilang P.", LocalDate.of(2026, 9, 19), 10_280, 104, true),
    )

@Composable
private fun HistoryDetailPreviewHost(uiState: HistoryDetailUiState) {
    GotestTheme {
        HistoryDetailScreen(
            uiState = uiState,
            onEvent = {},
            onBack = {},
            onShare = {},
            onOpenDiscussion = {},
            onOpenPackageLeaderboard = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryDetailScreenSuccessPreview() {
    HistoryDetailPreviewHost(HistoryDetailUiState.Success(previewResult, previewRanking))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryDetailScreenLoadingPreview() {
    HistoryDetailPreviewHost(HistoryDetailUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryDetailScreenEmptyPreview() {
    HistoryDetailPreviewHost(HistoryDetailUiState.Empty)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HistoryDetailScreenErrorPreview() {
    HistoryDetailPreviewHost(HistoryDetailUiState.Error)
}
