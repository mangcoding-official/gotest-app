package com.gotest.app.core.session

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A sentence the next screen shows once, as the web's flash message after a redirect: why the Mitra link of a
 * sign-up could not be used, after the email was verified (DEC-154; A2d review of T-026 required items, 1).
 */
@Singleton
class SessionNotices
    @Inject
    constructor() {
        private val emailVerifiedReason = AtomicReference<String?>(null)

        fun postEmailVerified(reason: String) = emailVerifiedReason.set(reason)

        /** The reason, once; the screen that shows it takes it. */
        fun takeEmailVerified(): String? = emailVerifiedReason.getAndSet(null)
    }
