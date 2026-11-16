package com.gotest.app.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PinLockoutTest {
    private val lockout = PinLockout()
    private val minute = 60_000L

    @Test
    fun `the fifth wrong PIN locks for fifteen minutes`() {
        var state = PinLockout.State()
        repeat(4) { state = lockout.afterWrong(state, nowMillis = 0) }
        assertNull(lockout.minutesLeft(state, nowMillis = 0))

        state = lockout.afterWrong(state, nowMillis = 0)
        assertEquals(15, lockout.minutesLeft(state, nowMillis = 0))
    }

    @Test
    fun `minutes left round up and the lock ends on time`() {
        val locked = PinLockout.State(lockedUntilMillis = 15 * minute)
        assertEquals(1, lockout.minutesLeft(locked, nowMillis = 15 * minute - 1))
        assertEquals(10, lockout.minutesLeft(locked, nowMillis = 5 * minute))
        assertEquals(11, lockout.minutesLeft(locked, nowMillis = 5 * minute - 1))
        assertNull(lockout.minutesLeft(locked, nowMillis = 15 * minute))
    }

    @Test
    fun `after a lock the count starts again`() {
        val afterLock = lockout.afterWrong(PinLockout.State(lockedUntilMillis = minute), nowMillis = 2 * minute)
        assertEquals(PinLockout.State(wrong = 1), afterLock)
    }
}
