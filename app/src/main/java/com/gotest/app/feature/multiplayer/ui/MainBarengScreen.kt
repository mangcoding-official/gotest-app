package com.gotest.app.feature.multiplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.problemText
import com.gotest.app.feature.multiplayer.ui.hub.BusyDialog
import com.gotest.app.feature.multiplayer.ui.hub.CreateGameDialog
import com.gotest.app.feature.multiplayer.ui.hub.HubContent
import com.gotest.app.feature.multiplayer.ui.hub.JoinGameDialog
import com.gotest.app.feature.multiplayer.ui.hub.PremiumDialog
import com.gotest.app.feature.multiplayer.ui.lobby.LobbyActions
import com.gotest.app.feature.multiplayer.ui.lobby.LobbyContent
import com.gotest.app.feature.multiplayer.ui.play.CountdownContent
import com.gotest.app.feature.multiplayer.ui.play.PlayContent
import com.gotest.app.feature.multiplayer.ui.result.ResultActions
import com.gotest.app.feature.multiplayer.ui.result.ResultContent
import com.gotest.app.feature.multiplayer.ui.result.WaitingContent
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone

/** Main Bareng of one space; [onOpenSpace] opens the hub of the space a running game belongs to. */
@Composable
fun MainBarengRoute(
    onBack: () -> Unit,
    onOpenSpace: (String?) -> Unit,
    onUpgrade: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: MainBarengViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is MainBarengEffect.OpenSpace -> onOpenSpace(effect.space)
        }
    }
    BackHandler(enabled = uiState.screen != GameScreen.Hub) {
        when (uiState.screen) {
            GameScreen.Play -> viewModel.onPlay(PlayEvent.Modal(PlayModal.Leave))
            GameScreen.Waiting -> viewModel.backToHub(fromWaiting = true)
            else -> viewModel.backToHub()
        }
    }
    MainBarengScreen(
        uiState = uiState,
        // The screens get what the view model does, not the view model itself (state hoisting).
        actions = remember(viewModel) { object : MainBarengActions by viewModel {} },
        onBack = onBack,
        onUpgrade = onUpgrade,
        modifier = modifier,
    )
}

@Composable
private fun MainBarengScreen(
    uiState: MainBarengUiState,
    actions: MainBarengActions,
    onBack: () -> Unit,
    onUpgrade: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val game = uiState.game
    val play = uiState.play
    when {
        uiState.screen == GameScreen.Countdown -> {
            CountdownContent(secondsLeft = uiState.secondsLeft, modifier = modifier)
        }

        uiState.screen == GameScreen.Play && game != null && play != null -> {
            PlayContent(
                game = game,
                play = play,
                secondsLeft = uiState.secondsLeft,
                onEvent = actions::onPlay,
                modifier = modifier,
            )
        }

        else -> {
            HubFrame(uiState = uiState, actions = actions, onBack = onBack, onUpgrade = onUpgrade, modifier = modifier)
        }
    }
}

/** Every step but the countdown and answering: the app bar, the step, and the hub's dialogs. */
@Composable
private fun HubFrame(
    uiState: MainBarengUiState,
    actions: MainBarengActions,
    onBack: () -> Unit,
    onUpgrade: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.multiplayer_title),
                navigationIcon = {
                    GotestBackButton(
                        onClick = {
                            if (uiState.screen ==
                                GameScreen.Hub
                            ) {
                                onBack()
                            } else {
                                actions.backToHub(uiState.screen == GameScreen.Waiting)
                            }
                        },
                    )
                },
            )
        },
    ) { padding ->
        when {
            uiState.loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            uiState.loadFailed -> {
                GotestErrorView(onRetry = actions::load, modifier = Modifier.padding(padding))
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .fillMaxSize()
                            // Each step starts at its top, as a new page does on the web.
                            .verticalScroll(key(uiState.screen) { rememberScrollState() })
                            .padding(
                                horizontal = GotestTheme.spacing.screenHorizontal,
                                vertical = GotestTheme.spacing.lg,
                            ),
                    verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
                ) {
                    ScreenBody(uiState = uiState, actions = actions)
                }
            }
        }
        HubDialogs(uiState = uiState, actions = actions, onUpgrade = onUpgrade)
    }
}

