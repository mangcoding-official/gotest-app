package com.gotest.app.feature.tryout.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.ui.result.formatScore
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.components.serverWindow
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutResult
import com.gotest.app.feature.tryout.domain.TryoutResultSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@Immutable
data class TryoutResultUiState(
    val isLoading: Boolean = true,
    val result: TryoutResult? = null,
)

/** A Try Out result: score, per subtest, and ranking; no key or pembahasan (web Tryout/Result, DEC-074). */
@HiltViewModel
class TryoutResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TryoutRepository,
    ) : ViewModel() {
        private val attemptId: Long = checkNotNull(savedStateHandle[ARG_ATTEMPT])

        private val _uiState = MutableStateFlow(TryoutResultUiState())
        val uiState: StateFlow<TryoutResultUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val result = repository.result(attemptId).getOrNull()
                _uiState.update { it.copy(isLoading = false, result = result) }
            }
        }

        companion object {
            const val ARG_ATTEMPT = "attemptId"
        }
    }

/** Where "Lihat Peringkat" leads: the package ranking, or the session's (web `leaderboardHref`). */
data class TryoutResultNavigation(
    val onBack: () -> Unit,
    val onOpenPackageBoard: (packageId: Long) -> Unit,
    val onOpenSession: (sessionId: Long) -> Unit,
)

@Composable
fun TryoutResultRoute(
    navigation: TryoutResultNavigation,
    modifier: Modifier = Modifier,
    viewModel: TryoutResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TryoutResultScreen(uiState = uiState, navigation = navigation, onRetry = viewModel::load, modifier = modifier)
}

@Composable
fun TryoutResultScreen(
    uiState: TryoutResultUiState,
    navigation: TryoutResultNavigation,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = uiState.result
    RuangScaffold(
        title =
            result?.let { stringResource(R.string.tres_title, it.packageName) } ?: stringResource(R.string.trun_kicker),
        subtitle = stringResource(R.string.tres_lede),
        onBack = navigation.onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            result == null -> {
                GotestErrorView(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = stringResource(R.string.tres_failed),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = GotestTheme.spacing.screenHorizontal,
                                vertical = GotestTheme.spacing.lg,
                            ),
                    verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
                ) {
                    ScoreHero(result = result, navigation = navigation)
                    Notices(result)
                    Sections(result)
                }
            }
        }
    }
}

@Composable
private fun pendingText(result: TryoutResult): String? {
    if (result.rankingAvailable != false) return null
    val at = result.rankingAvailableAt?.let(::serverWindow)
    return when {
        result.sessionId != null && at != null -> stringResource(R.string.tres_pending_session_at, at)
        result.sessionId != null -> stringResource(R.string.tres_pending_session)
        at != null -> stringResource(R.string.tres_pending_package_at, at)
        else -> stringResource(R.string.tres_pending_package)
    }
}

@Composable
private fun ScoreHero(
    result: TryoutResult,
    navigation: TryoutResultNavigation,
) {
    val colors = GotestTheme.colors
    val total = result.correctCount + result.wrongCount + result.emptyCount
    val percent = { value: Int -> if (total > 0) (value * PERCENT / total.toDouble()).roundToInt() else 0 }
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        BrandScore(result)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            CountTile(
                stringResource(R.string.review_correct),
                result.correctCount,
                percent(result.correctCount),
                colors.statusSuccess,
                Modifier.weight(1f),
            )
            CountTile(
                stringResource(R.string.review_wrong),
                result.wrongCount,
                percent(result.wrongCount),
                colors.statusDanger,
                Modifier.weight(1f),
            )
            CountTile(
                stringResource(R.string.review_empty),
                result.emptyCount,
                percent(result.emptyCount),
                colors.textSecondary,
                Modifier.weight(1f),
            )
        }
        AnswerBar(result.correctCount, result.wrongCount, result.emptyCount)
        Text(
            text = stringResource(R.string.tres_total_questions, total),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        val boardAction =
            when {
                result.rankingAvailable != true -> null
                result.sessionId != null -> ({ navigation.onOpenSession(result.sessionId) })
                result.packageId != null -> ({ navigation.onOpenPackageBoard(result.packageId) })
                else -> null
            }
        boardAction?.let {
            GotestButton(
                text = stringResource(R.string.tres_see_ranking),
                onClick = it,
                variant = GotestButtonVariant.Plain,
            )
        }
    }
}

