package com.gotest.app.feature.multiplayer.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.GameCategory
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSubtest
import com.gotest.app.feature.multiplayer.domain.GameTopic
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbBottomBar
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.gameTypeLabel
import kotlinx.coroutines.launch

@Composable
fun CreateGameRoute(
    onBack: () -> Unit,
    onOpenWaitingRoom: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.multiplayer_create_failed)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is CreateGameEffect.Created -> onOpenWaitingRoom(effect.code)
            CreateGameEffect.Failed -> scope.launch { snackbarHostState.showSnackbar(failedMessage) }
        }
    }
    CreateGameScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun CreateGameScreen(
    uiState: CreateGameUiState,
    onEvent: (CreateGameUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgApp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(
                title = stringResource(R.string.multiplayer_create_screen_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
            when (uiState) {
                CreateGameUiState.Loading -> {
                    GotestLoadingView()
                }

                CreateGameUiState.Error -> {
                    GotestErrorView(onRetry = { onEvent(CreateGameUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
                }

                is CreateGameUiState.Success -> {
                    CreateGameFormContent(
                        form = uiState.form,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                    )
                    MbBottomBar {
                        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                            GotestButton(
                                text = stringResource(R.string.multiplayer_cancel),
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                variant = GotestButtonVariant.Plain,
                            )
                            GotestButton(
                                text = stringResource(R.string.multiplayer_create_submit),
                                onClick = { onEvent(CreateGameUiEvent.Submit) },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.form.canSubmit && !uiState.isSubmitting,
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CreateGameFormContent(
    form: CreateGameForm,
    onEvent: (CreateGameUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.multiplayer_create_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        TypeSection(selected = form.type, onSelect = { onEvent(CreateGameUiEvent.TypeChange(it)) })
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            SelectField(
                label = stringResource(R.string.multiplayer_create_category),
                value = form.category.name,
                options = form.categories.map { it.name },
                onSelect = { onEvent(CreateGameUiEvent.CategoryChange(it)) },
            )
            SelectField(
                label = stringResource(R.string.multiplayer_create_subtest),
                value = form.subtest.name,
                options = form.category.subtests.map { it.name },
                onSelect = { onEvent(CreateGameUiEvent.SubtestChange(it)) },
            )
        }
        TopicSection(form = form, onEvent = onEvent)
        QuestionCountSection(form = form, onEvent = onEvent)
        DurationSection(form = form, onEvent = onEvent)
        if (form.isStockShort) {
            MbAlert(
                type = MbAlertType.Error,
                title = stringResource(R.string.multiplayer_create_stock_title),
                message = stringResource(R.string.multiplayer_create_stock_message, form.availableQuestions),
            )
        }
        SummaryBox(form = form)
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs + GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun HelperText(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) GotestTheme.colors.statusDanger else GotestTheme.colors.textSecondary,
    )
}

@Composable
private fun TypeSection(
    selected: GameType,
    onSelect: (GameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        SectionLabel(text = stringResource(R.string.multiplayer_create_type))
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            GameTypeCard(
                title = gameTypeLabel(GameType.Duel),
                subtitle = stringResource(R.string.multiplayer_create_duel_subtitle),
                selectedIcon = GotestIcons.Bold.User,
                idleIcon = GotestIcons.Linear.User,
                selected = selected == GameType.Duel,
                onClick = { onSelect(GameType.Duel) },
            )
            GameTypeCard(
                title = gameTypeLabel(GameType.Room),
                subtitle = stringResource(R.string.multiplayer_create_room_subtitle),
                selectedIcon = GotestIcons.Bold.People,
                idleIcon = GotestIcons.Linear.People,
                selected = selected == GameType.Room,
                onClick = { onSelect(GameType.Room) },
            )
        }
    }
}

@Composable
private fun TopicSection(
    form: CreateGameForm,
    onEvent: (CreateGameUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        SectionLabel(
            text = stringResource(R.string.multiplayer_create_topic),
            hint = stringResource(R.string.multiplayer_create_topic_hint),
        )
        TopicPicker(
            selected = form.selectedTopics,
            addable = form.addableTopics,
            onAdd = { onEvent(CreateGameUiEvent.TopicAdd(it)) },
            onRemove = { onEvent(CreateGameUiEvent.TopicRemove(it)) },
        )
        HelperText(text = stringResource(R.string.multiplayer_create_topic_helper))
    }
}

@Composable
private fun QuestionCountSection(
    form: CreateGameForm,
    onEvent: (CreateGameUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        SectionLabel(text = stringResource(R.string.multiplayer_detail_question_count))
        QuestionStepper(
            count = form.questionCount,
            isError = form.isStockShort,
            canDecrease = form.canDecrease,
            canIncrease = form.canIncrease,
            onDecrease = { onEvent(CreateGameUiEvent.DecreaseQuestions) },
            onIncrease = { onEvent(CreateGameUiEvent.IncreaseQuestions) },
        )
        HelperText(
            text = stringResource(R.string.multiplayer_create_stock, form.availableQuestions, GameRules.MAX_QUESTIONS),
            isError = form.isStockShort,
        )
    }
}

@Composable
private fun DurationSection(
    form: CreateGameForm,
    onEvent: (CreateGameUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        SectionLabel(text = stringResource(R.string.multiplayer_detail_duration))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            form.durationOptions.forEach { option ->
                DurationChip(
                    minutes = option.minutes,
                    selected = option.minutes == form.durationMinutes,
                    enabled = option.enabled,
                    onClick = { onEvent(CreateGameUiEvent.DurationChange(option.minutes)) },
                )
            }
        }
        val suggested = form.suggestedDuration
        if (form.disabledDurations.isNotEmpty() && suggested != null) {
            HelperText(
                text =
                    stringResource(
                        R.string.multiplayer_create_duration_helper,
                        form.disabledDurations.map { durationLabel(it) }.joinToString(", "),
                        GameRules.MIN_SECONDS_PER_QUESTION,
                        durationLabel(suggested),
                    ),
            )
        }
    }
}

@Composable
private fun SummaryBox(
    form: CreateGameForm,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.multiplayer_create_summary),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
        SummaryRow(stringResource(R.string.multiplayer_detail_type), gameTypeLabel(form.type))
        SummaryRow(stringResource(R.string.multiplayer_detail_subtest), form.subtest.name)
        SummaryRow(
            stringResource(R.string.multiplayer_detail_question_count),
            form.questionCount.toString(),
            isError = form.isStockShort,
        )
        SummaryRow(stringResource(R.string.multiplayer_detail_duration), durationLabel(form.durationMinutes))
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) GotestTheme.colors.statusDanger else GotestTheme.colors.textPrimary,
        )
    }
}

private val previewCategories =
    listOf(
        GameCategory(
            name = "UTBK-SNBT",
            subtests =
                listOf(
                    GameSubtest(
                        name = "Penalaran Matematika",
                        topics = listOf(GameTopic("Aljabar", 24), GameTopic("Logaritma", 53), GameTopic("Peluang", 19)),
                    ),
                ),
        ),
    )

private val previewForm =
    CreateGameForm(categories = previewCategories, selectedTopics = listOf("Aljabar", "Logaritma"))

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CreateGameScreenPreview() {
    GotestTheme {
        CreateGameScreen(uiState = CreateGameUiState.Success(previewForm), onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CreateGameStockShortPreview() {
    GotestTheme {
        CreateGameScreen(
            uiState =
                CreateGameUiState.Success(
                    previewForm.copy(selectedTopics = listOf("Aljabar"), questionCount = 30, durationMinutes = 20),
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CreateGameLoadingPreview() {
    GotestTheme { CreateGameScreen(uiState = CreateGameUiState.Loading, onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CreateGameErrorPreview() {
    GotestTheme { CreateGameScreen(uiState = CreateGameUiState.Error, onEvent = {}, onBack = {}) }
}
