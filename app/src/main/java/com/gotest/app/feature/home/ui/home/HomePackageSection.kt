package com.gotest.app.feature.home.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.home.ui.components.HomeDimens
import com.gotest.app.feature.home.ui.components.HomeSectionHeader
import com.gotest.app.feature.home.ui.components.HomeTabSwitcher

/** "Paket Untukmu": Latihan/Tryout tabs over a horizontal row of package cards. */
@Composable
internal fun HomeRecommendedSection(
    recommendations: HomeRecommendations,
    onSelectType: (PackageType) -> Unit,
    onSeeAll: () -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    // The row scrolls edge to edge inside the card, so only the header and tabs get side padding.
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = spacing.lg),
        verticalSpacing = spacing.lg,
    ) {
        HomeSectionHeader(
            title = stringResource(R.string.home_recommended_title),
            subtitle = stringResource(R.string.home_recommended_subtitle),
            actionText = stringResource(R.string.home_see_all),
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = spacing.lg),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            HomeTabSwitcher(
                options = listOf(stringResource(R.string.home_tab_practice), stringResource(R.string.home_tab_tryout)),
                selectedIndex = recommendations.selectedType.ordinal,
                onSelect = { onSelectType(PackageType.entries[it]) },
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
            if (recommendations.visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_recommended_empty),
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GotestTheme.colors.textSecondary,
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    items(recommendations.visible, key = { it.id }) { studyPackage ->
                        HomePackageCard(
                            studyPackage = studyPackage,
                            onOpen = { onOpenPackage(studyPackage.id, studyPackage.type) },
                        )
                    }
                }
            }
        }
    }
}

/** One "Paket Untukmu" card: chip, title, description, action and the attempts footer. */
@Composable
internal fun HomePackageCard(
    studyPackage: StudyPackage,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
    Surface(
        modifier = modifier.width(HomeDimens.packageCardWidth),
        shape = MaterialTheme.shapes.medium,
        color = colors.bgSubtle,
        border = border,
    ) {
        Column(modifier = Modifier.padding(spacing.xs)) {
            Surface(shape = MaterialTheme.shapes.small, color = colors.bgApp, border = border) {
                Column(
                    modifier = Modifier.padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    GotestChip(text = studyPackage.examLabel, tone = examChipTone(studyPackage.examLabel))
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = studyPackage.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = studyPackage.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HomePackageAction(studyPackage = studyPackage, onOpen = onOpen)
                }
            }
            HomePackageFooter(studyPackage = studyPackage)
        }
    }
}

@Composable
private fun HomePackageAction(
    studyPackage: StudyPackage,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionText =
        when {
            studyPackage.type == PackageType.Tryout -> R.string.home_package_start_tryout
            studyPackage.attempts > 0 -> R.string.home_package_retry_practice
            else -> R.string.home_package_start_practice
        }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestButton(
            text = stringResource(actionText),
            onClick = onOpen,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
        studyPackage.bestScore?.let { score ->
            GotestChip(
                text = stringResource(R.string.home_package_score, GotestFormat.number(score)),
                tone = GotestChipTone.Green,
            )
        }
    }
}

/** "Dikerjakan" and "Dikerjakan Pada" under the white card. */
@Composable
private fun HomePackageFooter(
    studyPackage: StudyPackage,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val empty = stringResource(R.string.home_package_value_empty)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        HomePackageFooterValue(
            label = stringResource(R.string.home_package_attempts_label),
            value =
                if (studyPackage.attempts > 0) {
                    stringResource(R.string.home_package_attempts_value, studyPackage.attempts)
                } else {
                    empty
                },
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .width(GotestTheme.sizes.strokeThin)
                    .fillMaxHeight()
                    .background(GotestTheme.colors.borderSubtle),
        )
        HomePackageFooterValue(
            label = stringResource(R.string.home_package_last_attempt_label),
            value = studyPackage.lastAttemptOn?.let(GotestFormat::mediumDate) ?: empty,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomePackageFooterValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeRecommendedSectionPreview() {
    GotestTheme {
        HomeRecommendedSection(
            recommendations = HomePreviewData.recommendations(),
            onSelectType = {},
            onSeeAll = {},
            onOpenPackage = { _, _ -> },
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeRecommendedSectionTryoutPreview() {
    GotestTheme {
        HomeRecommendedSection(
            recommendations = HomePreviewData.recommendations(PackageType.Tryout),
            onSelectType = {},
            onSeeAll = {},
            onOpenPackage = { _, _ -> },
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePackageCardPreview() {
    GotestTheme {
        HomePackageCard(
            studyPackage = HomePreviewData.practicePackages.last(),
            onOpen = {},
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}
