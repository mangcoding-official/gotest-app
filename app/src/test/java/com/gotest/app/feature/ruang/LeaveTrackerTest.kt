package com.gotest.app.feature.ruang

import com.gotest.app.feature.ruang.ui.components.PinState
import com.gotest.app.feature.ruang.ui.work.LeaveListener
import com.gotest.app.feature.ruang.ui.work.LeaveTracker
import org.junit.Assert.assertEquals
import org.junit.Test

/** When leaving the Ujian starts and ends (Aturan 12.13, API-K8 A, T-026 A2b review 1). */
class LeaveTrackerTest {
    private val pin =
        object : PinState {
            override var isPinned: Boolean = false
        }
    private val events = mutableListOf<String>()
    private val tracker =
        LeaveTracker(pin).apply {
            active = true
            listener =
                LeaveListener(
                    onLeave = { events += "leave" },
                    onReturn = { events += "return" },
                    onPinnedChange = { events += "pinned=$it" },
                )
        }

    @Test
    fun beforeThePaperWasShownLeavingDoesNotCount() {
        tracker.poll()
        tracker.onStop()
        assertEquals(listOf("pinned=false"), events)
    }

    @Test
    fun leavingTheAppAfterPinningCountsUntilBack() {
        pin.isPinned = true
        tracker.poll()
        tracker.onStop()
        tracker.onStart()
        assertEquals(listOf("pinned=true", "return", "leave", "return"), events)
    }

    @Test
    fun leavingThePinnedScreenCountsUntilPinnedAgain() {
        pin.isPinned = true
        tracker.poll()
        pin.isPinned = false
        tracker.poll()
        pin.isPinned = true
        tracker.poll()
        assertEquals(listOf("pinned=true", "return", "pinned=false", "leave", "pinned=true", "return"), events)
    }

    @Test
    fun aQuizOrALockedUjianCountsNothing() {
        tracker.active = false
        pin.isPinned = true
        tracker.poll()
        pin.isPinned = false
        tracker.poll()
        tracker.onStop()
        assertEquals(listOf("pinned=true", "return", "pinned=false"), events)
    }
}
