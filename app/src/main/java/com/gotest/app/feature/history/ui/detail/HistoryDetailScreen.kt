package com.gotest.app.feature.history.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryRankingRow
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.domain.HistorySection
import com.gotest.app.feature.history.ui.history.localDate
import kotlin.math.roundToInt

/**
 * [onOpenPackageBoard] gets the package id of a Try Out, or null (the web then opens the Leaderboard page).
 * "Lihat Hasil" and "Buka Pembahasan" wait for the result and explanation screens (T-026 A2c).
 */
@Composable
fun HistoryDetailRoute(
    onBack: () -> Unit,
    onOpenPackageBoard: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onOpenPackageBoard = onOpenPackageBoard,
        modifier = modifier,
    )
}

@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenPackageBoard: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = uiState.detail
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = detail?.attempt?.packageName.orEmpty(),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when {
            detail != null -> {
                DetailContent(detail = detail, onOpenPackageBoard = onOpenPackageBoard)
            }

            uiState.loadFailed -> {
                GotestErrorView(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(R.string.history_open_failed),
                )
            }

            else -> {
                GotestLoadingView()
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: HistoryDetail,
    onOpenPackageBoard: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val attempt = detail.attempt
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = attempt.packageName,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text =
                    attempt.description?.takeIf(String::isNotBlank) ?: stringResource(R.string.history_detail_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        SectionsCard(attempt)
        // Only a Try Out has a package ranking (web HistoryResultModal); Latihan shows the summary alone.
        if (attempt.isTryout) {
            RankingCard(ranking = detail.ranking, onMore = { onOpenPackageBoard(attempt.packageId) })
        }
    }
}

@Composable
private fun SectionsCard(
    attempt: HistoryResult,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(spacing.none),
        verticalSpacing = spacing.none,
    ) {
        Text(
            text = stringResource(R.string.history_detail_sections),
            modifier = Modifier.fillMaxWidth().background(GotestTheme.colors.bgSubtle).padding(spacing.md),
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        attempt.sections.forEach { section ->
            GotestDivider()
            SectionRow(section)
        }
        GotestDivider()
        FinalScoreRow(attempt)
    }
}

@Composable
private fun SectionRow(
    section: HistorySection,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Text(text = section.name, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            Count(stringResource(R.string.history_detail_correct, section.correct), colors.statusSuccess)
            Count(stringResource(R.string.history_detail_wrong, section.wrong), colors.statusDanger)
            Count(stringResource(R.string.history_detail_empty, section.empty), colors.textPrimary)
        }
    }
}

@Composable
private fun Count(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun FinalScoreRow(
    attempt: HistoryResult,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().background(colors.bgSubtle).padding(GotestTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.history_detail_final),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            if (attempt.ranked == false) {
                GotestChip(
                    text =
                        stringResource(
                            if (attempt.disqualified ==
                                true
                            ) {
                                R.string.history_detail_disqualified
                            } else {
                                R.string.history_detail_repeat
                            },
                        ),
                    tone = GotestChipTone.Grey,
                )
            }
        }
        Text(
            text = attempt.scoreLabel ?: stringResource(R.string.history_detail_score),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Text(
            text = attempt.score.roundToInt().toString(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.statusSuccess,
        )
    }
}

@Composable
private fun RankingCard(
    ranking: List<HistoryRankingRow>,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.history_detail_ranking),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.history_detail_ranking_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (ranking.isEmpty()) {
            Text(
                text = stringResource(R.string.history_detail_ranking_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            Column {
                ranking.forEachIndexed { index, row ->
                    if (index > 0) GotestDivider()
                    RankingRow(row)
                }
            }
        }
        GotestButton(
            text = stringResource(R.string.history_detail_more),
            onClick = onMore,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun RankingRow(
    row: HistoryRankingRow,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        RankBadge(row.rank)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOf(
                        localDate(row.submittedAt)?.let(GotestFormat::mediumDate)
                            ?: stringResource(R.string.history_date_missing),
                        row.durationLabel,
                    ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.history_detail_ranking_score, row.score.roundToInt()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (row.rank <= PODIUM) colors.bgYellow else colors.textPrimary,
        )
    }
}

/** Web `RankBadge`: gold, silver and bronze for the podium, grey after. */
@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val fill =
        when (rank) {
            1 -> colors.bgYellow
            2 -> colors.medalSilver
            PODIUM -> colors.medalBronze
            else -> colors.bgGrey
        }
    Box(
        modifier = modifier.size(BADGE_SIZE).background(fill, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = rank.toString(), style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
    }
}

private const val PODIUM = 3
private val BADGE_SIZE = 28.dp

private val previewDetail =
    HistoryDetail(
        attempt =
            HistoryResult(
                id = 1,
                packageName = "Try Out UTBK-SNBT 3",
                categoryName = "UTBK-SNBT",
                mode = "tryout",
                durationLabel = "95 Menit 12 Detik",
                score = 612.4,
                ranked = false,
                disqualified = false,
                packageId = 11,
                sections =
                    listOf(
                        HistorySection("Penalaran Umum", correct = 20, wrong = 7, empty = 3),
                        HistorySection("Literasi Bahasa Indonesia", correct = 18, wrong = 10, empty = 2),
                    ),
            ),
        ranking =
            listOf(
                HistoryRankingRow(1, "Nadia Putri", 812.0, "2026-11-10T09:00:00+07:00", "88 Menit 2 Detik"),
                HistoryRankingRow(2, "Rafi Akbar", 790.6, "2026-11-10T09:10:00+07:00", "90 Menit 40 Detik"),
                HistoryRankingRow(4, "Gilang Ramadhan", 702.0, null, "93 Menit 1 Detik"),
            ),
    )

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun HistoryDetailScreenPreview() {
    GotestTheme {
        HistoryDetailScreen(
            uiState = HistoryDetailUiState(isLoading = false, detail = previewDetail),
            onBack = {},
            onRetry = {},
            onOpenPackageBoard = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryDetailScreenErrorPreview() {
    GotestTheme {
        HistoryDetailScreen(
            uiState = HistoryDetailUiState(isLoading = false, loadFailed = true),
            onBack = {},
            onRetry = {},
            onOpenPackageBoard = {},
        )
    }
}
