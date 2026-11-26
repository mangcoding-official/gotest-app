package com.gotest.app.feature.practice.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.core.ui.StudentStatTiles
import com.gotest.app.core.ui.categoryTone
import com.gotest.app.feature.practice.domain.CatalogCategory
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.studypackage.ui.components.JenjangDropdown
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** Where the catalog leads. */
data class PracticeNavigation(
    val onOpenCategory: (categoryId: Long) -> Unit,
    val onOpenSubject: (categoryId: Long, subjectId: Long) -> Unit,
    val onOpenRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenResult: (attemptId: Long, packageName: String) -> Unit,
    /** "Lihat Hasil" of Aktivitas Terkini: the result per subtest, as Riwayat shows it. */
    val onOpenHistory: (attemptId: Long) -> Unit,
)

/** A catalog level's effects: refresh on return and where a start leads. */
@Composable
private fun CatalogEffects(
    navigation: PracticeNavigation,
    effects: Flow<PracticeCatalogEffect>,
    onResume: () -> Unit,
) {
    LifecycleResumeEffect(onResume) {
        onResume()
        onPauseOrDispose { }
    }
    ObserveAsEvents(effects) { effect ->
        when (effect) {
            is PracticeCatalogEffect.OpenRunner -> navigation.onOpenRunner(effect.attemptId, effect.packageName)
            is PracticeCatalogEffect.OpenResult -> navigation.onOpenResult(effect.attemptId, effect.packageName)
        }
    }
}

/** What the start sheet calls back. */
data class SheetActions(
    val onUseTimer: (Boolean) -> Unit,
    val onStart: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
private fun CatalogSheet(
    uiState: PracticeCatalogUiState,
    actions: SheetActions,
) {
    uiState.sheet?.let { sheet ->
        PracticeStartSheet(sheet = sheet, access = startAccess(uiState, sheet.item), actions = actions)
    }
}

private fun PracticeCatalogViewModel.sheetActions() = SheetActions(::onUseTimer, ::start, ::closeSheet)

/** The front of Paket Latihan inside the Paket tab (web Practice/Index): stats, categories, Aktivitas Terkini. */
@Composable
fun PracticeCatalogFront(
    navigation: PracticeNavigation,
    modifier: Modifier = Modifier,
    viewModel: PracticeCatalogViewModel = hiltViewModel(),
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
                    emptyCopy = stringResource(R.string.practice_activity_empty_copy),
                    onView = navigation.onOpenHistory,
                )
            }
        }
        CatalogSheet(uiState, viewModel.sheetActions())
    }
}

@Composable
private fun FrontPanel(
    uiState: PracticeCatalogUiState,
    onOpenCategory: (Long) -> Unit,
) {
    val categories = uiState.categories
    // The Paket tab's bar names the page with the web's title and lede (PracticeCatalog front).
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        JumpSelect(categories = categories, selectedId = null, onSelect = { id -> id?.let(onOpenCategory) })
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.practice_empty),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        } else {
            categories.forEachIndexed { index, group ->
                CategoryCard(group = group, index = index, onOpen = { onOpenCategory(group.id) })
            }
        }
    }
}

/** "Semua Jenjang" and the categories with Latihan; choosing one opens it. */
@Composable
private fun JumpSelect(
    categories: List<CategoryGroup>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    JenjangDropdown(
        options = categories.map { it.name },
        selectedIndex = categories.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 },
        onSelect = { index -> onSelect(index?.let { categories[it].id }) },
        allOptionLabel = stringResource(R.string.practice_all_levels),
    )
}

@Composable
internal fun Heading(
    title: String,
    lede: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = GotestTheme.colors.textPrimary)
        Text(text = lede, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

/** A category with its subtests (web PracticeCatalog, category level). */
@Composable
fun PracticeCategoryRoute(
    onBack: () -> Unit,
    navigation: PracticeNavigation,
    modifier: Modifier = Modifier,
    viewModel: PracticeCatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogEffects(navigation, viewModel.effects, viewModel::refresh)
    val category = uiState.category
    CatalogLevel(
        uiState = uiState,
        title = category?.name ?: stringResource(R.string.practice_title),
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    ) {
        if (category == null) {
            EmptyLevel()
        } else {
            Text(
                text = category.description ?: stringResource(R.string.practice_category_lede, category.name),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
            JumpSelect(
                categories = uiState.categories,
                selectedId = category.id,
                onSelect = { id ->
                    if (id == null) {
                        onBack()
                    } else if (id != category.id) {
                        navigation.onOpenCategory(id)
                    }
                },
            )
            category.subjects.forEach { group ->
                SubjectCard(group = group, onOpen = { navigation.onOpenSubject(category.id, group.subject.id) })
            }
        }
    }
}

/** A subtest with its packages (web PracticeCatalog, subject level). */
@Composable
fun PracticeSubjectRoute(
    onBack: () -> Unit,
    navigation: PracticeNavigation,
    modifier: Modifier = Modifier,
    viewModel: PracticeCatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogEffects(navigation, viewModel.effects, viewModel::refresh)
    val category = uiState.category
    val subject = uiState.subject
    CatalogLevel(
        uiState = uiState,
        title = subject?.let { subjectName(it) } ?: stringResource(R.string.practice_title),
        subtitle = category?.name,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    ) {
        if (category == null || subject == null) {
            EmptyLevel()
        } else {
            Text(
                text =
                    subject.subject.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.practice_subject_lede, subjectName(subject), category.name),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
            subject.packages.forEach { item ->
                PracticePackageCard(item = item, onStart = { viewModel.openSheet(item) })
            }
        }
        CatalogSheet(uiState, viewModel.sheetActions())
    }
}

@Composable
private fun CatalogLevel(
    uiState: PracticeCatalogUiState,
    title: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    RuangScaffold(title = title, subtitle = subtitle, onBack = onBack, modifier = modifier) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            uiState.page == null -> {
                GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize().padding(padding))
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
                ) { content() }
            }
        }
    }
}

