package com.gotest.app.core.data.api

import android.os.Build
import com.gotest.app.core.network.SessionSignals
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.SessionStore
import com.gotest.app.core.session.SignUpDraftStore
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
    ) {
        /** The phone's name, sent as `device_name` and shown in Sesi Aktif (at most 100 letters). */
        val deviceName: String
            get() =
                listOf(Build.MANUFACTURER, Build.MODEL)
                    .filterNot { it.isNullOrBlank() }
                    .joinToString(" ")
                    .ifBlank { FALLBACK_DEVICE }
                    .take(MAX_DEVICE_NAME)

        /** False when the reply carries no token (a sign-up waiting for its email code). */
        fun accept(reply: SignInReply): Boolean {
            val token = reply.token ?: return false
            session.save(token)
            // Signed in: an unfinished sign-up and its Mitra link belong to no one now.
            drafts.clearEmailCode()
            drafts.clearPhoneToken()
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