/** Total Skor with its scale and the ranking line, on the brand colour. */
@Composable
private fun BrandScore(result: TryoutResult) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.bgBrandDark,
        showBorder = false,
        verticalSpacing = GotestTheme.spacing.sm,
    ) {
        Text(
            text = stringResource(R.string.tres_total),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textOnBrand,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            Text(
                text = result.score.roundToInt().toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textOnBrand,
            )
            result.scoringProfile?.scale?.let {
                Text(
                    text = stringResource(R.string.tres_scale, formatScore(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textOnBrand,
                )
            }
        }
        Text(
            text = result.scoreLabel ?: stringResource(R.string.tres_score_label),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textOnBrand,
        )
        Text(text = rankLine(result), style = MaterialTheme.typography.bodyMedium, color = colors.textOnBrand)
    }
}

@Composable
private fun rankLine(result: TryoutResult): String {
    val rank = result.rank
    val total = result.totalParticipants
    return when {
        rank != null && total != null -> stringResource(R.string.tres_rank, rank, total)
        result.disqualified -> stringResource(R.string.tres_disqualified_short)
        result.ranked == false -> stringResource(R.string.tres_retry_short)
        pendingText(result) != null -> stringResource(R.string.tres_pending_short)
        else -> stringResource(R.string.tres_not_ranked)
    }
}

@Composable
private fun CountTile(
    label: String,
    value: Int,
    percent: Int,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        showBorder = false,
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall, color = tone)
        Text(
            text = stringResource(R.string.tres_percent, percent),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun AnswerBar(
    correct: Int,
    wrong: Int,
    empty: Int,
    modifier: Modifier = Modifier,
) {
    val total = (correct + wrong + empty).coerceAtLeast(1)
    val colors = GotestTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(colors.bgSubtle),
    ) {
        if (correct >
            0
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(
                            correct.toFloat() / total,
                        ).fillMaxHeight()
                        .background(colors.statusSuccess),
            )
        }
        if (wrong >
            0
        ) {
            Box(modifier = Modifier.weight(wrong.toFloat() / total).fillMaxHeight().background(colors.statusDanger))
        }
        if (empty >
            0
        ) {
            Box(modifier = Modifier.weight(empty.toFloat() / total).fillMaxHeight().background(colors.borderStrong))
        }
    }
}

@Composable
private fun Notices(result: TryoutResult) {
    val pending = pendingText(result)
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        when {
            result.disqualified -> NoticeBox(text = stringResource(R.string.tres_disqualified))
            pending != null -> NoticeBox(text = pending, tone = NoticeTone.Info)
        }
        if (result.ranked == false && !result.disqualified) NoticeBox(text = stringResource(R.string.tres_retry))
        NoticeBox(text = stringResource(R.string.tres_review), tone = NoticeTone.Info)
    }
}

@Composable
private fun Sections(result: TryoutResult) {
    val colors = GotestTheme.colors
    val weighted = result.scoringProfile?.weighted == true
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.tres_sections),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.tres_sections_lede),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (result.sections.isEmpty()) {
            Text(
                text = stringResource(R.string.tres_sections_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        result.sections.forEach { section ->
            SectionCard(
                section = section,
                maxScore = if (weighted) result.scoringProfile.scale ?: WEIGHTED_SCALE else section.maxScore,
            )
        }
    }
}

@Composable
private fun SectionCard(
    section: TryoutResultSection,
    maxScore: Double,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = section.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text =
                    if (maxScore > 0) {
                        stringResource(
                            R.string.tres_section_score,
                            section.sectionScore.roundToInt(),
                            formatScore(maxScore),
                        )
                    } else {
                        section.sectionScore.roundToInt().toString()
                    },
                style = MaterialTheme.typography.titleSmall,
                color = colors.textBrandDark,
            )
        }
        AnswerBar(section.correctCount, section.wrongCount, section.emptyCount)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            Legend(colors.statusSuccess, stringResource(R.string.review_correct), section.correctCount)
            Legend(colors.statusDanger, stringResource(R.string.review_wrong), section.wrongCount)
            Legend(colors.borderStrong, stringResource(R.string.review_empty), section.emptyCount)
        }
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
    value: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

private const val PERCENT = 100
private const val WEIGHTED_SCALE = 1000.0
