package com.gotest.app.feature.ruang.ui.work

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.ui.components.PinGuidance
import com.gotest.app.feature.ruang.ui.components.PinRequestState
import com.gotest.app.feature.ruang.ui.components.PinState
import com.gotest.app.feature.ruang.ui.components.rememberPinRequest
import com.gotest.app.feature.ruang.ui.components.rememberScreenPinning
import kotlinx.coroutines.delay

@Composable
fun ExamWorkRoute(
    onOpenResult: (code: String, attemptId: Long) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamWorkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinning = rememberScreenPinning()
    val pinRequest = rememberPinRequest(pinning)
    val guarded = uiState.page?.isQuiz == false && !uiState.locked
    ObserveAsEvents(viewModel.effects) { effect ->
        pinning.release()
        when (effect) {
            ExamWorkEffect.OpenResult -> onOpenResult(viewModel.code, viewModel.attemptId)
            ExamWorkEffect.Leave -> onLeave()
        }
    }
    KeepScreenOn(enabled = uiState.page?.isQuiz == false)
    BackHandler { viewModel.onEvent(ExamWorkEvent.OpenExit) }
    rememberLeaveTracker(
        pinning = pinning,
        active = guarded,
        listener =
            LeaveListener(
                onLeave = viewModel::onLeave,
                onReturn = viewModel::onReturn,
                onPinnedChange = viewModel::onPinnedChange,
            ),
    )
    ExamWorkScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        pinGate = {
            PinGate(
                state = pinRequest,
                onPin = { pinRequest.request {} },
                onExit = { viewModel.onEvent(ExamWorkEvent.OpenExit) },
            )
        },
    )
}

/** What the screen hears about leaving and pinning. */
data class LeaveListener(
    val onLeave: () -> Unit,
    val onReturn: () -> Unit,
    val onPinnedChange: (Boolean) -> Unit,
)

/**
 * Leaving the Ujian (Aturan 12.13, API-K8 A): the app is no longer in front (another app, home, the
 * screen turned off), or the pinned screen was left. It counts from the moment the paper was first
 * shown, which needs the screen pinned; before that the paper is covered, so a trip to Settings to
 * turn pinning on is not a violation. The moment itself is kept by the view model (T-026 A2b review).
 */
@Stable
class LeaveTracker(
    private val pinning: PinState,
) {
    /** An Ujian that can still be worked on (not a quiz, not locked). */
    var active: Boolean = false
    var listener: LeaveListener = LeaveListener({}, {}, {})

    private var paperShown = pinning.isPinned
    private var inFront = true
    private var lastPinned: Boolean? = null

    fun onStop() {
        inFront = false
        if (active && paperShown) listener.onLeave()
    }

    fun onStart() {
        inFront = true
        if (pinning.isPinned || !paperShown) listener.onReturn()
    }

    /** Called every half second: pinning has no listener on Android. */
    fun poll() {
        val pinned = pinning.isPinned
        if (pinned != lastPinned) {
            lastPinned = pinned
            listener.onPinnedChange(pinned)
            if (pinned) {
                paperShown = true
                if (inFront) listener.onReturn()
            } else if (active && paperShown) {
                listener.onLeave()
            }
        }
    }
}

@Composable
private fun rememberLeaveTracker(
    pinning: PinState,
    active: Boolean,
    listener: LeaveListener,
): LeaveTracker {
    val tracker = remember(pinning) { LeaveTracker(pinning) }
    SideEffect {
        tracker.active = active
        tracker.listener = listener
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, tracker) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) tracker.onStop()
                if (event == Lifecycle.Event.ON_START) tracker.onStart()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(tracker) {
        while (true) {
            tracker.poll()
            delay(PIN_POLL_MS)
        }
    }
    return tracker
}

/**
 * Covers the paper until the screen is pinned (T-026 A2b review 1): before work starts, and again after
 * the pinned screen was left. Nothing of the Ujian shows here.
 */
@Composable
private fun PinGate(
    state: PinRequestState,
    onPin: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exam_pin_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.exam_pin_message),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        GotestButton(
            text = stringResource(R.string.exam_pin_confirm),
            onClick = onPin,
            modifier = Modifier.fillMaxWidth(),
        )
        PinGuidance(state = state)
        GotestButton(
            text = stringResource(R.string.work_exit),
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

/** The web keeps the screen awake with Wake Lock during an Ujian; here the window flag does it. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private const val PIN_POLL_MS = 500L
