package com.gotest.app.feature.home.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestActivityRow
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.home.ui.components.HomeDimens
import com.gotest.app.feature.home.ui.components.HomeSectionHeader
import com.gotest.app.feature.home.ui.components.HomeTabSwitcher

/** "Aktivitas Terkini": latest finished attempts with a link to each result. */
@Composable
internal fun HomeActivitySection(
    activity: HomeActivity,
    onSelectType: (PackageType) -> Unit,
    onSeeAll: () -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        HomeSectionHeader(
            title = stringResource(R.string.home_activity_title),
            subtitle = stringResource(R.string.home_activity_subtitle),
            actionText = stringResource(R.string.home_see_all),
            onAction = onSeeAll,
        )
        HomeTabSwitcher(
            options = listOf(stringResource(R.string.home_tab_practice), stringResource(R.string.home_tab_tryout)),
            selectedIndex = activity.selectedType.ordinal,
            onSelect = { onSelectType(PackageType.entries[it]) },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, shape),
        ) {
            if (activity.attempts.isEmpty()) {
                HomeActivityEmpty()
            } else {
                activity.attempts.forEach { attempt ->
                    GotestActivityRow(
                        chipText = attempt.examLabel,
                        title = attempt.title,
                        subtitle =
                            stringResource(
                                R.string.home_activity_completed_on,
                                GotestFormat.longDate(attempt.completedOn),
                            ),
                        actionText = stringResource(R.string.home_activity_view_result),
                        onAction = { onOpenResult(attempt.attemptId) },
                        chipTone = examChipTone(attempt.examLabel),
                    )
                }
            }
        }
    }
}

/** "Belum ada Aktivitas" block from "Beranda / Empty State". */
@Composable
private fun HomeActivityEmpty(modifier: Modifier = Modifier) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl, vertical = spacing.xxxl + spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.illus_empty_activity),
            contentDescription = null,
            modifier = Modifier.widthIn(max = HomeDimens.emptyActivityIllustrationWidth).fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_activity_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_activity_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeActivitySectionPreview() {
    GotestTheme {
        HomeActivitySection(
            activity = HomeActivity(selectedType = PackageType.Practice, attempts = HomePreviewData.attempts),
            onSelectType = {},
            onSeeAll = {},
            onOpenResult = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeActivitySectionEmptyPreview() {
    GotestTheme {
        HomeActivitySection(
            activity = HomeActivity(selectedType = PackageType.Practice, attempts = emptyList()),
            onSelectType = {},
            onSeeAll = {},
            onOpenResult = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}
