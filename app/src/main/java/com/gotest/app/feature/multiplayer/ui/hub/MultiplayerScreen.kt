package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.CreateBlock
import com.gotest.app.feature.multiplayer.ui.components.MbCodeBoxes
import com.gotest.app.feature.multiplayer.ui.components.MbCreateBlockedDialog
import com.gotest.app.feature.multiplayer.ui.components.MbCreateBlockedDialogContent
import com.gotest.app.feature.multiplayer.ui.components.MbIconTile
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewOverlay
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewSheet
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData

@Composable
fun MultiplayerRoute(
    onBack: () -> Unit,
    onCreateGame: () -> Unit,
    onUpgrade: () -> Unit,
    onOpenGame: (code: String, screen: GameScreen) -> Unit,
    onOpenResult: (code: String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenGameDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultiplayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            MultiplayerEffect.OpenCreateGame -> onCreateGame()
            MultiplayerEffect.OpenUpgrade -> onUpgrade()
            is MultiplayerEffect.OpenGame -> onOpenGame(effect.code, effect.screen)
        }
    }
    MultiplayerScreen(
        uiState = uiState,
        overlay = overlay,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenResult = onOpenResult,
        onSeeAllHistory = onSeeAllHistory,
        onOpenGameDetail = onOpenGameDetail,
        modifier = modifier,
    )
}

@Composable
fun MultiplayerScreen(
    uiState: MultiplayerUiState,
    overlay: MultiplayerOverlayState,
    onEvent: (MultiplayerUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenResult: (code: String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenGameDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(
                title = stringResource(R.string.multiplayer_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
            when (uiState) {
                MultiplayerUiState.Loading -> {
                    GotestLoadingView()
                }

                MultiplayerUiState.Error -> {
                    GotestErrorView(
                        onRetry = { onEvent(MultiplayerUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is MultiplayerUiState.Success -> {
                    HubContent(
                        state = uiState,
                        onEvent = onEvent,
                        onOpenResult = onOpenResult,
                        onSeeAllHistory = onSeeAllHistory,
                        onOpenGameDetail = onOpenGameDetail,
                    )
                }
            }
        }
        overlay.dialog?.let { block ->
            MbCreateBlockedDialog(
                block = block,
                onUpgrade = { onEvent(MultiplayerUiEvent.UpgradeClick) },
                onJoinWithCode = { onEvent(MultiplayerUiEvent.OpenJoinSheet) },
                onReturnToGame = { onEvent(MultiplayerUiEvent.ReturnToGame(it)) },
                onDismissRequest = { onEvent(MultiplayerUiEvent.DismissDialog) },
            )
        }
        overlay.joinSheet?.let { JoinGameSheet(state = it, onEvent = onEvent) }
    }
}

@Composable
private fun HubContent(
    state: MultiplayerUiState.Success,
    onEvent: (MultiplayerUiEvent) -> Unit,
    onOpenResult: (code: String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenGameDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        IntroCard()
        CreateGameCard(onCreate = { onEvent(MultiplayerUiEvent.CreateGameClick) })
        JoinCodeCard(onJoin = { onEvent(MultiplayerUiEvent.OpenJoinSheet) })
        RulesCard(
            heading = stringResource(R.string.multiplayer_rules_duel_heading),
            title = stringResource(R.string.multiplayer_rules_duel_title),
            rows =
                listOf(
                    R.string.multiplayer_rules_players to R.string.multiplayer_rules_duel_players,
                    R.string.multiplayer_rules_start to R.string.multiplayer_rules_duel_start,
                    R.string.multiplayer_rules_winner to R.string.multiplayer_rules_duel_winner,
                ),
        )
        RulesCard(
            heading = stringResource(R.string.multiplayer_rules_room_heading),
            title = stringResource(R.string.multiplayer_rules_room_title),
            rows =
                listOf(
                    R.string.multiplayer_rules_players to R.string.multiplayer_rules_room_players,
                    R.string.multiplayer_rules_start to R.string.multiplayer_rules_room_start,
                    R.string.multiplayer_rules_ranking to R.string.multiplayer_rules_room_ranking,
                ),
        )
        HistoryCard(
            history = state.history,
            onOpenResult = onOpenResult,
            onSeeAll = onSeeAllHistory,
            onDetail = onOpenGameDetail,
        )
    }
}

@Composable
private fun IntroCard(modifier: Modifier = Modifier) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.xl),
        verticalSpacing = GotestTheme.spacing.md,
    ) {
        GotestChip(
            text = stringResource(R.string.multiplayer_title),
            tone = GotestChipTone.Violet,
            leadingIcon = GotestIcons.Bold.People,
        )
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.multiplayer_intro_title),
                style = MaterialTheme.typography.headlineMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.multiplayer_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun CreateGameCard(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.xl) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MbIconTile(
                icon = GotestIcons.Bold.AddCircle,
                container = colors.bgBrandSoft,
                tint = colors.iconBrandDark,
                size = MultiplayerDimens.featureTile,
                iconSize = MultiplayerDimens.featureTileIcon,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                GotestChip(
                    text = stringResource(R.string.multiplayer_premium),
                    tone = GotestChipTone.Yellow,
                    leadingIcon = GotestIcons.Bold.Award,
                )
            }
        }
        CardHeading(
            title = stringResource(R.string.multiplayer_create_title),
            body = stringResource(R.string.multiplayer_create_body),
        )
        GotestButton(
            text = stringResource(R.string.multiplayer_create_title),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
            size = GotestButtonSize.Medium,
        )
    }
}

@Composable
private fun JoinCodeCard(
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        MbIconTile(
            icon = GotestIcons.Bold.Link,
            container = colors.bgBlueSoft,
            tint = colors.bgBlue,
            size = MultiplayerDimens.featureTile,
            iconSize = MultiplayerDimens.featureTileIcon,
            modifier = Modifier.padding(bottom = GotestTheme.spacing.sm),
        )
        CardHeading(
            title = stringResource(R.string.multiplayer_join_title),
            body = stringResource(R.string.multiplayer_join_body),
        )
        // The boxes are a shortcut into the join sheet, where the code is typed.
        GotestCard(
            onClick = onJoin,
            showBorder = false,
            contentPadding = PaddingValues(GotestTheme.spacing.none),
            shape = MaterialTheme.shapes.small,
        ) {
            MbCodeBoxes(
                code = "",
                modifier = Modifier.fillMaxWidth(),
                spacing = GotestTheme.spacing.xs + GotestTheme.spacing.xxs,
            )
        }
        GotestButton(
            text = stringResource(R.string.multiplayer_join_action),
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Medium,
        )
    }
}

