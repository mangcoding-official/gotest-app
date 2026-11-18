package com.gotest.app.feature.ruang.ui.work

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.ui.components.ScreenPinning
import com.gotest.app.feature.ruang.ui.components.rememberScreenPinning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExamWorkRoute(
    onOpenResult: (code: String, attemptId: Long) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamWorkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinning = rememberScreenPinning()
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
    val tracker = rememberLeaveTracker(pinning = pinning, active = guarded, onReturn = viewModel::onReturned)
    ExamWorkScreen(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
    if (tracker.needsPin && !uiState.showExit) {
        PinPrompt(pinning = pinning, onExit = { viewModel.onEvent(ExamWorkEvent.OpenExit) })
    }
}

/**
 * Leaving the Ujian (Aturan 12.13, API-K8 A): the app is no longer in front (another app, home, the
 * screen turned off) or the pinned screen was left. When the student is back with the screen pinned,
 * the time away goes to [onReturn], which reports two seconds or more.
 */
@Stable
class LeaveTracker(
    private val pinning: ScreenPinning,
) {
    /** Counted only while an Ujian can be worked on (not a quiz, not locked). */
    var active: Boolean = false
    var onReturn: (awayMs: Long, leftAtElapsed: Long) -> Unit = { _, _ -> }

    private var pinnedOnce by mutableStateOf(pinning.isPinned)
    private var awaySince by mutableStateOf<Long?>(null)
    private var inFront = true

    /** The screen must be pinned before work starts, and again after the pinned screen was left. */
    val needsPin: Boolean get() = active && !pinning.isPinned && (awaySince != null || !pinnedOnce)

    fun onStop() {
        inFront = false
        leave()
    }

    fun onStart() {
        inFront = true
        comeBack()
    }

    /** Called every half second: pinning has no listener on Android. */
    fun poll() {
        if (pinning.isPinned) {
            pinnedOnce = true
            comeBack()
        } else {
            leave()
        }
    }

    private fun leave() {
        if (active && pinnedOnce && awaySince == null) awaySince = SystemClock.elapsedRealtime()
    }

    private fun comeBack() {
        val since = awaySince ?: return
        if (!inFront || !pinning.isPinned) return
        awaySince = null
        onReturn(SystemClock.elapsedRealtime() - since, since)
    }
}

@Composable
private fun rememberLeaveTracker(
    pinning: ScreenPinning,
    active: Boolean,
    onReturn: (awayMs: Long, leftAtElapsed: Long) -> Unit,
): LeaveTracker {
    val tracker = remember(pinning) { LeaveTracker(pinning) }
    SideEffect {
        tracker.active = active
        tracker.onReturn = onReturn
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

/** Android's screen pinning, asked before work starts and again after the pinned screen was left. */
@Composable
private fun PinPrompt(
    pinning: ScreenPinning,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var asking by remember { mutableStateOf(false) }
    if (asking) return
    GotestDialog(
        title = stringResource(R.string.exam_pin_title),
        message = stringResource(R.string.exam_pin_message),
        confirmText = stringResource(R.string.exam_pin_confirm),
        onConfirm = {
            asking = true
            scope.launch {
                pinning.request()
                asking = false
            }
        },
        onDismissRequest = {},
        dismissText = stringResource(R.string.work_exit),
        onDismiss = onExit,
    )
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
