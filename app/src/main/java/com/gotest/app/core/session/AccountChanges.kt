package com.gotest.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts the account writes that went through (profile, photo, number, email, password), so the screens that
 * show `GET /profil` and `GET /akun` read them again, as Inertia reloads its props after a write (T-026
 * required 15, last sentence of API module F).
 */
@Singleton
class AccountChanges
    @Inject
    constructor() {
        private val version = MutableStateFlow(0)

        /** Changes after every saved write; readers reload when it does. */
        val changes: StateFlow<Int> = version.asStateFlow()

        fun markChanged() = version.update { it + 1 }
    }
