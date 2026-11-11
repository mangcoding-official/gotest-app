package com.gotest.app.feature.history.ui.history

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.domain.ExportFormat
import com.gotest.app.feature.history.domain.ExportPeriod
import com.gotest.app.feature.history.ui.components.HistoryDimens
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The open Riwayat sheet. "Reset" and "Terapkan" slide the sort sheet away before reporting;
 * "Unduh File" keeps the export sheet open until a destination is picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorySheet(
    state: HistorySheetUiState,
    onEvent: (HistoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (HistoryUiEvent) -> Unit = { event ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { onEvent(event) }
    }
    GotestBottomSheet(
        onDismissRequest = { onEvent(HistoryUiEvent.SheetDismiss) },
        modifier = modifier,
        sheetState = sheetState,
    ) {
        when (state) {
            is HistorySheetUiState.Sort -> {
                HistorySortSheetContent(
                    selected = state.selected,
                    onSelect = { onEvent(HistoryUiEvent.SortSelect(it)) },
                    onReset = { hideThen(HistoryUiEvent.SortReset) },
                    onApply = { hideThen(HistoryUiEvent.SortApply) },
                )
            }

            is HistorySheetUiState.Export -> {
                HistoryExportSheetContent(
                    state = state,
                    onEvent = onEvent,
                    onCancel = { hideThen(HistoryUiEvent.SheetDismiss) },
                )
            }
        }
    }
}

/** "Urutkan": one radio row per [HistorySort], then "Reset" and "Terapkan". */
@Composable
internal fun HistorySortSheetContent(
    selected: HistorySort,
    onSelect: (HistorySort) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetColumn(modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.history_sort_title),
            subtitle = stringResource(R.string.history_sort_subtitle),
        )
        OptionCard {
            HistorySort.entries.forEach { sort ->
                SortOption(
                    label = stringResource(sort.labelRes),
                    selected = sort == selected,
                    onClick = { onSelect(sort) },
                )
            }
        }
        SheetActions(
            secondaryText = stringResource(R.string.history_sort_reset),
            onSecondary = onReset,
            primaryText = stringResource(R.string.history_sort_apply),
            onPrimary = onApply,
        )
    }
}

/** "Ekspor Riwayat": period pills, format radios, what will be exported, "Batal" and "Unduh File". */
@Composable
internal fun HistoryExportSheetContent(
    state: HistorySheetUiState.Export,
    onEvent: (HistoryUiEvent) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    SheetColumn(modifier = modifier) {
        SheetHeader(
            title = stringResource(R.string.history_export_title),
            subtitle = stringResource(R.string.history_export_subtitle),
        )
        SheetField(label = stringResource(R.string.history_export_period)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                ExportPeriod.entries.forEach { period ->
                    GotestFilterPill(
                        text = stringResource(period.labelRes),
                        selected = period == state.period,
                        onClick = { onEvent(HistoryUiEvent.ExportPeriodSelect(period)) },
                    )
                }
            }
        }
        SheetField(label = stringResource(R.string.history_export_format)) {
            OptionCard {
                ExportFormat.entries.forEach { format ->
                    FormatOption(
                        format = format,
                        selected = format == state.format,
                        onClick = { onEvent(HistoryUiEvent.ExportFormatSelect(format)) },
                    )
                }
            }
        }
        Text(
            text = exportSummary(state),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        SheetActions(
            secondaryText = stringResource(R.string.history_export_cancel),
            onSecondary = onCancel,
            primaryText = stringResource(R.string.history_export_download),
            onPrimary = { onEvent(HistoryUiEvent.ExportDownload) },
            primaryEnabled = state.canDownload,
        )
    }
}

/** "24 attempt • 1 Okt – 29 Okt 2026", or a note when the period has no attempts. */
@Composable
private fun exportSummary(state: HistorySheetUiState.Export): String {
    val first = state.firstDate
    val last = state.lastDate
    if (first == null || last == null) return stringResource(R.string.history_export_summary_empty)
    return stringResource(
        R.string.history_export_summary,
        GotestFormat.number(state.attemptCount),
        dateRangeText(first, last),
    )
}

/** "18 Okt – 21 Okt 2026"; the year is written once unless the range crosses years. */
@Composable
private fun dateRangeText(
    first: LocalDate,
    last: LocalDate,
): String {
    if (first == last) return GotestFormat.mediumDate(last)
    val start = if (first.year == last.year) GotestFormat.shortDate(first) else GotestFormat.mediumDate(first)
    return stringResource(R.string.history_export_date_range, start, GotestFormat.mediumDate(last))
}

