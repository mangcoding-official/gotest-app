package com.gotest.app.feature.tryout.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.core.ui.StudentStatTiles
import com.gotest.app.feature.practice.ui.catalog.ActivityFilters
import com.gotest.app.feature.practice.ui.catalog.ActivityPanel
import com.gotest.app.feature.practice.ui.catalog.CatalogCategoryCard
import com.gotest.app.feature.practice.ui.catalog.Heading
import com.gotest.app.feature.practice.ui.catalog.shortDate
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.components.serverWindow
import com.gotest.app.feature.studypackage.ui.components.JenjangDropdown
import com.gotest.app.feature.tryout.domain.LeaderboardRow
import com.gotest.app.feature.tryout.domain.TryoutPackage
import com.gotest.app.feature.tryout.domain.TryoutWindow
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** Where the Try Out catalog leads. */
data class TryoutNavigation(
    val onOpenCategory: (categoryId: Long) -> Unit,
    val onOpenRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenResult: (attemptId: Long) -> Unit,
    /** "Lihat Hasil" of Aktivitas Terkini: the result per subtest, as Riwayat shows it. */
    val onOpenHistory: (attemptId: Long) -> Unit,
)

@Composable
private fun CatalogEffects(
    navigation: TryoutNavigation,
    effects: Flow<TryoutCatalogEffect>,
    onResume: () -> Unit,
) {
    LifecycleResumeEffect(onResume) {
        onResume()
        onPauseOrDispose { }
    }
    ObserveAsEvents(effects) { effect ->
        when (effect) {
            is TryoutCatalogEffect.OpenRunner -> navigation.onOpenRunner(effect.attemptId, effect.packageName)
            is TryoutCatalogEffect.OpenResult -> navigation.onOpenResult(effect.attemptId)
        }
    }
}

/** The start and session sheets a catalog level may show. */
@Composable
private fun CatalogSheets(
    uiState: TryoutCatalogUiState,
    sheetActions: SheetCallbacks,
    sessionActions: SessionCallbacks,
) {
    uiState.sheet?.let { sheet ->
        TryoutStartSheet(sheet = sheet, access = startAccess(uiState, sheet.item.premiumLocked), actions = sheetActions)
    }
    val session = uiState.openSession
    val sessionSheet = uiState.sessionSheet
    if (session != null && sessionSheet != null) {
        SessionDetailSheet(
            session = session,
            sheet = sessionSheet,
            access = startAccess(uiState, session.premiumLocked),
            actions = sessionActions,
        )
    }
}

private fun TryoutCatalogViewModel.sheetActions() = SheetCallbacks(onStart = ::start, onClose = ::closeSheet)

private fun TryoutCatalogViewModel.sessionActions() =
    SessionCallbacks(
        onRegister = ::register,
        onUnregister = ::unregister,
        onStart = ::startSession,
        onClose = ::closeSession,
    )

/** The front of Paket Try Out inside the Paket tab (web Tryout/Index). */
@Composable
fun TryoutCatalogFront(
    navigation: TryoutNavigation,
    modifier: Modifier = Modifier,
    viewModel: TryoutCatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogEffects(navigation, viewModel.effects, viewModel::refresh)
    val page = uiState.page
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.padding(vertical = GotestTheme.spacing.xxxl))
            }

            page == null -> {
                GotestErrorView(onRetry = viewModel::retry, modifier = Modifier.fillMaxWidth())
            }

            else -> {
                StudentStatTiles(page.stats.questionsDone, page.stats.accuracy, page.stats.packagesDone)
                FrontPanel(uiState = uiState, onOpenCategory = navigation.onOpenCategory)
                SeasonalTryout(uiState = uiState, onOpen = viewModel::openSession)
                ActivityPanel(
                    filters =
                        ActivityFilters(
                            categories = page.categories,
                            draftCategory = uiState.draftActivityCategory,
                            draftDate = uiState.draftDateFilter,
                            onDraftCategory = viewModel::onDraftActivityCategory,
                            onDraftDate = viewModel::onDraftDateFilter,
                            onApply = viewModel::applyActivityFilter,
                        ),
                    attempts = uiState.activity,
                    emptyCopy = stringResource(R.string.tryout_activity_empty_copy),
                    onView = navigation.onOpenHistory,
                )
            }
        }
        CatalogSheets(uiState, viewModel.sheetActions(), viewModel.sessionActions())
    }
}

