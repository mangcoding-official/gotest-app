package com.gotest.app.core.data.api

import android.os.Build
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.SignUpDraftStore
import com.gotest.app.core.session.SignUpJoin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Takes the reply of any way of signing in or signing up: keeps the token and opens the right screen at once
 * from the reply itself (T-026 required 3), not after the first 403: an account whose email is not proven waits
 * for it (DEC-154), and one whose parent has not approved waits for the parent (API-K6 B).
 */
@Singleton
class SignInAcceptor
    @Inject
    constructor(
        private val session: SessionStore,
        private val signals: SessionSignals,
        private val changes: AccountChanges,
        private val drafts: SignUpDraftStore,
        private val join: SignUpJoin = SignUpJoin(),
    ) {
        /** The phone's name, sent as `device_name` and shown in Sesi Aktif (at most 100 letters). */
        val deviceName: String
            get() =
                listOf(Build.MANUFACTURER, Build.MODEL)
                    .filterNot { it.isNullOrBlank() }
                    .joinToString(" ")
                    .ifBlank { FALLBACK_DEVICE }
                    .take(MAX_DEVICE_NAME)

        /**
         * A new account: as [accept], and the Mitra its `mitra_link` joined opens next while the link held
         * since before signing in is dropped (T-026 A2a-1). Read before [accept] forgets the link.
         */
        fun acceptSignUp(reply: SignInReply): Boolean {
            if (reply.token == null) return false
            join.signedUp(reply.joined, drafts.mitraLink)
            return accept(reply)
        }

        /** False when the reply carries no token (a sign-up waiting for its email code). */
        fun accept(reply: SignInReply): Boolean {
            val token = reply.token ?: return false
            session.save(token)
            // Signed in: an unfinished sign-up and its Mitra link belong to no one now.
            drafts.clearEmailCode()
            drafts.clearPhoneToken()
            drafts.clearGoogleToken()
            drafts.mitraLink = null
            when {
                reply.code == SignInReply.EMAIL_VERIFICATION_PENDING -> signals.markAwaitingEmailVerification()
                reply.consentLocked -> signals.markAwaitingConsent()
            }
            changes.markChanged()
            return true
        }

        private companion object {
            const val MAX_DEVICE_NAME = 100
            const val FALLBACK_DEVICE = "Aplikasi Gotest"
        }
    }
