package com.gotest.app.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the server said about the session as a whole (401, 403 `guardian_consent`, 426), kept as state
 * rather than one-off events: an answer that arrives while the app is in the background, or before the
 * shell listens, is still there when the app comes back to the front (T-018 review 4).
 */
@Singleton
class SessionSignals
    @Inject
    constructor() {
        private val expiredState = MutableStateFlow(false)
        private val updateRequiredState = MutableStateFlow(false)
        private val awaitingConsentState = MutableStateFlow(false)

        /** 401 on a signed-in request: the session is gone and Masuk opens, until [consumeExpired]. */
        val expired: StateFlow<Boolean> = expiredState.asStateFlow()

        /** 426 `update_required`: below the server's minimum version; nothing else opens for the rest of the run. */
        val updateRequired: StateFlow<Boolean> = updateRequiredState.asStateFlow()

        /** 403 `guardian_consent`: the account sees only the waiting page until its session ends (API-K6 B). */
        val awaitingConsent: StateFlow<Boolean> = awaitingConsentState.asStateFlow()

        fun markExpired() {
            expiredState.value = true
        }

        /** Masuk has opened for the expired session. */
        fun consumeExpired() {
            expiredState.value = false
        }

        fun markUpdateRequired() {
            updateRequiredState.value = true
        }

        fun markAwaitingConsent() {
            awaitingConsentState.value = true
        }

        /** The session ended (signed out, deleted, or expired), so the next account starts without it. */
        fun clearAwaitingConsent() {
            awaitingConsentState.value = false
        }
    }