@Composable
private fun FrontPanel(
    uiState: TryoutCatalogUiState,
    onOpenCategory: (Long) -> Unit,
) {
    val groups = uiState.categories
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        JenjangDropdown(
            options = groups.map { it.first.name },
            selectedIndex = null,
            onSelect = { index -> index?.let { onOpenCategory(groups[it].first.id) } },
            allOptionLabel = stringResource(R.string.practice_all_levels),
        )
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.tryout_empty),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        } else {
            groups.forEachIndexed { index, (category, packages) ->
                CatalogCategoryCard(
                    name = category.name,
                    description = category.description ?: stringResource(R.string.tryout_category_text, category.name),
                    index = index,
                    meta = listOf(stringResource(R.string.tryout_category_packages, packages.size)),
                    onOpen = { onOpenCategory(category.id) },
                )
            }
        }
    }
}

/** Seasonal Try Out: sessions in their order with the countdown to the running one's end or the next start. */
@Composable
private fun SeasonalTryout(
    uiState: TryoutCatalogUiState,
    onOpen: (com.gotest.app.feature.tryout.domain.TryoutSession) -> Unit,
) {
    val sessions = uiState.sessions
    if (sessions.isEmpty()) return
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Heading(stringResource(R.string.session_seasonal_title), stringResource(R.string.session_seasonal_lede))
        val target = uiState.countdownTarget
        val left = uiState.countdown
        if (target != null && left != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            ) {
                Text(
                    text =
                        stringResource(
                            if (target.state ==
                                com.gotest.app.feature.tryout.domain.TryoutSession.RUNNING
                            ) {
                                R.string.session_ends_in
                            } else {
                                R.string.session_starts_in
                            },
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
                Text(
                    text =
                        stringResource(
                            R.string.session_countdown,
                            (left / HOUR).toInt(),
                            ((left % HOUR) / MINUTE).toInt(),
                            (
                                left %
                                    MINUTE
                            ).toInt(),
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                )
            }
        }
        sessions.forEach { session -> SessionCard(session = session, onOpen = { onOpen(session) }) }
    }
}

/** A category with its packages (web TryoutCatalog, category level). */
@Composable
fun TryoutCategoryRoute(
    onBack: () -> Unit,
    navigation: TryoutNavigation,
    modifier: Modifier = Modifier,
    viewModel: TryoutCatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogEffects(navigation, viewModel.effects, viewModel::refresh)
    val group = uiState.categories.firstOrNull { it.first.id == uiState.categoryId }
    RuangScaffold(
        title = group?.first?.name ?: stringResource(R.string.tryout_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            uiState.page == null -> {
                GotestErrorView(onRetry = viewModel::retry, modifier = Modifier.fillMaxSize().padding(padding))
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
                    if (group == null) {
                        Text(
                            text = stringResource(R.string.tryout_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = GotestTheme.colors.textSecondary,
                        )
                    } else {
                        Text(
                            text =
                                group.first.description
                                    ?: stringResource(R.string.tryout_category_lede, group.first.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = GotestTheme.colors.textSecondary,
                        )
                        group.second.forEach { item ->
                            TryoutPackageCard(item = item, onStart = { viewModel.openSheet(item) })
                        }
                    }
                }
            }
        }
    }
    CatalogSheets(uiState, viewModel.sheetActions(), viewModel.sessionActions())
}

/** What a start may do for this account (web `useStartAccess('tryout')`). */
data class TryoutAccess(
    val canStart: Boolean,
    val note: String?,
    val blockedLabel: Int?,
    val premiumLocked: Boolean,
    /** A public session may be registered while the quota of its month lasts (review R-08). */
    val left: Int?,
)

@Composable
private fun startAccess(
    uiState: TryoutCatalogUiState,
    premiumLocked: Boolean,
): TryoutAccess = tryoutAccess(uiState.access, premiumLocked)

@Composable
internal fun tryoutAccess(
    known: com.gotest.app.feature.practice.domain.StartAccess?,
    premiumLocked: Boolean,
): TryoutAccess {
    val free = known?.freePlan
    val quota = free?.tryout
    return when {
        premiumLocked -> {
            TryoutAccess(
                false,
                stringResource(R.string.practice_premium_note),
                R.string.practice_premium_only,
                true,
                null,
            )
        }

        // `/akun` not read (it failed): Mulai stays on and the server says why if it refuses (A2c review, item 4).
        quota == null -> {
            TryoutAccess(known?.access?.active ?: true, null, null, false, null)
        }

        quota.left > 0 -> {
            TryoutAccess(
                true,
                stringResource(R.string.tryout_quota_left, quota.left, quota.limit),
                null,
                false,
                quota.left,
            )
        }

        else -> {
            TryoutAccess(
                false,
                stringResource(R.string.tryout_quota_used),
                R.string.tryout_quota_used_label,
                false,
                0,
            )
        }
    }
}

data class SheetCallbacks(
    val onStart: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The start sheet (web TryoutStartModal): subtests in order, the rules, the window, the access note, and the
 * package ranking. A Premium package for a Gratis account says why and its button stays off (A2c-3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryoutStartSheet(
    sheet: TryoutSheet,
    access: TryoutAccess,
    actions: SheetCallbacks,
    modifier: Modifier = Modifier,
) {
    val item = sheet.item
    GotestBottomSheet(onDismissRequest = actions.onClose, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Heading(item.name, item.description ?: stringResource(R.string.tryout_start_lede))
            if (item.sections.isNotEmpty()) SectionOrder(item)
            GotestCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = GotestTheme.colors.bgSubtle,
                verticalSpacing = GotestTheme.spacing.xs,
            ) {
                Text(
                    text = stringResource(R.string.tryout_rules),
                    style = MaterialTheme.typography.labelMedium,
                    color = GotestTheme.colors.textSecondary,
                )
                Text(
                    text = item.ruleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
            StartNotes(sheet = sheet, access = access)
            StartButtons(sheet = sheet, access = access, actions = actions)
            PackageBoard(item.leaderboard)
        }
    }
}

@Composable
private fun SectionOrder(item: TryoutPackage) {
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        Text(
            text = stringResource(R.string.tryout_order),
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        item.sections.forEach { section ->
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                Text(
                    text = section.title,
                    modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.tryout_section_questions, section.questionCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.tryout_section_minutes, section.durationMinutes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun StartNotes(
    sheet: TryoutSheet,
    access: TryoutAccess,
) {
    val item = sheet.item
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        sheet.error?.let {
            Text(
                text = problemText(it, R.string.tryout_start_failed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.statusDanger,
            )
        }
        windowNotice(item.window)?.let { notice ->
            val review =
                item.window?.reviewOpensAt?.let {
                    stringResource(
                        R.string.tryout_review_opens,
                        serverWindow(it),
                    )
                }
            Text(
                text = stringResource(R.string.tryout_schedule, notice) + (review?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (item.resultOnlyId != null) {
            Text(
                text = stringResource(R.string.tryout_once_done),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (access.note != null && item.resultOnlyId == null && item.continueAttemptId == null) {
            Text(
                text = access.note,
                style = MaterialTheme.typography.bodySmall,
                color = if (access.canStart) colors.textSecondary else colors.statusWarning,
            )
        }
    }
}

@Composable
private fun StartButtons(
    sheet: TryoutSheet,
    access: TryoutAccess,
    actions: SheetCallbacks,
) {
    val (label, enabled) = startButton(sheet, access)
    Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(
            text = stringResource(R.string.practice_close),
            onClick = actions.onClose,
            modifier = Modifier.weight(1f),
            variant = GotestButtonVariant.Plain,
        )
        GotestButton(
            text = stringResource(label),
            onClick = actions.onStart,
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
    }
}

/** The start button's words and whether it works (web TryoutStartModal). */
private fun startButton(
    sheet: TryoutSheet,
    access: TryoutAccess,
): Pair<Int, Boolean> {
    val item = sheet.item
    val ready = access.canStart || item.resultOnlyId != null || item.continueAttemptId != null
    return startLabel(sheet, access) to (ready && !sheet.isStarting && !windowBlocked(item) && !toPremium(item, access))
}

private fun toPremium(
    item: TryoutPackage,
    access: TryoutAccess,
): Boolean = access.premiumLocked && item.resultOnlyId == null && item.continueAttemptId == null && !windowBlocked(item)

private fun startLabel(
    sheet: TryoutSheet,
    access: TryoutAccess,
): Int {
    val item = sheet.item
    return when {
        sheet.isStarting -> R.string.session_starting
        item.resultOnlyId != null -> R.string.tryout_see_result
        item.continueAttemptId != null -> R.string.tryout_continue
        item.windowState == TryoutWindow.STATE_UPCOMING -> R.string.tryout_upcoming
        item.windowState == TryoutWindow.STATE_CLOSED -> R.string.tryout_closed
        else -> access.blockedLabel?.takeIf { toPremium(item, access) || !access.canStart } ?: R.string.tryout_start
    }
}

/** "Peringkat": the first attempt of every participant on this package (web TryoutStartModal aside). */
@Composable
private fun PackageBoard(rows: List<LeaderboardRow>) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Heading(stringResource(R.string.tryout_leaderboard), stringResource(R.string.tryout_leaderboard_lede))
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.tryout_leaderboard_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        rows.forEach { row ->
            BoardLine(
                rank = row.rank,
                name = row.name,
                meta = listOfNotNull(shortDate(row.submittedAt), durationText(row.durationUsed)).joinToString(" · "),
                score = row.score,
            )
        }
    }
}

@Composable
internal fun BoardLine(
    rank: Int,
    name: String,
    meta: String,
    score: Double,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleSmall,
            color =
                if (rank <=
                    TOP
                ) {
                    colors.statusWarning
                } else {
                    colors.textPrimary
                },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1)
            Text(text = meta, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.tryout_leaderboard_score),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = score.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (rank <= TOP) colors.statusWarning else colors.textPrimary,
            )
        }
    }
}

/** Web `formatTryoutDuration`: "75 Menit 20 Detik". */
@Composable
internal fun durationText(seconds: Int): String {
    val minutes = seconds / SECONDS_PER_MINUTE
    val rest = seconds % SECONDS_PER_MINUTE
    return when {
        minutes <= 0 -> stringResource(R.string.tryout_duration_seconds, rest)
        rest == 0 -> stringResource(R.string.tryout_duration_minutes, minutes)
        else -> stringResource(R.string.tryout_duration_both, minutes, rest)
    }
}

@Composable
internal fun problemText(
    problem: TryoutProblem,
    fallback: Int,
): String =
    when (problem) {
        is TryoutProblem.Server -> problem.message
        TryoutProblem.Offline -> stringResource(R.string.runner_offline)
        TryoutProblem.Fallback -> stringResource(fallback)
    }

@Composable
internal fun SheetNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    NoticeBox(text = text, modifier = modifier, tone = NoticeTone.Info)
}

private const val HOUR = 3_600L
private const val MINUTE = 60L
private const val TOP = 3
private const val SECONDS_PER_MINUTE = 60
