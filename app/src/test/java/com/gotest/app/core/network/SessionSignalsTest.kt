package com.gotest.app.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSignalsTest {
    private val signals = SessionSignals()

    @Test
    fun `an expired session waits until Masuk has opened`() {
        signals.markExpired()
        // Nobody listened yet (the app was in the background): the answer is still there.
        assertTrue(signals.expired.value)
        signals.consumeExpired()
        assertFalse(signals.expired.value)
    }

    @Test
    fun `waiting for a parent ends with the session, the update requirement stays`() {
        signals.markAwaitingConsent()
        signals.markUpdateRequired()
        signals.clearAwaitingConsent()

        assertFalse(signals.awaitingConsent.value)
        assertTrue(signals.updateRequired.value)
    }
}
