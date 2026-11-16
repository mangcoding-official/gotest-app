package com.gotest.app.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the server said about the session as a whole; the app shell reacts to it. */
sealed interface SessionEvent {
    /** 401: the token is no longer valid; the app goes back to Masuk. */
    data object SignedOut : SessionEvent

    /** 426 `update_required`: this app version is below the server's minimum. */
    data object UpdateRequired : SessionEvent
}

@Singleton
class SessionEvents
    @Inject
    constructor() {
        private val events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)

        val flow: SharedFlow<SessionEvent> = events.asSharedFlow()

        fun emit(event: SessionEvent) {
            events.tryEmit(event)
        }
    }
