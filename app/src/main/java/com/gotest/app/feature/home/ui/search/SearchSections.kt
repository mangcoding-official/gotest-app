package com.gotest.app.feature.home.ui.search

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.home.domain.HelpArticle
import com.gotest.app.feature.home.domain.SearchAttempt
import com.gotest.app.feature.home.ui.components.HomeIconTile

/** "Paket Tryout" / "Paket Latihan" with a package count and tappable rows. */
@Composable
internal fun SearchPackageSection(
    title: String,
    packages: List<StudyPackage>,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    SearchSection(
        title = title,
        count = stringResource(R.string.home_search_package_count, packages.size),
        modifier = modifier,
    ) {
        SearchListCard {
            packages.forEach { studyPackage ->
                val tryout = studyPackage.type == PackageType.Tryout
                SearchRow(
                    icon = GotestIcons.Bold.Note2,
                    containerColor = if (tryout) colors.bgBlueSoft else colors.bgBrandSoft,
                    iconColor = if (tryout) colors.bgBlue else colors.iconBrand,
                    title = studyPackage.title,
                    subtitle =
                        studyPackage.durationMinutes?.let {
                            stringResource(R.string.home_search_package_meta, studyPackage.questionCount, it)
                        } ?: stringResource(R.string.home_package_question_count, studyPackage.questionCount),
                    onClick = { onOpenPackage(studyPackage.id, studyPackage.type) },
                ) {
                    Icon(
                        painter = painterResource(GotestIcons.Linear.ArrowRight),
                        contentDescription = null,
                        modifier = Modifier.size(GotestTheme.sizes.iconMd),
                        tint = colors.iconMuted,
                    )
                }
            }
        }
    }
}

/** "Topik": grey chips. */
@Composable
internal fun SearchTopicSection(
    topics: List<String>,
    modifier: Modifier = Modifier,
) {
    SearchSection(
        title = stringResource(R.string.home_search_section_topics),
        count = stringResource(R.string.home_search_topic_count, topics.size),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            topics.forEach { topic -> GotestChip(text = topic, tone = GotestChipTone.Grey) }
        }
    }
}

/** "Hasil Kamu": the student's own attempts with score and rank. */
@Composable
internal fun SearchAttemptSection(
    attempts: List<SearchAttempt>,
    onOpenResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    SearchSection(title = stringResource(R.string.home_search_section_results), count = null, modifier = modifier) {
        SearchListCard {
            attempts.forEach { attempt ->
                SearchRow(
                    icon = GotestIcons.Bold.Award,
                    containerColor = colors.bgGreenSoft,
                    iconColor = colors.bgGreen,
                    title = attempt.title,
                    subtitle =
                        stringResource(R.string.home_search_result_date, GotestFormat.mediumDate(attempt.completedOn)),
                    onClick = { onOpenResult(attempt.attemptId) },
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = GotestFormat.number(attempt.score),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        attempt.rank?.let { rank ->
                            Text(
                                text = stringResource(R.string.home_search_result_rank, rank),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Artikel & Bantuan": Pusat Bantuan articles. */
@Composable
internal fun SearchArticleSection(
    articles: List<HelpArticle>,
    onOpenArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    SearchSection(title = stringResource(R.string.home_search_section_articles), count = null, modifier = modifier) {
        SearchListCard {
            articles.forEach { article ->
                SearchRow(
                    icon = GotestIcons.Bold.BookOpened,
                    containerColor = colors.bgYellowSoft,
                    iconColor = colors.bgYellow,
                    title = article.title,
                    subtitle = stringResource(R.string.home_search_article_meta, article.readMinutes),
                    onClick = { onOpenArticle(article.id) },
                    titleMaxLines = 2,
                ) {
                    Text(
                        text = stringResource(R.string.home_search_article_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textBrandDark,
                    )
                }
            }
        }
    }
}

/** Section title with an optional brand-coloured count ("2 paket"). */
@Composable
private fun SearchSection(
    title: String,
    count: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            if (count != null) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelMedium,
                    color = GotestTheme.colors.textBrandDark,
                )
            }
        }
        content()
    }
}

@Composable
private fun SearchListCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        content()
    }
}

/** One result row: tinted icon, title and caption, and a trailing slot. */
@Composable
private fun SearchRow(
    @DrawableRes icon: Int,
    containerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
    trailing: @Composable RowScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeIconTile(icon = icon, containerColor = containerColor, iconColor = iconColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun SearchSectionsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            SearchPackageSection(
                title = "Paket Tryout",
                packages = SearchPreviewData.success.tryouts,
                onOpenPackage = { _, _ -> },
            )
            SearchTopicSection(topics = SearchPreviewData.success.topics)
            SearchAttemptSection(attempts = SearchPreviewData.success.attempts, onOpenResult = {})
            SearchArticleSection(articles = SearchPreviewData.success.articles, onOpenArticle = {})
        }
    }
}
