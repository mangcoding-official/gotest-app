package com.gotest.app.feature.history.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.component.GotestTrackedProgress
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.StudentStatTiles
import com.gotest.app.core.ui.categoryTone
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

/** Web `StudentStatCards`: Soal Dikerjakan, Akurasi Jawaban, Paket Selesai. */
@Composable
internal fun HistoryStatTiles(
    stats: HistoryStats,
    modifier: Modifier = Modifier,
) = StudentStatTiles(stats.questionsDone, stats.accuracy, stats.packagesDone, modifier)

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            HistorySelectDropdown(
                options = HistoryMode.entries.map { it to stringResource(it.label()) },
                selected = draft.mode,
                onSelect = { onDraftChange(draft.copy(mode = it)) },
                modifier = Modifier.weight(1f),
            )
            val categoryOptions =
                listOf(null to stringResource(R.string.history_category_all)) +
                    categories.map { it.id to it.name }
            HistorySelectDropdown(
                options = categoryOptions,
                selected = draft.category,
                onSelect = { onDraftChange(draft.copy(category = it)) },
                modifier = Modifier.weight(1f),
            )
            HistorySelectDropdown(
                options = HistorySort.entries.map { it to stringResource(it.label()) },
                selected = draft.sort,
                onSelect = { onDraftChange(draft.copy(sort = it)) },
                modifier = Modifier.weight(1f),
            )
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

@Composable
private fun <T> HistorySelectDropdown(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val selectedText = options.firstOrNull { it.first == selected }?.second.orEmpty()
    val colors = GotestTheme.colors
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { anchorWidthPx = it.width }
                    .semantics { role = Role.DropdownList },
            shape = MaterialTheme.shapes.small,
            color = colors.bgApp,
            border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
        ) {
            Row(
                modifier =
                    Modifier
                        .heightIn(min = GotestTheme.sizes.inputHeight)
                        .padding(horizontal = GotestTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(GotestIcons.Bold.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconSm),
                    tint = colors.iconMuted,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { anchorWidthPx.toDp() }),
            shape = MaterialTheme.shapes.small,
            containerColor = colors.bgApp,
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                DropdownMenuItem(
                    text = {
                        val typography = MaterialTheme.typography
                        Text(
                            text = label,
                            style = if (isSelected) typography.labelMedium else typography.bodySmall,
                            color = if (isSelected) colors.textBrandDark else colors.textPrimary,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
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
