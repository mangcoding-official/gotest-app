package com.gotest.app.feature.support.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.support.domain.SupportSection
import com.gotest.app.feature.support.domain.SupportTopic
import kotlinx.coroutines.launch

@Composable
fun SupportTopicRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupportTopicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SupportTopicScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::load, modifier = modifier)
}

@Composable
fun SupportTopicScreen(
    uiState: SupportTopicUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topic = uiState.topic
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = topic?.title ?: stringResource(R.string.support_title),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when {
            topic != null -> TopicContent(topic)
            uiState.loadFailed -> GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize())
            else -> GotestLoadingView()
        }
    }
}

/** Web ArticleLayout: title and description, the "Topik" list (tap scrolls to that part), then every part. */
@Composable
private fun TopicContent(
    topic: SupportTopic,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var active by remember(topic.slug) { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.md,
                bottom = spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "head") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = topic.title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
        }
        item(key = "sections") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(R.string.support_topic_sections),
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    topic.sections.forEachIndexed { index, section ->
                        GotestFilterPill(
                            text = section.title,
                            selected = index == active,
                            onClick = {
                                active = index
                                scope.launch { listState.animateScrollToItem(FIRST_SECTION_ITEM + index) }
                            },
                        )
                    }
                }
            }
        }
        itemsIndexed(topic.sections, key = { _, section -> section.id }) { _, section -> SectionCard(section) }
    }
}

/** The topic head and the "Topik" list come before the parts. */
private const val FIRST_SECTION_ITEM = 2

@Composable
private fun SectionCard(
    section: SupportSection,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.xs) {
        Text(
            text = section.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        Text(text = section.body, style = MaterialTheme.typography.bodyMedium, color = GotestTheme.colors.textSecondary)
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SupportTopicScreenPreview() {
    GotestTheme {
        SupportTopicScreen(
            uiState =
                SupportTopicUiState(
                    isLoading = false,
                    topic =
                        SupportTopic(
                            slug = "paket-latihan-tryout",
                            title = "Paket Latihan & Try Out",
                            description = "mengerjakan, report latihan, dan Try Out",
                            sections =
                                listOf(
                                    SupportSection(
                                        "ikuti-tryout",
                                        "Bagaimana cara mengikuti Try Out",
                                        "Buka menu \"Paket Try Out\".",
                                    ),
                                    SupportSection(
                                        "pembahasan",
                                        "Bagaimana cara melihat pembahasan soal",
                                        "Buka hasil latihan.",
                                    ),
                                ),
                        ),
                ),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportTopicScreenErrorPreview() {
    GotestTheme {
        SupportTopicScreen(
            uiState = SupportTopicUiState(isLoading = false, loadFailed = true),
            onBack = {},
            onRetry = {},
        )
    }
}
