package com.gotest.app.feature.ruang.ui.components

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Screen pinning for Ujian (API-K8 A): the app asks to pin the screen before an Ujian starts, and
 * leaving the pinned screen counts as leaving the page (Aturan 12.13). Android shows its own
 * "pin this app?" confirmation; the app only starts the Ujian once the screen is really pinned.
 */
class ScreenPinning(
    private val activity: Activity?,
) {
    val isPinned: Boolean
        get() =
            activity
                ?.getSystemService(ActivityManager::class.java)
                ?.lockTaskModeState
                ?.let { it != ActivityManager.LOCK_TASK_MODE_NONE } ?: false

    /**
     * Asks Android to pin the screen and waits for the student's answer: true once pinned, false when
     * the confirmation was declined (the window got its focus back while still not pinned).
     */
    suspend fun request(): Boolean {
        val target = activity ?: return false
        var answer: Boolean? = if (isPinned) true else null
        if (answer == null) target.startLockTask()
        var lostFocus = false
        var poll = 0
        while (answer == null && poll < MAX_POLLS) {
            delay(POLL_MS)
            if (!target.hasWindowFocus()) lostFocus = true
            answer =
                when {
                    isPinned -> true

                    // Android's confirmation closed without pinning: declined.
                    lostFocus && target.hasWindowFocus() -> false

                    // No confirmation appeared at all (pinning unavailable on this phone): not pinned.
                    !lostFocus && poll >= NO_PROMPT_POLLS -> false

                    else -> null
                }
            poll++
        }
        return answer ?: isPinned
    }

    /** After the Ujian is submitted or left: the phone works normally again. */
    fun release() {
        if (isPinned) runCatching { activity?.stopLockTask() }
    }

    private companion object {
        const val POLL_MS = 250L

        /** About a minute for the student to read Android's confirmation. */
        const val MAX_POLLS = 240

        /** Android's confirmation shows within a moment; without it after 3 seconds, there is none. */
        const val NO_PROMPT_POLLS = 12
    }
}

@Composable
fun rememberScreenPinning(): ScreenPinning {
    val context = LocalContext.current
    return remember(context) { ScreenPinning(context.findActivity()) }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
