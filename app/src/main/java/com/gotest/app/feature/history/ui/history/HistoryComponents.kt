package com.gotest.app.feature.history.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.component.GotestTrackedProgress
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.domain.HistoryCategory
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryMode
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.domain.HistorySort
import com.gotest.app.feature.history.domain.HistoryStats
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** The phone's own date of a server time, as the web's `new Date(value)` does; null when unreadable. */
internal fun localDate(value: String?): LocalDate? =
    value?.let {
        try {
            OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
        } catch (_: DateTimeParseException) {
            null
        }
    }

/** Web `groupAttemptsByMonth`: buckets in list order; results without a date share one "Tanpa tanggal" bucket. */
internal fun groupByMonth(results: List<HistoryResult>): List<Pair<YearMonth?, List<HistoryResult>>> =
    results
        .groupBy { result -> localDate(result.submittedAt)?.let(YearMonth::from) }
        .toList()

/** Web `categoryBadgeClass`: SMA green, UTBK/SNBT blue, others violet. */
internal fun categoryTone(name: String): GotestChipTone {
    val value = name.lowercase()
    return when {
        "sma" in value -> GotestChipTone.Green
        "utbk" in value || "snbt" in value -> GotestChipTone.Blue
        else -> GotestChipTone.Violet
    }
}

/** Web `StudentStatCards`: Soal Dikerjakan, Akurasi Jawaban, Paket Selesai. */
@Composable
internal fun HistoryStatTiles(
    stats: HistoryStats,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        StatTile(
            GotestFormat.number(stats.questionsDone),
            stringResource(R.string.history_stat_questions),
            Modifier.weight(1f),
        )
        StatTile(
            stringResource(R.string.history_percent, stats.accuracy),
            stringResource(R.string.history_stat_accuracy),
            Modifier.weight(1f),
        )
        StatTile(
            GotestFormat.number(stats.packagesDone),
            stringResource(R.string.history_stat_packages),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(GotestTheme.spacing.md),
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = GotestTheme.colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/** Web `HistoryPanel` filters: search, package type, level, order; nothing changes until "Filter". */
@Composable
internal fun HistoryFilterPanel(
    categories: List<HistoryCategory>,
    draft: HistoryFilters,
    busy: Boolean,
    onDraftChange: (HistoryFilters) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        GotestTextField(
            value = draft.q,
            onValueChange = { onDraftChange(draft.copy(q = it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.history_search),
            leadingIcon = GotestIcons.Linear.SearchNormal,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onApply() }),
        )
        PillRow {
            HistoryMode.entries.forEach { mode ->
                GotestFilterPill(
                    text = stringResource(mode.label()),
                    selected = draft.mode == mode,
                    onClick = { onDraftChange(draft.copy(mode = mode)) },
                )
            }
        }
        PillRow {
            GotestFilterPill(
                text = stringResource(R.string.history_category_all),
                selected = draft.category == null,
                onClick = { onDraftChange(draft.copy(category = null)) },
            )
            categories.forEach { category ->
                GotestFilterPill(
                    text = category.name,
                    selected = draft.category == category.id,
                    onClick = { onDraftChange(draft.copy(category = category.id)) },
                )
            }
        }
        PillRow {
            HistorySort.entries.forEach { sort ->
                GotestFilterPill(
                    text = stringResource(sort.label()),
                    selected = draft.sort == sort,
                    onClick = { onDraftChange(draft.copy(sort = sort)) },
                )
            }
        }
        GotestButton(
            text = stringResource(R.string.history_filter),
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            size = GotestButtonSize.Medium,
            enabled = !busy,
        )
    }
}

private fun HistoryMode.label(): Int =
    when (this) {
        HistoryMode.All -> R.string.history_mode_all
        HistoryMode.Practice -> R.string.history_mode_practice
        HistoryMode.Tryout -> R.string.history_mode_tryout
    }

private fun HistorySort.label(): Int =
    when (this) {
        HistorySort.Newest -> R.string.history_sort_newest
        HistorySort.Oldest -> R.string.history_sort_oldest
    }

@Composable
private fun PillRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) { content() }
}

/** "November 2026", or "Tanpa tanggal". */
@Composable
internal fun MonthLabel(
    month: YearMonth?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = month?.let { GotestFormat.monthYear(it.atDay(1)) } ?: stringResource(R.string.history_no_date),
        modifier = modifier.padding(top = GotestTheme.spacing.xs),
        style = MaterialTheme.typography.labelLarge,
        color = GotestTheme.colors.textPrimary,
    )
}

/** One result as on the web: name, session, date and duration, type and level, accuracy, "Lihat Hasil". */
@Composable
internal fun HistoryResultRow(
    result: HistoryResult,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.sm) {
        Column {
            Text(text = result.packageName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            result.sessionTitle?.let {
                Text(
                    text = stringResource(R.string.history_session, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Text(
                text =
                    listOf(
                        localDate(result.submittedAt)?.let(GotestFormat::mediumDate)
                            ?: stringResource(R.string.history_date_missing),
                        result.durationLabel,
                    ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            GotestChip(
                text =
                    stringResource(
                        if (result.isTryout) R.string.history_type_tryout else R.string.history_type_practice,
                    ),
                tone = if (result.isTryout) GotestChipTone.Green else GotestChipTone.Violet,
            )
            GotestChip(text = result.categoryName, tone = categoryTone(result.categoryName))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.history_accuracy),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            GotestTrackedProgress(
                progress = result.accuracy.coerceIn(0, PERCENT) / PERCENT.toFloat(),
                modifier = Modifier.weight(1f),
            )
        }
        GotestDivider()
        GotestButton(
            text = stringResource(R.string.history_view_result),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

private const val PERCENT = 100

@Preview(showBackground = true)
@Composable
private fun HistoryResultRowPreview() {
    GotestTheme {
        HistoryResultRow(
            result =
                HistoryResult(
                    id = 1,
                    packageName = "Try Out UTBK-SNBT 3",
                    sessionTitle = "Try Out Bersama November",
                    categoryName = "UTBK-SNBT",
                    mode = "tryout",
                    submittedAt = "2026-11-17T08:30:00+07:00",
                    durationLabel = "95 Menit 12 Detik",
                    accuracy = 71,
                ),
            onOpen = {},
        )
    }
}
