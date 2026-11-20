package com.gotest.app.feature.practice.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.domain.PracticePackage
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A category of the front (web CategoryCard): a coloured bar, its name, text, and counts. */
@Composable
fun CategoryCard(
    group: CategoryGroup,
    index: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogCategoryCard(
        name = group.name,
        description = group.description ?: stringResource(R.string.practice_category_text, group.name),
        index = index,
        meta =
            listOf(
                stringResource(R.string.practice_category_subjects, group.subjects.size),
                stringResource(R.string.practice_category_packages, group.packages.size),
            ),
        onOpen = onOpen,
        modifier = modifier,
    )
}

/** Web CategoryCard, shared by Paket Latihan and Paket Try Out. */
@Composable
internal fun CatalogCategoryCard(
    name: String,
    description: String,
    index: Int,
    meta: List<String>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogCard(onClick = onOpen, modifier = modifier, footer = { MetaFooter(meta) }) {
        Bar(barColor(index))
        CardTitle(name)
        CardText(description)
    }
}

/** A subtest of a category (web SubjectCard): topics, packages, and how many are done. */
@Composable
fun SubjectCard(
    group: SubjectGroup,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = subjectName(group)
    val count = group.packages.size
    CatalogCard(
        onClick = onOpen,
        modifier = modifier,
        footer = {
            MetaFooter(
                listOf(
                    stringResource(R.string.practice_subject_topics, group.topicCount),
                    stringResource(R.string.practice_category_packages, count),
                ),
            )
        },
    ) {
        Bar(GotestTheme.colors.iconMuted)
        CardTitle(name)
        CardText(subjectText(group))
        GotestProgressBar(
            progress = if (count > 0) group.finishedPackages.toFloat() / count else 0f,
            modifier = Modifier.fillMaxWidth().padding(top = GotestTheme.spacing.md),
            color = GotestTheme.colors.statusSuccess,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.practice_subject_done),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.practice_subject_done_of, group.finishedPackages, count),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
fun subjectName(group: SubjectGroup): String =
    if (group.subject.id == GENERAL.id) stringResource(R.string.practice_general_subject) else group.subject.name

@Composable
fun subjectText(group: SubjectGroup): String =
    when {
        group.subject.id == GENERAL.id -> stringResource(R.string.practice_general_subject_text)
        !group.subject.description.isNullOrBlank() -> group.subject.description
        else -> stringResource(R.string.practice_subject_text, group.subject.name)
    }

/**
 * A package (web PracticePackageCard): name, Premium mark, topics, its button, the last score, and when it was
 * done. A Premium package for a Gratis account opens the start sheet, which says why it cannot start (A2c-3).
 */
@Composable
fun PracticePackageCard(
    item: PracticePackage,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished = item.finishedCount
    CatalogCard(
        modifier = modifier,
        footer = {
            Row(modifier = Modifier.fillMaxWidth()) {
                FooterFact(
                    stringResource(R.string.practice_package_done),
                    if (finished > 0) finished.toString() else stringResource(R.string.practice_none),
                    Modifier.weight(1f),
                )
                FooterFact(
                    stringResource(R.string.practice_package_done_at),
                    shortDate(item.lastFinishedAt) ?: stringResource(R.string.practice_none),
                    Modifier.weight(1f),
                )
            }
        },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            if (item.premium) GotestChip(text = stringResource(R.string.practice_premium), tone = GotestChipTone.Yellow)
        }
        CardText(stringResource(R.string.practice_package_topics, item.topicIds.size))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = stringResource(cardLabel(item)),
                onClick = onStart,
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
            Box(modifier = Modifier.weight(1f))
            val score = item.lastScore
            if (score != null && finished > 0) {
                GotestChip(
                    text =
                        stringResource(
                            R.string.practice_package_score,
                            item.lastScoreLabel ?: stringResource(R.string.practice_score_fallback),
                            plainNumber(score),
                        ),
                    tone = GotestChipTone.Green,
                )
            }
        }
    }
}

/** The card's button (web PracticePackageCard `label`). */
fun cardLabel(item: PracticePackage): Int {
    val finished = item.finishedCount > 0
    val toPremium = item.premiumLocked && item.continueAttemptId == null && !(finished && !item.allowRetry)
    return when {
        item.continueAttemptId != null -> R.string.practice_continue
        toPremium -> R.string.practice_premium_only
        finished && !item.allowRetry -> R.string.practice_review
        finished -> R.string.practice_again
        else -> R.string.practice_start
    }
}

@Composable
internal fun CatalogCard(
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = GotestTheme.colors.bgSubtle,
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(GotestTheme.spacing.xs),
        verticalSpacing = 0.dp,
    ) {
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            verticalSpacing = GotestTheme.spacing.xs,
        ) { content() }
        Box(modifier = Modifier.padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm)) {
            footer()
        }
    }
}

@Composable
private fun Bar(color: Color) {
    Box(
        modifier =
            Modifier
                .width(32.dp)
                .height(8.dp)
                .background(color, CircleShape),
    )
}

@Composable
private fun barColor(index: Int): Color {
    val colors = GotestTheme.colors
    return listOf(colors.statusWarning, colors.iconMuted, colors.bgBlue, colors.statusDanger)[index % BAR_COLORS]
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
}

@Composable
private fun CardText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.textSecondary,
        maxLines = 2,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetaFooter(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        items.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    Box(
                        modifier =
                            Modifier
                                .padding(end = GotestTheme.spacing.sm)
                                .width(4.dp)
                                .height(4.dp)
                                .background(GotestTheme.colors.borderStrong, CircleShape),
                    )
                }
                Text(text = item, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
internal fun FooterFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textPrimary)
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

private const val BAR_COLORS = 4
