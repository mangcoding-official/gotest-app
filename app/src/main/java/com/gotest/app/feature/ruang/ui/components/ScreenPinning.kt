package com.gotest.app.feature.ruang.ui.components

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/** Whether the screen is pinned; replaceable in tests. */
interface PinState {
    val isPinned: Boolean
}

/**
 * Screen pinning for Ujian (API-K8 A): the app asks to pin the screen before an Ujian starts, and
 * leaving the pinned screen counts as leaving the page (Aturan 12.13). Android shows its own
 * "pin this app?" confirmation; the app only starts the Ujian once the screen is really pinned.
 */
class ScreenPinning(
    private val activity: Activity?,
) : PinState {
    override val isPinned: Boolean
        get() =
            activity
                ?.getSystemService(ActivityManager::class.java)
                ?.lockTaskModeState
                ?.let { it != ActivityManager.LOCK_TASK_MODE_NONE } ?: false

    /** Shows Android's "pin this app?" confirmation again; asking twice is harmless. */
    fun ask() {
        if (!isPinned) activity?.startLockTask()
    }

    /**
     * Waits up to a minute for the screen to be pinned: true once pinned. Android's confirmation does
     * not take the window focus and says nothing when declined, and on a phone where pinning is off in
     * Settings no confirmation appears at all; so the screen keeps its guidance visible while this waits.
     */
    suspend fun awaitPinned(): Boolean {
        var poll = 0
        while (activity != null && !isPinned && poll < MAX_POLLS) {
            delay(POLL_MS)
            poll++
        }
        return isPinned
    }

    /** After the Ujian is submitted or left: the phone works normally again. */
    fun release() {
        if (isPinned) runCatching { activity?.stopLockTask() }
    }

    private companion object {
        const val POLL_MS = 250L

        /** About a minute for the student to read Android's confirmation. */
        const val MAX_POLLS = 240
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
