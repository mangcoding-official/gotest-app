package com.gotest.app.feature.practice.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.OptionText
import com.gotest.app.feature.practice.domain.PracticeResult
import com.gotest.app.feature.practice.domain.ResponseKind
import com.gotest.app.feature.practice.domain.ReviewItem
import com.gotest.app.feature.practice.domain.responseKind
import com.gotest.app.feature.practice.ui.runner.PracticeResponse
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerEvent
import com.gotest.app.feature.practice.ui.runner.mediaUrl
import com.gotest.app.feature.practice.ui.runner.optionLetter
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.ruang.ui.work.ExamImage
import kotlinx.coroutines.launch

@Composable
fun PracticeResultRoute(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeResultScreen(
        uiState = uiState,
        packageName = packageName,
        actions =
            ReviewActions(
                onBack = onBack,
                onRetry = viewModel::load,
                onTry = viewModel::onTry,
                onReveal = viewModel::reveal,
            ),
        modifier = modifier,
    )
}

data class ReviewActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onTry: (Long, AnswerValue) -> Unit = { _, _ -> },
    val onReveal: (Long) -> Unit = {},
)

/** Web PembahasanPage for a Latihan: score, topics, question grid, then every question with its pembahasan. */
@Composable
fun PracticeResultScreen(
    uiState: PracticeResultUiState,
    packageName: String,
    actions: ReviewActions,
    modifier: Modifier = Modifier,
) {
    val result = uiState.result
    RuangScaffold(
        title = stringResource(R.string.review_kicker),
        subtitle = packageName.takeIf { it.isNotBlank() },
        onBack = actions.onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GotestLoadingView()
                    Text(text = stringResource(R.string.review_loading), style = MaterialTheme.typography.bodySmall)
                }
            }

            result == null -> {
                GotestErrorView(
                    onRetry = actions.onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    message = stringResource(R.string.review_load_failed),
                )
            }

            else -> {
                ReviewList(uiState = uiState, result = result, actions = actions, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ReviewList(
    uiState: PracticeResultUiState,
    result: PracticeResult,
    actions: ReviewActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The score and navigation take the first two rows; each question follows.
    val jumpTo: (Int) -> Unit = { index -> scope.launch { listState.animateScrollToItem(index + HEADER_ROWS) } }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                horizontal = GotestTheme.spacing.screenHorizontal,
                vertical = GotestTheme.spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
    ) {
        item { ScoreCard(result) }
        item { NavigationCard(items = result.items, onJump = jumpTo) }
        itemsIndexed(result.items, key = { _, item -> item.id }) { index, item ->
            ReviewCard(
                item = item,
                index = index,
                gated = item.isEmpty && item.id !in uiState.revealed,
                answeredHere = item.isEmpty,
                tryValue = uiState.tries[item.id],
                onTry = { actions.onTry(item.id, it) },
                onReveal = { actions.onReveal(item.id) },
            )
        }
    }
}

@Composable
private fun ScoreCard(result: PracticeResult) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.review_score_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        ScoreLine(stringResource(R.string.review_correct), result.correctCount.toString(), colors.statusSuccess)
        ScoreLine(stringResource(R.string.review_wrong), result.wrongCount.toString(), colors.statusDanger)
        ScoreLine(stringResource(R.string.review_empty), result.emptyCount.toString(), colors.textSecondary)
        val display = result.displayScore
        Text(
            text =
                stringResource(
                    R.string.review_total,
                    display?.label ?: stringResource(R.string.review_score_default_title),
                    formatScore(display?.value ?: result.score),
                ),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textBrandDark,
        )
        val label =
            listOfNotNull(
                display?.let { stringResource(R.string.review_display_score, it.correct, it.total) }
                    ?: result.scoreLabel,
                stringResource(R.string.review_disqualified).takeIf { result.disqualified },
            ).joinToString(" · ")
        if (label.isNotEmpty()) {
            val scale = display?.scale ?: result.scoringProfile?.scale
            Text(
                text = label + (scale?.let { stringResource(R.string.review_scale, formatScore(it)) } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ScoreLine(
    label: String,
    value: String,
    color: Color,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** Topics and the question grid with its legend; a tap scrolls to the question (web PembahasanPage sidebar). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NavigationCard(
    items: List<ReviewItem>,
    onJump: (Int) -> Unit,
) {
    val colors = GotestTheme.colors
    val general = stringResource(R.string.practice_general_subject)
    val topics = items.map { it.topicName?.trim()?.takeIf(String::isNotEmpty) ?: general }.distinct()
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        if (topics.isNotEmpty()) {
            Text(
                text = stringResource(R.string.runner_topics),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            ) {
                topics.forEachIndexed { index, topic ->
                    val first =
                        items.indexOfFirst {
                            (it.topicName?.trim()?.takeIf(String::isNotEmpty) ?: general) ==
                                topic
                        }
                    GotestFilterPill(text = "${index + 1}. $topic", selected = false, onClick = { onJump(first) })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.runner_nav_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.review_nav_count, items.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            items.forEachIndexed { index, item ->
                val (label, _) = statusOf(item)
                val description = stringResource(R.string.review_nav_item, index + 1, stringResource(label))
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(statusColor(item), MaterialTheme.shapes.small)
                            .clickable { onJump(index) }
                            .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.review_legend),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            Legend(colors.statusSuccess, stringResource(R.string.review_correct))
            Legend(colors.statusDanger, stringResource(R.string.review_wrong))
            Legend(colors.statusWarning, stringResource(R.string.review_empty))
        }
    }
}

@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Box(modifier = Modifier.size(8.dp).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

@Composable
private fun statusColor(item: ReviewItem): Color =
    when {
        item.isEmpty -> GotestTheme.colors.statusWarningSoft
        item.isCorrect == true -> GotestTheme.colors.statusSuccessSoft
        else -> GotestTheme.colors.statusDangerSoft
    }

/** Web `itemStatus`: Benar, Salah, or Kosong with its chip colour. */
private fun statusOf(item: ReviewItem): Pair<Int, GotestChipTone> =
    when {
        item.isEmpty -> R.string.review_empty to GotestChipTone.Yellow
        item.isCorrect == true -> R.string.review_correct to GotestChipTone.Green
        else -> R.string.review_wrong to GotestChipTone.Red
    }

/** One question of the pembahasan (web ReviewCard). An empty question is answered here first (`gated`). */
@Composable
private fun ReviewCard(
    item: ReviewItem,
    index: Int,
    gated: Boolean,
    answeredHere: Boolean,
    tryValue: AnswerValue?,
    onTry: (AnswerValue) -> Unit,
    onReveal: () -> Unit,
) {
    val colors = GotestTheme.colors
    val kind = responseKind(item.questionType)
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.runner_question, index + 1),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            val (label, tone) = statusOf(item)
            GotestChip(
                text =
                    stringResource(label) + " · " +
                        stringResource(
                            R.string.review_item_score,
                            formatScore(item.scoreAwarded),
                            formatScore(item.maxScore),
                        ),
                tone = tone,
            )
        }
        item.question.stimulus?.let { stimulus ->
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgSubtle) {
                RichText(
                    content = stimulus.text,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                mediaUrl(stimulus.mediaPath)?.let { ExamImage(url = it, description = stimulus.name.orEmpty()) }
            }
        }
        RichText(
            content = item.question.prompt.orEmpty(),
            isHtml = true,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        mediaUrl(item.question.mediaPath)?.let { ExamImage(url = it, description = "") }
        if (gated) {
            Text(
                text = stringResource(R.string.review_answer_first),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            PracticeResponse(
                kind = kind,
                content = item.question,
                answer = tryValue,
                enabled = true,
                onEvent = { event -> tryAnswer(event, tryValue, item.question.statements.size)?.let(onTry) },
            )
            GotestButton(
                text = stringResource(R.string.review_reveal),
                onClick = onReveal,
                enabled = complete(kind, tryValue, item.question.statements.size),
            )
        } else {
            val picked = if (answeredHere) tryValue else null
            Revealed(item = item, kind = kind, answeredHere = answeredHere, picked = picked)
        }
    }
}

/** An answer typed while reviewing, in the shape of the question (local only). */
private fun tryAnswer(
    event: PracticeRunnerEvent,
    current: AnswerValue?,
    rows: Int,
): AnswerValue? =
    when (event) {
        is PracticeRunnerEvent.SelectOption -> {
            AnswerValue.Single(event.index)
        }

        is PracticeRunnerEvent.ToggleOption -> {
            val chosen = (current as? AnswerValue.Multiple)?.options.orEmpty()
            AnswerValue.Multiple(if (event.index in chosen) chosen - event.index else chosen + event.index)
        }

        is PracticeRunnerEvent.StatementAnswer -> {
            val labels = (current as? AnswerValue.Statement)?.labels.orEmpty()
            AnswerValue.Statement(List(rows) { if (it == event.row) event.label else labels.getOrNull(it) })
        }

        else -> {
            null
        }
    }

private fun complete(
    kind: ResponseKind,
    value: AnswerValue?,
    rows: Int,
): Boolean =
    when (kind) {
        ResponseKind.Single -> {
            value is AnswerValue.Single
        }

        ResponseKind.Multiple -> {
            (value as? AnswerValue.Multiple)?.options?.isNotEmpty() == true
        }

        ResponseKind.Statement -> {
            rows > 0 &&
                (value as? AnswerValue.Statement)?.labels?.let { l ->
                    (0 until rows).all { l.getOrNull(it) != null }
                } ==
                true
        }
    }

/** The pembahasan of one question: options or statements with the key, the answers in words, and the text. */
@Composable
private fun Revealed(
    item: ReviewItem,
    kind: ResponseKind,
    answeredHere: Boolean,
    picked: AnswerValue?,
) {
    val single = if (answeredHere) (picked as? AnswerValue.Single)?.option else item.selectedOption
    val multiple = if (answeredHere) (picked as? AnswerValue.Multiple)?.options.orEmpty() else item.selectedOptions
    val statements = if (answeredHere) (picked as? AnswerValue.Statement)?.labels.orEmpty() else item.statementAnswers
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        if (kind == ResponseKind.Statement) {
            StatementReview(item = item, answers = statements, answeredHere = answeredHere)
        } else {
            OptionReview(
                options = item.question.options,
                multiple =
                    item.questionType == "multiple_select" || multiple.isNotEmpty() || item.correctOptions.isNotEmpty(),
                picked =
                    if (item.questionType == "multiple_select" ||
                        multiple.isNotEmpty()
                    ) {
                        multiple
                    } else {
                        listOfNotNull(single)
                    },
                keys = item.correctOptions.ifEmpty { listOfNotNull(item.correctOption) },
            )
        }
        AnswerSummary(
            answeredHere = answeredHere,
            yours = answerText(item, kind, single, multiple, statements),
            key = answerText(item, kind, item.correctOption, item.correctOptions, item.statementCorrectAnswers),
        )
        Explanation(item)
    }
}

@Composable
private fun StatementReview(
    item: ReviewItem,
    answers: List<Int?>,
    answeredHere: Boolean,
) {
    val colors = GotestTheme.colors
    val labels = item.question.answerLabels
    val who = stringResource(if (answeredHere) R.string.review_answer_during else R.string.review_answer_yours)
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        item.question.statements.forEachIndexed { row, statement ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, MaterialTheme.shapes.medium)
                        .padding(GotestTheme.spacing.md),
            ) {
                RichText(
                    content = statement.text,
                    isHtml = true,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                val yours =
                    labels.getOrNull(answers.getOrNull(row) ?: -1)?.text ?: stringResource(R.string.review_not_answered)
                val key =
                    labels.getOrNull(item.statementCorrectAnswers.getOrNull(row) ?: -1)?.text
                        ?: stringResource(R.string.review_dash)
                Text(
                    text = stringResource(R.string.review_answer_line, who, yours),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.review_answer_correct_line, key),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.statusSuccess,
                )
            }
        }
    }
}

@Composable
private fun AnswerSummary(
    answeredHere: Boolean,
    yours: String,
    key: String,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.bgSubtle,
        verticalSpacing = GotestTheme.spacing.sm,
    ) {
        Text(
            text =
                stringResource(
                    if (answeredHere) R.string.review_answer_during_title else R.string.review_answer_yours_title,
                ),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Text(text = yours, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Text(
            text = stringResource(R.string.review_answer_correct_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Text(text = key, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun Explanation(item: ReviewItem) {
    val colors = GotestTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.review_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        if (item.explanation.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.review_explanation_missing),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        } else {
            RichText(
                content = item.explanation,
                isHtml = true,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        mediaUrl(item.explanationMediaPath)?.let { ExamImage(url = it, description = "") }
    }
}

/** Web `choiceLabel`, `choiceListLabel`, `statementList`. */
@Composable
private fun answerText(
    item: ReviewItem,
    kind: ResponseKind,
    single: Int?,
    multiple: List<Int>,
    statements: List<Int?>,
): String {
    val none = stringResource(R.string.review_not_answered)
    val options = item.question.options
    return when (kind) {
        ResponseKind.Multiple -> {
            if (multiple.isEmpty()) none else multiple.joinToString(", ") { choice(options, it) }
        }

        ResponseKind.Single -> {
            single?.let { choice(options, it) } ?: none
        }

        ResponseKind.Statement -> {
            val rows = item.question.statements
            val labels = item.question.answerLabels
            if (rows.isEmpty()) {
                if (statements.isEmpty()) none else statements.joinToString(", ") { it?.toString() ?: "—" }
            } else {
                rows
                    .mapIndexed { index, row ->
                        val label =
                            statements.getOrNull(index)?.let { labels.getOrNull(it)?.text ?: it.toString() } ?: "—"
                        "${stripHtml(
                            row.text,
                        ).ifEmpty { stringResource(R.string.review_statement_number, index + 1) }}: $label"
                    }.joinToString(" · ")
            }
        }
    }
}

private fun choice(
    options: List<OptionText>,
    index: Int,
): String {
    val text = stripHtml(options.getOrNull(index)?.text)
    return if (text.isNotEmpty()) "${optionLetter(index)}. $text" else optionLetter(index)
}

private fun stripHtml(value: String?): String =
    (value ?: "").replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()

/** Web `formatScore`: whole numbers as they are, otherwise one decimal. */
fun formatScore(value: Double): String =
    if (value % 1.0 ==
        0.0
    ) {
        value.toLong().toString()
    } else {
        "%.1f".format(java.util.Locale.ROOT, value).removeSuffix(".0")
    }

/** The options with what was picked and the key (web QuestionOptionReview). */
@Composable
private fun OptionReview(
    options: List<OptionText>,
    multiple: Boolean,
    picked: List<Int>,
    keys: List<Int>,
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text =
                stringResource(R.string.review_options) +
                    if (multiple) " " + stringResource(R.string.review_options_multiple) else "",
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        if (multiple) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
                GotestChip(text = stringResource(R.string.review_badge_right_picked), tone = GotestChipTone.Green)
                GotestChip(text = stringResource(R.string.review_badge_wrong_picked), tone = GotestChipTone.Red)
                GotestChip(text = stringResource(R.string.review_badge_key_missed), tone = GotestChipTone.Yellow)
            }
        }
        options.forEachIndexed { index, option ->
            OptionReviewRow(
                index = index,
                option = option,
                state = optionState(index in picked, index in keys, multiple),
            )
        }
    }
}

/** Web QuestionOptionReview badges: picked and right, picked and wrong, the key not picked, or nothing. */
private fun optionState(
    picked: Boolean,
    key: Boolean,
    multiple: Boolean,
): Pair<Int?, GotestChipTone> =
    when {
        picked && key -> {
            (if (multiple) R.string.review_yours_right else R.string.review_yours_and_key) to
                GotestChipTone.Green
        }

        picked -> {
            R.string.review_yours_wrong to GotestChipTone.Red
        }

        key && multiple -> {
            R.string.review_key_missed to GotestChipTone.Yellow
        }

        key -> {
            R.string.review_key to GotestChipTone.Green
        }

        else -> {
            null to GotestChipTone.Grey
        }
    }

@Composable
private fun OptionReviewRow(
    index: Int,
    option: OptionText,
    state: Pair<Int?, GotestChipTone>,
) {
    val colors = GotestTheme.colors
    val (badge, tone) = state
    val border =
        when (tone) {
            GotestChipTone.Green -> colors.statusSuccess
            GotestChipTone.Red -> colors.statusDanger
            GotestChipTone.Yellow -> colors.statusWarning
            else -> colors.borderSubtle
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(GotestTheme.sizes.strokeThin, border, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(24.dp).background(colors.bgSubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = optionLetter(index),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textPrimary,
                )
            }
            RichText(
                content = option.text,
                isHtml = true,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        mediaUrl(option.mediaPath)?.let {
            ExamImage(url = it, description = stringResource(R.string.runner_option_image, optionLetter(index)))
        }
        badge?.let { GotestChip(text = stringResource(it), tone = tone) }
    }
}

private const val HEADER_ROWS = 2
