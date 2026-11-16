package com.gotest.app.navigation

import androidx.lifecycle.ViewModel
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.SessionEvent
import com.gotest.app.core.network.SessionEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** What the app shell needs from the session: where to start, and the server's session-wide answers (T-018). */
@HiltViewModel
class AppShellViewModel
    @Inject
    constructor(
        auth: AuthRepository,
        events: SessionEvents,
    ) : ViewModel() {
        val isSignedIn: StateFlow<Boolean> = auth.isSignedIn

        /** Read once: a signed-in user opens on Beranda, anyone else on Onboarding. */
        val startSignedIn: Boolean = auth.isSignedIn.value

        val sessionEvents: Flow<SessionEvent> = events.flow
    }
