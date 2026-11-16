package com.gotest.app.feature.auth.domain

/**
 * The PIN lock of a child account (Aturan 11.29): the fifth wrong PIN locks it for 15 minutes; while
 * locked even the right PIN is refused, and the counter starts again after the lock.
 */
class PinLockout(
    private val maxWrong: Int = MAX_WRONG,
    private val lockMillis: Long = LOCK_MILLIS,
) {
    data class State(
        val wrong: Int = 0,
        val lockedUntilMillis: Long? = null,
    )

    /** Whole minutes left on the lock, rounded up and at least 1; null when not locked. */
    fun minutesLeft(
        state: State,
        nowMillis: Long,
    ): Int? {
        val until = state.lockedUntilMillis ?: return null
        val left = until - nowMillis
        return if (left <= 0) null else ((left + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
    }

    fun afterWrong(
        state: State,
        nowMillis: Long,
    ): State {
        val wrong =
            (if (minutesLeft(state, nowMillis) == null && state.lockedUntilMillis != null) 0 else state.wrong) + 1
        return if (wrong >=
            maxWrong
        ) {
            State(wrong = 0, lockedUntilMillis = nowMillis + lockMillis)
        } else {
            State(wrong = wrong)
        }
    }

    companion object {
        const val MAX_WRONG = 5
        const val LOCK_MILLIS = 15 * 60_000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