@get:StringRes
private val ExportPeriod.labelRes: Int
    get() =
        when (this) {
            ExportPeriod.Last7Days -> R.string.history_export_period_7_days
            ExportPeriod.Last30Days -> R.string.history_export_period_30_days
            ExportPeriod.Last3Months -> R.string.history_export_period_3_months
            ExportPeriod.All -> R.string.history_export_period_all
        }

/** Title, description and icon of each "Format file" option. */
private data class FormatCopy(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
)

private fun ExportFormat.formatCopy(): FormatCopy =
    when (this) {
        ExportFormat.Pdf -> {
            FormatCopy(
                R.string.history_export_format_pdf,
                R.string.history_export_format_pdf_description,
                GotestIcons.Linear.DocumentText,
            )
        }

        ExportFormat.Csv -> {
            FormatCopy(
                R.string.history_export_format_csv,
                R.string.history_export_format_csv_description,
                GotestIcons.Linear.MenuBoard,
            )
        }
    }

/** Sheet body padding and spacing; scrolls when the sheet is taller than the screen. */
@Composable
private fun SheetColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        content()
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/** "Periode" / "Format file" label above its control. */
@Composable
private fun SheetField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
        content()
    }
}

/** White card holding the radio rows, announced as one radio group. */
@Composable
private fun OptionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.none),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        Column(modifier = Modifier.selectableGroup()) { content() }
    }
}

@Composable
private fun SortOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        RadioIndicator(selected = selected)
    }
}

/** Icon tile, title with description, and the radio; the tile turns brand-tinted when selected. */
@Composable
private fun FormatOption(
    format: ExportFormat,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val copy = format.formatCopy()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(start = spacing.sm, end = spacing.lg, top = spacing.sm, bottom = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HistoryDimens.formatTile)
                    .background(
                        if (selected) colors.bgBrandSoft else colors.bgSubtle,
                        RoundedCornerShape(HistoryDimens.formatTileRadius),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(copy.icon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = if (selected) colors.iconBrand else colors.iconDefault,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(copy.title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(copy.description),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        RadioIndicator(selected = selected)
    }
}

/** The sheets' 20dp radio: a 2dp ring, dark with a dot when selected. */
@Composable
private fun RadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = GotestTheme.shapes.full
    val ringColor = if (selected) colors.borderBrandDark else colors.borderStrong
    Box(
        modifier =
            modifier
                .size(GotestTheme.sizes.iconMd)
                .border(HistoryDimens.radioStroke, ringColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(HistoryDimens.radioDot).background(colors.bgBrandDark, shape))
        }
    }
}

/** Plain secondary and filled primary button, side by side. */
@Composable
private fun SheetActions(
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(
            text = secondaryText,
            onClick = onSecondary,
            modifier = Modifier.weight(1f),
            variant = GotestButtonVariant.Plain,
        )
        GotestButton(
            text = primaryText,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
        )
    }
}

@Composable
private fun SheetPreviewHost(content: @Composable () -> Unit) {
    GotestTheme {
        Column(
            modifier =
                Modifier
                    .background(GotestTheme.colors.bgApp, GotestTheme.shapes.bottomSheet)
                    .padding(top = GotestTheme.spacing.xl),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HistorySortSheetPreview() {
    SheetPreviewHost {
        HistorySortSheetContent(selected = HistorySort.Newest, onSelect = {}, onReset = {}, onApply = {})
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HistoryExportSheetPreview() {
    SheetPreviewHost {
        HistoryExportSheetContent(
            state =
                HistorySheetUiState.Export(
                    period = ExportPeriod.Last30Days,
                    format = ExportFormat.Pdf,
                    attemptCount = 24,
                    firstDate = LocalDate.of(2026, 10, 1),
                    lastDate = LocalDate.of(2026, 10, 29),
                ),
            onEvent = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HistoryExportSheetEmptyPeriodPreview() {
    SheetPreviewHost {
        HistoryExportSheetContent(
            state =
                HistorySheetUiState.Export(
                    period = ExportPeriod.Last7Days,
                    format = ExportFormat.Csv,
                    attemptCount = 0,
                    firstDate = null,
                    lastDate = null,
                ),
            onEvent = {},
            onCancel = {},
        )
    }
}
