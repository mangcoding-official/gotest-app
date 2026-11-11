package com.gotest.app.feature.studypackage.ui.studypackage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestActivityRow
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.studypackage.ui.components.AccentBar
import com.gotest.app.feature.studypackage.ui.components.CardMetaLine
import com.gotest.app.feature.studypackage.ui.components.CardStat
import com.gotest.app.feature.studypackage.ui.components.NestedCard
import com.gotest.app.feature.studypackage.ui.components.StudyItemCard
import com.gotest.app.feature.studypackage.ui.components.StudyPackageDimens
import com.gotest.app.feature.studypackage.ui.components.StudyPackagePreviewData

/** Category card of the Paket tab ("Perguruan Tinggi", "SLTA / Sederajat", …). */
@Composable
internal fun CategoryCard(
    category: StudyCategory,
    type: PackageType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val packageCountRes =
        when (type) {
            PackageType.Practice -> R.string.studypackage_category_practice_count
            PackageType.Tryout -> R.string.studypackage_category_tryout_count
        }
    NestedCard(
        onClick = onClick,
        footer = {
            CardMetaLine(
                start = stringResource(R.string.studypackage_category_sub_count, category.subCategoryCount),
                end = stringResource(packageCountRes, category.packageCount),
            )
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(GotestTheme.spacing.lg)) {
            AccentBar(accent = category.accent)
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "Aktivitas Terkini": the latest finished attempts with a "Lihat Hasil" action. */
@Composable
internal fun RecentActivityBlock(
    activities: List<AttemptSummary>,
    onActivityClick: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
        SectionHeading(
            title = stringResource(R.string.studypackage_activity_title),
            subtitle = stringResource(R.string.studypackage_activity_subtitle),
        )
        if (activities.isEmpty()) {
            Text(
                text = stringResource(R.string.studypackage_activity_empty),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        } else {
            val shape = MaterialTheme.shapes.small
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, shape),
            ) {
                activities.forEachIndexed { index, activity ->
                    key(activity.attemptId) {
                        if (index > 0) GotestDivider()
                        GotestActivityRow(
                            chipText = activity.examLabel,
                            title = activity.title,
                            subtitle =
                                stringResource(
                                    R.string.studypackage_activity_completed,
                                    GotestFormat.longDate(activity.completedOn),
                                ),
                            actionText = stringResource(R.string.studypackage_activity_action),
                            onAction = { onActivityClick(activity.attemptId) },
                            chipTone = examChipTone(activity.examLabel),
                        )
                    }
                }
            }
        }
    }
}

/** "Seasonal Tryout": description, live countdown and a horizontal row of tryout cards. */
@Composable
internal fun SeasonalTryoutBlock(
    seasonal: SeasonalTryout,
    countdown: Countdown?,
    onTryoutClick: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            SectionHeading(title = seasonal.title, subtitle = seasonal.description)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.studypackage_seasonal_ends_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GotestTheme.colors.textSecondary,
                )
                CountdownBox(countdown = countdown)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            // Seasonal entries can point at the same package, so the index keeps keys unique.
            itemsIndexed(seasonal.packages, key = { index, pkg -> "$index:${pkg.id}" }) { _, pkg ->
                SeasonalTryoutCard(
                    pkg = pkg,
                    onClick = { onTryoutClick(pkg.id) },
                    modifier = Modifier.width(StudyPackageDimens.seasonalCardWidth),
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = GotestTheme.colors.textSecondary)
    }
}

/** The countdown sits in a plain button-shaped box in the design; it is not interactive. */
@Composable
private fun CountdownBox(
    countdown: Countdown?,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
    ) {
        Box(
            modifier =
                Modifier
                    .heightIn(min = GotestTheme.sizes.buttonLarge)
                    .padding(horizontal = spacing.md + spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    if (countdown == null) {
                        stringResource(R.string.studypackage_value_empty)
                    } else {
                        stringResource(
                            R.string.studypackage_countdown,
                            countdown.hours,
                            countdown.minutes,
                            countdown.seconds,
                        )
                    },
                style = GotestTheme.typography.buttonLarge,
                color = GotestTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SeasonalTryoutCard(
    pkg: StudyPackage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.studypackage_value_empty)
    val attempted = pkg.attempts > 0
    val rank = pkg.rank
    val participants = pkg.participants
    StudyItemCard(
        title = pkg.title,
        subtitle = stringResource(R.string.studypackage_question_count, pkg.questionCount),
        actionText =
            stringResource(
                if (attempted) R.string.studypackage_seasonal_again else R.string.studypackage_seasonal_start,
            ),
        onAction = onClick,
        onClick = onClick,
        stats =
            CardStat(
                label = stringResource(R.string.studypackage_ranking),
                value =
                    if (rank != null && participants != null) {
                        stringResource(R.string.studypackage_ranking_value, rank, GotestFormat.number(participants))
                    } else {
                        empty
                    },
            ) to
                CardStat(
                    label = stringResource(R.string.studypackage_done_on),
                    value = pkg.lastAttemptOn?.let(GotestFormat::mediumDate) ?: empty,
                ),
        modifier = modifier,
        scoreText = pkg.bestScore?.takeIf { attempted }?.let { stringResource(R.string.studypackage_score, it) },
    )
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun StudyPackageSectionsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            CategoryCard(
                category = StudyPackagePreviewData.categories.first(),
                type = PackageType.Practice,
                onClick = {},
            )
            RecentActivityBlock(activities = StudyPackagePreviewData.history, onActivityClick = {})
            SeasonalTryoutBlock(
                seasonal = StudyPackagePreviewData.seasonalTryout,
                countdown = Countdown(hours = 24, minutes = 41, seconds = 31),
                onTryoutClick = {},
            )
        }
    }
}