@Composable
private fun CardHeading(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
    }
}

@Composable
private fun RulesCard(
    heading: String,
    title: String,
    rows: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Text(text = heading, style = MaterialTheme.typography.labelMedium, color = colors.textBrandDark)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                    .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                rows.forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                        Text(
                            text = stringResource(label),
                            modifier = Modifier.weight(RULE_LABEL_WEIGHT),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = stringResource(value),
                            modifier = Modifier.weight(RULE_VALUE_WEIGHT),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

/** Label and value columns of the rules (96 : 222 in the design). */
private const val RULE_LABEL_WEIGHT = 0.3f
private const val RULE_VALUE_WEIGHT = 0.7f

private val previewSuccess =
    MultiplayerUiState.Success(history = MultiplayerPreviewData.history, isPremium = true, activeGame = null)

@Composable
private fun HubPreview(
    uiState: MultiplayerUiState = previewSuccess,
    overlay: MultiplayerOverlayState = MultiplayerOverlayState(),
) {
    MultiplayerScreen(
        uiState = uiState,
        overlay = overlay,
        onEvent = {},
        onBack = {},
        onOpenResult = {},
        onSeeAllHistory = {},
        onOpenGameDetail = {},
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerScreenPreview() {
    GotestTheme { HubPreview() }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerScreenEmptyHistoryPreview() {
    GotestTheme { HubPreview(uiState = previewSuccess.copy(history = emptyList())) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerScreenLoadingPreview() {
    GotestTheme { HubPreview(uiState = MultiplayerUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerScreenErrorPreview() {
    GotestTheme { HubPreview(uiState = MultiplayerUiState.Error) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerPremiumDialogPreview() {
    GotestTheme {
        MbPreviewOverlay(screen = { HubPreview() }) {
            MbCreateBlockedDialogContent(
                block = CreateBlock.PremiumRequired,
                onUpgrade = {},
                onJoinWithCode = {},
                onReturnToGame = {},
                onClose = {},
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerInAnotherGameDialogPreview() {
    GotestTheme {
        MbPreviewOverlay(screen = { HubPreview() }) {
            MbCreateBlockedDialogContent(
                block = CreateBlock.InAnotherGame(MultiplayerPreviewData.hostDuelLobby),
                onUpgrade = {},
                onJoinWithCode = {},
                onReturnToGame = {},
                onClose = {},
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            )
        }
    }
}

@Composable
private fun JoinSheetPreview(state: JoinSheetState) {
    MbPreviewOverlay(screen = { HubPreview() }, overlayAlignment = Alignment.BottomCenter) {
        MbPreviewSheet { JoinGameSheetContent(state = state, onEvent = {}) }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetFoundPreview() {
    GotestTheme {
        JoinSheetPreview(JoinSheetState("KTR4QZ", JoinStatus.Found(MultiplayerPreviewData.roomLobby)))
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetNotFoundPreview() {
    GotestTheme { JoinSheetPreview(JoinSheetState("KTR4QX", JoinStatus.NotFound)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetStartedPreview() {
    GotestTheme { JoinSheetPreview(JoinSheetState("MNB7RT", JoinStatus.AlreadyStarted)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetFullPreview() {
    GotestTheme { JoinSheetPreview(JoinSheetState("FULL10", JoinStatus.Full)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetInactivePreview() {
    GotestTheme { JoinSheetPreview(JoinSheetState("BATAL1", JoinStatus.Inactive)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun JoinSheetInAnotherGamePreview() {
    GotestTheme {
        JoinSheetPreview(
            JoinSheetState(
                "MNB7RT",
                JoinStatus.InAnotherGame(MultiplayerPreviewData.hostDuelLobby),
            ),
        )
    }
}
