package com.gotest.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.SessionSignals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the app shell needs from the session: where to start, and the server's session-wide answers (T-018). */
@HiltViewModel
class AppShellViewModel
    @Inject
    constructor(
        auth: AuthRepository,
        private val signals: SessionSignals,
    ) : ViewModel() {
        val isSignedIn: StateFlow<Boolean> = auth.isSignedIn

        /** Read once: a signed-in user opens on Beranda, anyone else on Onboarding. */
        val startSignedIn: Boolean = auth.isSignedIn.value

        val expired: StateFlow<Boolean> = signals.expired
        val updateRequired: StateFlow<Boolean> = signals.updateRequired
        val awaitingConsent: StateFlow<Boolean> = signals.awaitingConsent

        init {
            // Waiting for a parent belongs to one account: it ends with that account's session.
            viewModelScope.launch {
                auth.isSignedIn.collect { signedIn -> if (!signedIn) signals.clearAwaitingConsent() }
            }
        }

        fun consumeExpired() = signals.consumeExpired()
    }
