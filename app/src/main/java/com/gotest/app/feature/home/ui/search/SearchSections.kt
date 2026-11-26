package com.gotest.app.feature.home.ui.search

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.home.domain.SearchGroup
import com.gotest.app.feature.home.domain.SearchItem
import com.gotest.app.feature.home.ui.components.HomeIconTile

/** One group of the web's search panel: its icon and title, then its rows (at most three). */
@Composable
internal fun SearchGroupCard(
    group: SearchGroup,
    onOpen: (SearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = groupTone(group.key)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIconTile(icon = tone.icon, containerColor = tone.container, iconColor = tone.content)
            Text(
                text = group.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
        }
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(),
            verticalSpacing = GotestTheme.spacing.none,
        ) {
            group.items.forEachIndexed { index, item ->
                if (index > 0) GotestDivider()
                SearchRow(item = item, onOpen = { onOpen(item) })
            }
        }
    }
}

/** Web SearchRow: the title, its details with dots between them, and the action. */
@Composable
private fun SearchRow(
    item: SearchItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.meta.isNotEmpty()) {
                Text(
                    text = item.meta.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
        GotestButton(
            text = item.actionLabel,
            onClick = onOpen,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

private data class GroupTone(
    @param:DrawableRes val icon: Int,
    val container: Color,
    val content: Color,
)

/** Web `groupTone`: Latihan green, Try Out blue, Hasil violet, Bantuan yellow. */
@Composable
private fun groupTone(key: String): GroupTone {
    val colors = GotestTheme.colors
    return when (key) {
        "tryout" -> GroupTone(GotestIcons.Bold.Note2, colors.bgBlueSoft, colors.bgBlue)
        "results" -> GroupTone(GotestIcons.Bold.Award, colors.bgBrandSoft, colors.iconBrand)
        "help" -> GroupTone(GotestIcons.Bold.BookOpened, colors.bgYellowSoft, colors.bgYellow)
        else -> GroupTone(GotestIcons.Bold.Note2, colors.bgGreenSoft, colors.bgGreen)
    }
}

internal val previewGroups =
    listOf(
        SearchGroup(
            key = "practice",
            title = "Paket Latihan",
            items =
                listOf(
                    SearchItem(
                        "practice-11",
                        "Latihan Penalaran Umum 1",
                        null,
                        "Mulai Latihan",
                        listOf("UTBK-SNBT", "20 Soal", "3 Topik"),
                    ),
                ),
        ),
        SearchGroup(
            key = "results",
            title = "Hasil",
            items =
                listOf(
                    SearchItem(
                        "result-2",
                        "Try Out UTBK-SNBT 1",
                        null,
                        "Lihat Hasil",
                        listOf("UTBK-SNBT", "160 Soal", "195 Menit"),
                    ),
                ),
        ),
    )

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun SearchGroupCardPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            previewGroups.forEach { SearchGroupCard(group = it, onOpen = {}) }
        }
    }
}
