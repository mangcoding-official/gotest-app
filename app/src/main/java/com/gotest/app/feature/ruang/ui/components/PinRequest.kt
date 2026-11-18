package com.gotest.app.feature.ruang.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.theme.GotestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One request to pin the screen at a time (T-026 A2b review 3). Android says nothing when the student
 * declines, and shows nothing at all when pinning is off in Settings; so after a few seconds without
 * pinning the screen explains it (A2b-3, A2b-6) while the request keeps waiting.
 */
@Stable
class PinRequestState(
    private val pinning: ScreenPinning,
    private val scope: CoroutineScope,
) {
    /** A few seconds passed without pinning: the refusal and the Settings guidance show. */
    var slow by mutableStateOf(false)
        private set

    private var job: Job? = null

    val isWaiting: Boolean get() = job?.isActive == true

    /** Asks Android again on every tap (after "No thanks" too); only one wait runs at a time. */
    fun request(onPinned: () -> Unit) {
        if (pinning.isPinned) {
            onPinned()
            return
        }
        pinning.ask()
        if (isWaiting) return
        job =
            scope.launch {
                val timer =
                    launch {
                        delay(SLOW_MS)
                        slow = true
                    }
                val pinned = pinning.awaitPinned()
                timer.cancel()
                if (pinned) {
                    slow = false
                    onPinned()
                }
            }
    }

    private companion object {
        const val SLOW_MS = 5_000L
    }
}

@Composable
fun rememberPinRequest(pinning: ScreenPinning): PinRequestState {
    val scope = rememberCoroutineScope()
    return remember(pinning, scope) { PinRequestState(pinning, scope) }
}

/**
 * Why the paper waits ("Ujian belum dimulai…" before Mulai, [refusal] once the Ujian runs) and how to turn
 * pinning on, with a way to Settings > Security.
 */
@Composable
fun PinGuidance(
    state: PinRequestState,
    modifier: Modifier = Modifier,
    refusal: String = stringResource(R.string.exam_pin_refused),
) {
    if (!state.slow) return
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        NoticeBox(text = refusal, tone = NoticeTone.Danger)
        NoticeBox(text = stringResource(R.string.exam_pin_guidance))
        GotestButton(
            text = stringResource(R.string.exam_pin_settings),
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}
