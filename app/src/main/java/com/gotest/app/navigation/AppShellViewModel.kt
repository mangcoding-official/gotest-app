package com.gotest.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.SignUpDraftStore
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
        private val drafts: SignUpDraftStore,
    ) : ViewModel() {
        val isSignedIn: StateFlow<Boolean> = auth.isSignedIn

        /** Read once: a signed-in user opens on Beranda, anyone else on Onboarding. */
        val startSignedIn: Boolean = auth.isSignedIn.value

        val expired: StateFlow<Boolean> = signals.expired
        val updateRequired: StateFlow<Boolean> = signals.updateRequired
        val awaitingConsent: StateFlow<Boolean> = signals.awaitingConsent
        val awaitingEmailVerification: StateFlow<Boolean> = signals.awaitingEmailVerification

        init {
            // Waiting for a parent belongs to one account: it ends with that account's session.
            viewModelScope.launch {
                auth.isSignedIn.collect { signedIn ->
                    if (!signedIn) {
                        signals.clearAwaitingConsent()
                        signals.clearAwaitingEmailVerification()
                    }
                }
            }
        }

        fun consumeExpired() = signals.consumeExpired()

        /**
         * A class code or invitation opened before signing in goes along with Daftar as `mitra_link` (T-026
         * required 4, Aturan 2.18), as the web keeps it in the session; after signing in the link still opens.
         */
        fun keepMitraLink(link: AppLink) {
            when (link) {
                is AppLink.JoinCode -> drafts.mitraLink = "/gabung/${link.code}"
                is AppLink.Invitation -> drafts.mitraLink = "/undangan/${link.token}"
                else -> Unit
            }
        }
    }
