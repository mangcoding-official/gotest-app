package com.gotest.app.core.session

import com.gotest.app.feature.ruang.domain.JoinedMitra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a sign-up from a Mitra link did, for the app shell (T-026 A2a-1 = a, as the web's `completeMitraJoin`):
 * the Mitra it joined opens once (a student's Ruang, or the A2b-2 dialog for staff), and the link held since
 * before signing in is not opened again, because the sign-up already used it as `mitra_link`.
 */
@Singleton
class SignUpJoin
    @Inject
    constructor() {
        private val _joined = MutableStateFlow<JoinedMitra?>(null)

        /** The Mitra the sign-up joined, until the app shell opened it. */
        val joined: StateFlow<JoinedMitra?> = _joined.asStateFlow()

        private val usedLink = AtomicReference<String?>(null)

        /** The Mitra's name from the Daftar page's notice, for the staff dialog; the reply only has its code. */
        @Volatile
        var mitraName: String? = null

        /** A sign-up finished; [link] is the `mitra_link` it sent, if any. */
        fun signedUp(
            joined: JoinedMitra?,
            link: String?,
        ) {
            if (link != null) usedLink.set(link)
            if (joined != null) _joined.value = joined
        }

        /** True once for the link the sign-up used: the app shell then drops it instead of opening it. */
        fun takeUsedLink(link: String): Boolean = usedLink.compareAndSet(link, null)

        fun consumeJoined() {
            _joined.value = null
        }

        /** Keluar: nothing of the sign-up is left for the next account. */
        fun clear() {
            _joined.value = null
            usedLink.set(null)
            mitraName = null
        }
    }