@Composable
private fun ScreenBody(
    uiState: MainBarengUiState,
    actions: MainBarengActions,
) {
    val game = uiState.game
    val result = uiState.result
    when {
        uiState.screen == GameScreen.Lobby && game != null -> {
            LobbyContent(
                game = game,
                secondsLeft = uiState.secondsLeft,
                busy = uiState.busy,
                error = uiState.error,
                actions =
                    LobbyActions(
                        onStart = actions::startGame,
                        onCancel = actions::cancelGame,
                        onLeave = actions::leaveLobby,
                        onBack = { actions.backToHub() },
                        onCreateAgain = actions::createAgain,
                    ),
            )
        }

        uiState.screen == GameScreen.Waiting && game != null -> {
            WaitingContent(
                game = game,
                secondsLeft = uiState.secondsLeft,
                onBack = { actions.backToHub(fromWaiting = true) },
            )
        }

        uiState.screen == GameScreen.Result && game != null && result != null -> {
            ResultContent(
                game = game,
                result = result,
                busy = uiState.busy,
                error = uiState.error,
                actions = ResultActions(onPlayAgain = actions::playAgain, onBack = { actions.backToHub() }),
            )
        }

        else -> {
            uiState.flash?.let { NoticeBox(text = stringResource(flashText(it)), tone = NoticeTone.Info) }
            // The waiting room shows its own refusals; the hub shows them here (Aturan 8.8).
            uiState.error?.let {
                MbAlert(
                    type = MbAlertType.Warning,
                    title = stringResource(R.string.multiplayer_error_title),
                    message = problemText(it),
                )
            }
            HubContent(
                uiState = uiState,
                onCreate = actions::openCreate,
                onJoin = actions::openJoin,
                onResume = actions::resume,
                onViewResult = actions::viewResult,
            )
        }
    }
}

@Composable
private fun HubDialogs(
    uiState: MainBarengUiState,
    actions: MainBarengActions,
    onUpgrade: (() -> Unit)?,
) {
    when (uiState.dialog) {
        HubDialog.Create -> {
            CreateGameDialog(
                categories = uiState.hub?.options.orEmpty(),
                draft = uiState.draft,
                status = uiState.create,
                onDraft = actions::updateDraft,
                onSubmit = actions::create,
                onClose = actions::closeDialog,
            )
        }

        HubDialog.Join -> {
            JoinGameDialog(
                join = uiState.join,
                onCode = actions::onJoinCode,
                onSubmit = actions::submitJoin,
                onResume = actions::resume,
                onClose = actions::closeDialog,
            )
        }

        HubDialog.Premium -> {
            PremiumDialog(onJoin = { actions.openJoin() }, onUpgrade = onUpgrade, onClose = actions::closeDialog)
        }

        HubDialog.Busy -> {
            BusyDialog(
                game = uiState.active ?: uiState.game,
                onResume = actions::resume,
                onClose = actions::closeDialog,
            )
        }

        HubDialog.None -> {}
    }
}

private fun flashText(flash: HubFlash): Int =
    when (flash) {
        HubFlash.HubFailed -> R.string.multiplayer_flash_hub_failed
        HubFlash.Removed -> R.string.multiplayer_flash_removed
        HubFlash.LeftLobby -> R.string.multiplayer_flash_left_lobby
        HubFlash.QuestionsFailed -> R.string.multiplayer_flash_questions_failed
        HubFlash.LeftGame -> R.string.multiplayer_flash_left_game
        HubFlash.ResultLater -> R.string.multiplayer_flash_result_later
    }
