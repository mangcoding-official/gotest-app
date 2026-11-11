package com.gotest.app.feature.assistant.ui.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconBadgeColors
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AttachmentRules
import com.gotest.app.feature.assistant.ui.components.AssistantAttachmentSheet
import com.gotest.app.feature.assistant.ui.components.AssistantComposer
import com.gotest.app.feature.assistant.ui.components.AssistantDayDivider
import com.gotest.app.feature.assistant.ui.components.AssistantHistoryPanel
import com.gotest.app.feature.assistant.ui.components.AssistantMessageItem
import com.gotest.app.feature.assistant.ui.components.AssistantTopBar
import com.gotest.app.feature.assistant.ui.components.AssistantTypingIndicator
import com.gotest.app.feature.assistant.ui.components.AssistantVoiceOverlay
import com.gotest.app.feature.assistant.ui.components.AssistantWelcome
import com.gotest.app.feature.assistant.ui.components.assistantBackgroundBrush
import kotlinx.coroutines.launch

@Composable
fun AssistantRoute(
    onBack: () -> Unit,
    onOpenPractice: (packageId: String) -> Unit,
    onOpenTryout: (packageId: String) -> Unit,
    onBrowseTryouts: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val showMessage: (Int) -> Unit = { messageRes ->
        scope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            AssistantEffect.FocusInput -> focusRequester.requestFocus()
            is AssistantEffect.ShowMessage -> showMessage(effect.messageRes)
        }
    }
    val attachedCount = uiState.composer?.attachments?.size ?: 0
    val pickAttachment =
        rememberAttachmentPicker(
            remainingSlots = AttachmentRules.MAX_FILES - attachedCount,
            onPick = { uris -> viewModel.onEvent(AssistantUiEvent.AttachmentsPicked(uris)) },
            onMessage = showMessage,
        )
    AssistantScreen(
        uiState = uiState,
        history = history,
        input = viewModel.input,
        historyQuery = viewModel.historyQuery,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onPackageClick = { pkg ->
            when (pkg.type) {
                PackageType.Practice -> onOpenPractice(pkg.id)
                PackageType.Tryout -> onOpenTryout(pkg.id)
            }
        },
        onActionClick = { action ->
            when (action) {
                is AssistantAction.StartTryout -> onOpenTryout(action.packageId)
                AssistantAction.BrowseTryouts -> onBrowseTryouts()
                AssistantAction.OpenLeaderboard -> onOpenLeaderboard()
            }
        },
        onPickAttachment = pickAttachment,
        snackbarHostState = snackbarHostState,
        focusRequester = focusRequester,
        modifier = modifier,
    )
}

/**
 * "Asisten Gotest": welcome (frame 11855) until the first question, then the conversation
 * (frame 11990), with the voice overlay (frame 12266), "Tambah Lampiran" (frame 22009) or
 * "Riwayat Percakapan" (frame 21622) on top.
 */
