package com.gotest.app.feature.studypackage.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.examChipTone

/** One of the three grey tiles under the hero text (Soal / Durasi / Penilaian or Section). */
@Immutable
internal data class InfoTileData(
    @param:DrawableRes val icon: Int,
    val value: String,
    val label: String,
)

/** One row of "Materi yang diujikan" or "Urutan Section". */
@Immutable
internal data class NumberedItem(
    val number: Int,
    val title: String,
    val meta: String,
)

/** Hero card of Detail Paket / Detail Tryout: exam + type chips, title, description and tiles. */
@Composable
internal fun PackageHeroCard(
    examLabel: String,
    typeLabel: String,
    typeTone: GotestChipTone,
    title: String,
    description: String,
    tiles: List<InfoTileData>,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), shape = GotestTheme.shapes.largeIncreased) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs)) {
            GotestChip(text = examLabel, tone = examChipTone(examLabel))
            GotestChip(text = typeLabel, tone = typeTone)
        }
        Text(text = title, style = titleStyle, color = GotestTheme.colors.textPrimary)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = GotestTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            tiles.forEach { tile -> InfoTile(tile = tile, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun InfoTile(
    tile: InfoTileData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(tile.icon),
            contentDescription = null,
            modifier = Modifier.size(StudyPackageDimens.infoTileIcon),
            tint = GotestTheme.colors.iconBrand,
        )
        Column {
            Text(text = tile.value, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

/** White card with a heading and numbered rows; [headerChip] adds the yellow chip on the right. */
@Composable
internal fun NumberedListCard(
    title: String,
    items: List<NumberedItem>,
    modifier: Modifier = Modifier,
    headerChip: String? = null,
    itemStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = spacing.sm),
        verticalSpacing = spacing.none,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.lg, bottom = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
            if (headerChip != null) {
                GotestChip(text = headerChip, tone = GotestChipTone.Yellow)
            }
        }
        items.forEach { item -> NumberedRow(item = item, style = itemStyle) }
    }
}

@Composable
private fun NumberedRow(
    item: NumberedItem,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(StudyPackageDimens.numberBadge)
                    .background(GotestTheme.colors.bgBrandSoft, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textBrandDark,
            )
        }
        Text(text = item.title, modifier = Modifier.weight(1f), style = style, color = GotestTheme.colors.textPrimary)
        Text(text = item.meta, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/** Yellow "Sebelum mulai" / "Ketentuan Pengerjaan" card with bullet rules. */
@Composable
internal fun RulesCard(
    title: String,
    rules: List<String>,
    modifier: Modifier = Modifier,
    showBorder: Boolean = false,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.large
    val border = if (showBorder) BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle) else null
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgYellowSoft, shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(GotestIcons.Bold.InfoCircle),
                contentDescription = null,
                modifier = Modifier.size(StudyPackageDimens.infoTileIcon),
                tint = colors.bgYellow,
            )
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = colors.bgYellowDark)
        }
        Column {
            rules.forEach { rule ->
                Text(
                    text = stringResource(R.string.studypackage_rule_bullet, rule),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.bgYellowDark,
                )
            }
        }
    }
}

/** White bottom bar with an optional "Progres terakhir" line above the primary button. */
@Composable
internal fun DetailBottomBar(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progressText: String? = null,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(GotestTheme.colors.bgApp)) {
        GotestDivider()
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (progressText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.studypackage_last_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = GotestTheme.colors.textSecondary,
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = GotestTheme.colors.textPrimary,
                    )
                }
            }
            GotestButton(text = buttonText, onClick = onClick, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun DetailComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            PackageHeroCard(
                examLabel = "UTBK-SNBT",
                typeLabel = "Latihan",
                typeTone = GotestChipTone.Violet,
                title = "Latihan September Session 1",
                description = "Latihan persiapan UTBK-SNBT dengan 20 soal dan penilaian benar 1, salah 0.",
                tiles =
                    listOf(
                        InfoTileData(GotestIcons.Linear.Note2, "20", "Soal"),
                        InfoTileData(GotestIcons.Linear.Calendar, "45 mnt", "Durasi"),
                        InfoTileData(GotestIcons.Linear.Award, "+1 / 0", "Penilaian"),
                    ),
            )
            NumberedListCard(
                title = "Urutan Section",
                items = listOf(NumberedItem(1, "Penalaran Umum", "30 soal • 30 mnt")),
                headerChip = "Waktu dikelola server",
            )
            RulesCard(title = "Sebelum mulai", rules = listOf("Pembahasan tersedia setelah latihan selesai."))
            DetailBottomBar(buttonText = "Lanjutkan Latihan", onClick = {}, progressText = "5 / 20 soal")
        }
    }
}