/** A level whose category or subtest no longer has Latihan: the web falls back to the front. */
@Composable
private fun EmptyLevel() {
    Text(
        text = stringResource(R.string.practice_empty),
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.textSecondary,
    )
}

/** The filters of Aktivitas Terkini: drafts until "Filter" is pressed (web ActivityPanel). */
data class ActivityFilters(
    val categories: List<CatalogCategory>,
    val draftCategory: String?,
    val draftDate: DateFilter,
    val onDraftCategory: (String?) -> Unit,
    val onDraftDate: (DateFilter) -> Unit,
    val onApply: () -> Unit,
)

/** Aktivitas Terkini: filters applied with "Filter", then the latest results (web ActivityPanel). */
@Composable
internal fun ActivityPanel(
    filters: ActivityFilters,
    attempts: List<RecentAttempt>,
    emptyCopy: String,
    onView: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = filters.categories
    val dates = DateFilter.entries
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Heading(stringResource(R.string.practice_activity_title), stringResource(R.string.practice_activity_lede))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            JenjangDropdown(
                options = categories.map { it.name },
                selectedIndex = categories.indexOfFirst { it.name == filters.draftCategory }.takeIf { it >= 0 },
                onSelect = { index -> filters.onDraftCategory(index?.let { categories[it].name }) },
                allOptionLabel = stringResource(R.string.practice_activity_all_levels),
                modifier = Modifier.weight(1f),
            )
            JenjangDropdown(
                options = dates.drop(1).map { stringResource(dateLabel(it)) },
                selectedIndex = dates.indexOf(filters.draftDate).takeIf { it > 0 }?.minus(1),
                onSelect = { index -> filters.onDraftDate(index?.let { dates[it + 1] } ?: DateFilter.All) },
                allOptionLabel = stringResource(R.string.practice_activity_date_all),
                modifier = Modifier.weight(1f),
            )
        }
        GotestButton(
            text = stringResource(R.string.practice_activity_filter),
            onClick = filters.onApply,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
        if (attempts.isEmpty()) {
            Text(
                text = stringResource(R.string.practice_activity_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            Text(text = emptyCopy, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        } else {
            attempts.forEach { attempt -> ActivityRow(attempt = attempt, onView = { onView(attempt.id) }) }
        }
    }
}

@Composable
private fun ActivityRow(
    attempt: RecentAttempt,
    onView: () -> Unit,
) {
    val category = attempt.categoryName ?: stringResource(R.string.practice_general_subject)
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        GotestChip(text = category, tone = categoryTone(category))
        Text(
            text = attempt.packageName,
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text =
                stringResource(
                    R.string.practice_activity_done_on,
                    longDate(attempt.submittedAt) ?: stringResource(R.string.practice_activity_not_yet),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                    stringResource(
                        R.string.practice_activity_score,
                        attempt.scoreLabel ?: stringResource(R.string.practice_score_fallback),
                        (attempt.score ?: 0.0).roundToInt(),
                    ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = GotestTheme.colors.textPrimary,
            )
            GotestButton(
                text = stringResource(R.string.practice_activity_open),
                onClick = onView,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
    }
}

private fun dateLabel(filter: DateFilter): Int =
    when (filter) {
        DateFilter.Days7 -> R.string.practice_activity_date_7d
        DateFilter.Days30 -> R.string.practice_activity_date_30d
        DateFilter.Year -> R.string.practice_activity_date_year
        DateFilter.All -> R.string.practice_activity_date_all
    }

/** What the start sheet may do for this account (web `useStartAccess`). */
data class SheetAccess(
    val canStart: Boolean,
    /** The quota line, the quota-used sentence, or the Premium sentence. */
    val note: String?,
    val blockedLabel: Int?,
    val premiumLocked: Boolean,
)

@Composable
internal fun startAccess(
    uiState: PracticeCatalogUiState,
    item: PracticePackage,
): SheetAccess {
    val free = uiState.access?.freePlan
    val left = free?.practice?.left ?: 0
    return when {
        item.premiumLocked -> {
            SheetAccess(false, stringResource(R.string.practice_premium_note), R.string.practice_premium_only, true)
        }

        // `/akun` not read (it failed): Mulai stays on and the server says why if it refuses (A2c review, item 4).
        free == null -> {
            SheetAccess(uiState.access?.access?.active ?: true, null, null, false)
        }

        left > 0 -> {
            SheetAccess(true, stringResource(R.string.practice_quota_left, left, free.practice.limit), null, false)
        }

        else -> {
            SheetAccess(false, stringResource(R.string.practice_quota_used), R.string.practice_quota_used_label, false)
        }
    }
}

/**
 * The start sheet (web PracticeStartModal): composition, timer setting with the optional-timer choice, the access
 * note, and Mulai, Lanjutkan, or Lihat Pembahasan. The price list is not in the app (K-AN2): a Premium package for
 * a Gratis account shows why and its button stays off (A2c-3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeStartSheet(
    sheet: StartSheet,
    access: SheetAccess,
    actions: SheetActions,
    modifier: Modifier = Modifier,
) {
    val item = sheet.item
    val review = item.reviewAttemptId
    val continuing = item.continueAttemptId != null
    val canProceed = access.canStart || continuing || review != null
    val toPremium = access.premiumLocked && !continuing && review == null
    GotestBottomSheet(onDismissRequest = actions.onClose, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Heading(item.name, item.description ?: stringResource(R.string.practice_start_lede))
            if (item.composition.isNotEmpty()) Composition(item)
            TimerSetting(sheet = sheet, onUseTimer = actions.onUseTimer)
            SheetNotes(sheet = sheet, access = access)
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.practice_close),
                    onClick = actions.onClose,
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                )
                GotestButton(
                    text = stringResource(sheetLabel(sheet, access, toPremium)),
                    onClick = actions.onStart,
                    modifier = Modifier.weight(1f),
                    enabled = canProceed && !toPremium && !sheet.isStarting,
                )
            }
        }
    }
}

@Composable
private fun TimerSetting(
    sheet: StartSheet,
    onUseTimer: (Boolean) -> Unit,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.bgSubtle,
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(
            text = stringResource(R.string.practice_timer_setting),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Text(text = sheet.item.timerLabel, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        if (offersTimerChoice(sheet.item)) TimerChoice(checked = sheet.useTimer, onChange = onUseTimer)
    }
}

/** The refusal, the "satu kali" note, and the access note (only for a new start). */
@Composable
private fun SheetNotes(
    sheet: StartSheet,
    access: SheetAccess,
) {
    val colors = GotestTheme.colors
    val review = sheet.item.reviewAttemptId != null
    val newStart = sheet.item.continueAttemptId == null && !review
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        sheet.error?.let {
            Text(text = problemText(it), style = MaterialTheme.typography.bodySmall, color = colors.statusDanger)
        }
        if (review) {
            Text(
                text = stringResource(R.string.practice_once_only),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (access.note != null && newStart) {
            Text(
                text = access.note,
                style = MaterialTheme.typography.bodySmall,
                color = if (access.canStart) colors.textSecondary else colors.statusWarning,
            )
        }
    }
}

private fun sheetLabel(
    sheet: StartSheet,
    access: SheetAccess,
    toPremium: Boolean,
): Int =
    when {
        sheet.isStarting -> R.string.practice_starting
        sheet.item.continueAttemptId != null -> R.string.practice_continue
        sheet.item.reviewAttemptId != null -> R.string.practice_review
        toPremium -> access.blockedLabel ?: R.string.practice_premium_only
        else -> R.string.practice_start
    }

@Composable
internal fun problemText(problem: StartProblem): String =
    when (problem) {
        is StartProblem.Server -> problem.message
        StartProblem.Offline -> stringResource(R.string.runner_offline)
        StartProblem.Fallback -> stringResource(R.string.practice_start_failed)
    }

@Composable
private fun Composition(item: PracticePackage) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        Text(
            text = stringResource(R.string.practice_composition),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        CompositionRow(
            stringResource(R.string.practice_composition_subject),
            stringResource(R.string.practice_composition_count),
            stringResource(R.string.practice_composition_difficulty),
            header = true,
        )
        item.composition.forEach { row ->
            CompositionRow(
                row.subject,
                stringResource(R.string.practice_composition_questions, row.questionCount),
                row.difficulty,
            )
        }
    }
}

@Composable
private fun CompositionRow(
    subject: String,
    count: String,
    difficulty: String,
    header: Boolean = false,
) {
    val color = if (header) GotestTheme.colors.textSecondary else GotestTheme.colors.textPrimary
    Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(text = subject, modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = color)
        Text(text = count, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = color)
        Text(
            text = difficulty,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun TimerChoice(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = GotestTheme.colors.bgBrandDark),
        )
        Column(modifier = Modifier.padding(top = GotestTheme.spacing.sm)) {
            Text(
                text = stringResource(R.string.practice_use_timer),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.practice_use_timer_note),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PracticeStartSheetPreview() {
    GotestTheme {
        Column {
            PracticePackageCard(
                item =
                    PracticePackage(
                        1,
                        "Latihan Penalaran Umum 1",
                        timerLabel = "Waktu total: 10 menit",
                        topicIds = listOf(1, 2),
                    ),
                onStart = {},
            )
        }
    }
}