@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    history: ConversationHistoryUiState,
    input: String,
    historyQuery: String,
    onEvent: (AssistantUiEvent) -> Unit,
    onBack: () -> Unit,
    onPackageClick: (StudyPackage) -> Unit,
    onActionClick: (AssistantAction) -> Unit,
    onPickAttachment: (AttachmentSource) -> Unit,
    snackbarHostState: SnackbarHostState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val composer = uiState.composer
    val voice = composer?.voice
    BackHandler(enabled = voice != null) { onEvent(AssistantUiEvent.CancelVoice) }
    BackHandler(enabled = history.isOpen) { onEvent(AssistantUiEvent.CloseHistory) }
    Box(modifier = modifier.fillMaxSize().background(assistantBackgroundBrush())) {
        Scaffold(
            topBar = {
                AssistantTopBar(
                    navigationIcon = GotestIcons.Linear.ArrowLeft,
                    navigationContentDescription = stringResource(R.string.cd_back),
                    onNavigationClick = onBack,
                    onMenuClick = { onEvent(AssistantUiEvent.OpenHistory) },
                )
            },
            bottomBar = {
                if (composer != null) {
                    AssistantComposer(
                        input = input,
                        prompts = if (uiState is AssistantUiState.Success) conversationPrompts() else emptyList(),
                        attachments = composer.attachments,
                        focusRequester = focusRequester,
                        onInputChange = { onEvent(AssistantUiEvent.InputChange(it)) },
                        onSend = { onEvent(AssistantUiEvent.Send) },
                        onPromptClick = { onEvent(AssistantUiEvent.SendPrompt(it)) },
                        onMicClick = { onEvent(AssistantUiEvent.StartVoice) },
                        onAttachClick = { onEvent(AssistantUiEvent.OpenAttachmentSheet) },
                        onRemoveAttachment = { onEvent(AssistantUiEvent.RemoveAttachment(it.uri)) },
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            AssistantContent(
                uiState = uiState,
                onEvent = onEvent,
                onPackageClick = onPackageClick,
                onActionClick = onActionClick,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
        if (voice != null) {
            AssistantVoiceOverlay(
                voice = voice,
                onMenuClick = { onEvent(AssistantUiEvent.OpenHistory) },
                onTypeClick = { onEvent(AssistantUiEvent.SwitchToKeyboard) },
                onDoneClick = { onEvent(AssistantUiEvent.FinishVoice) },
                onCancelClick = { onEvent(AssistantUiEvent.CancelVoice) },
            )
        }
        AssistantHistoryPanel(
            state = history,
            query = historyQuery,
            onQueryChange = { onEvent(AssistantUiEvent.HistoryQueryChange(it)) },
            onNewConversation = { onEvent(AssistantUiEvent.NewConversation) },
            onConversationClick = { onEvent(AssistantUiEvent.OpenConversation(it)) },
            onClearClick = { onEvent(AssistantUiEvent.ClearHistory) },
            onClose = { onEvent(AssistantUiEvent.CloseHistory) },
        )
    }
    AssistantOverlays(
        isAttachmentSheetOpen = composer?.isAttachmentSheetOpen == true,
        isConfirmingClear = history.isConfirmingClear,
        onEvent = onEvent,
        onPickAttachment = onPickAttachment,
    )
}

/** "Tambah Lampiran" and the clear-history confirmation, both in their own windows. */
@Composable
private fun AssistantOverlays(
    isAttachmentSheetOpen: Boolean,
    isConfirmingClear: Boolean,
    onEvent: (AssistantUiEvent) -> Unit,
    onPickAttachment: (AttachmentSource) -> Unit,
) {
    if (isAttachmentSheetOpen) {
        AssistantAttachmentSheet(
            onSourceClick = onPickAttachment,
            onDismiss = { onEvent(AssistantUiEvent.DismissAttachmentSheet) },
        )
    }
    if (isConfirmingClear) {
        // Not in the design: the app's standard confirmation with a danger badge.
        GotestDialog(
            title = stringResource(R.string.assistant_history_clear_title),
            message = stringResource(R.string.assistant_history_clear_message),
            confirmText = stringResource(R.string.assistant_history_clear_confirm),
            onConfirm = { onEvent(AssistantUiEvent.ConfirmClearHistory) },
            onDismissRequest = { onEvent(AssistantUiEvent.DismissClearHistory) },
            icon = GotestIcons.Linear.Trash,
            iconColors = GotestIconBadgeColors(GotestTheme.colors.statusDangerSoft, GotestTheme.colors.statusDanger),
            dismissText = stringResource(R.string.assistant_history_clear_cancel),
        )
    }
}

/** Prompt chips above the input once the conversation has started. */
@Composable
private fun conversationPrompts(): List<String> =
    listOf(
        stringResource(R.string.assistant_prompt_unfinished),
        stringResource(R.string.assistant_prompt_rank),
        stringResource(R.string.assistant_prompt_tryout),
    )

@Composable
private fun AssistantContent(
    uiState: AssistantUiState,
    onEvent: (AssistantUiEvent) -> Unit,
    onPackageClick: (StudyPackage) -> Unit,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AssistantUiState.Loading -> {
            GotestLoadingView(modifier = modifier)
        }

        AssistantUiState.Error -> {
            GotestErrorView(onRetry = { onEvent(AssistantUiEvent.Retry) }, modifier = modifier)
        }

        is AssistantUiState.Empty -> {
            AssistantWelcome(
                firstName = uiState.firstName,
                onSuggestionClick = { onEvent(AssistantUiEvent.SendPrompt(it)) },
                modifier = modifier,
            )
        }

        is AssistantUiState.Success -> {
            // A conversation opened from the history starts at its newest message, not mid-scroll.
            key(uiState.conversationId) {
                Conversation(
                    items = uiState.items,
                    onPackageClick = onPackageClick,
                    onActionClick = onActionClick,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun Conversation(
    items: List<AssistantChatItem>,
    onPackageClick: (StudyPackage) -> Unit,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = items.lastIndex.coerceAtLeast(0))
    // Keep the newest message (or the typing dots) in view.
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(GotestTheme.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
    ) {
        items(items = items, key = { it.key }, contentType = { it::class }) { item ->
            when (item) {
                is AssistantChatItem.DayDivider -> {
                    AssistantDayDivider(date = item.date)
                }

                is AssistantChatItem.Message -> {
                    AssistantMessageItem(
                        message = item.message,
                        onPackageClick = onPackageClick,
                        onActionClick = onActionClick,
                    )
                }

                AssistantChatItem.Typing -> {
                    AssistantTypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun AssistantPreviewScreen(
    uiState: AssistantUiState,
    history: ConversationHistoryUiState = ConversationHistoryUiState(isLoading = false),
) {
    GotestTheme {
        AssistantScreen(
            uiState = uiState,
            history = history,
            input = "",
            historyQuery = "",
            onEvent = {},
            onBack = {},
            onPackageClick = {},
            onActionClick = {},
            onPickAttachment = {},
            snackbarHostState = remember { SnackbarHostState() },
            focusRequester = remember { FocusRequester() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenWelcomePreview() {
    AssistantPreviewScreen(uiState = AssistantUiState.Empty(firstName = "Gilang", composer = ComposerState()))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenConversationPreview() {
    AssistantPreviewScreen(
        uiState =
            AssistantUiState.Success(
                conversationId = "conv_seed_utbk",
                items = AssistantPreviewData.conversation,
                composer = ComposerState(),
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenAttachmentsPreview() {
    AssistantPreviewScreen(
        uiState =
            AssistantUiState.Success(
                conversationId = "conv_seed_utbk",
                items = AssistantPreviewData.conversation,
                composer = ComposerState(attachments = AssistantPreviewData.attachments),
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenHistoryPreview() {
    AssistantPreviewScreen(
        uiState =
            AssistantUiState.Success(
                conversationId = "conv_seed_utbk",
                items = AssistantPreviewData.conversation,
                composer = ComposerState(),
            ),
        history = AssistantPreviewData.history,
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenVoicePreview() {
    AssistantPreviewScreen(
        uiState =
            AssistantUiState.Success(
                conversationId = "conv_seed_utbk",
                items = AssistantPreviewData.conversation,
                composer = ComposerState(voice = VoiceInputState(transcript = AssistantPreviewData.VOICE_TRANSCRIPT)),
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenLoadingPreview() {
    AssistantPreviewScreen(uiState = AssistantUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun AssistantScreenErrorPreview() {
    AssistantPreviewScreen(uiState = AssistantUiState.Error)
}
