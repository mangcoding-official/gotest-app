package com.gotest.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the session is written; a plain key-value store so the session logic runs in unit tests. */
interface KeyValueStore {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)
}

/** Encrypts the token before it is written: the Android Keystore in the app, a fake in tests. */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** Null when the stored value can no longer be read, for example after the key was lost. */
    fun decrypt(encrypted: String): String?
}

/**
 * The signed-in session (T-018): the API token, kept encrypted so it survives closing the app.
 * The start screen follows [isSignedIn]; a 401 from the server clears it.
 */
class SessionStore(
    private val store: KeyValueStore,
    private val cipher: TokenCipher,
) {
    private val signedIn = MutableStateFlow(false)
    private var cached: String? = null

    init {
        cached = load()
        signedIn.value = cached != null
    }

    val isSignedIn: StateFlow<Boolean> = signedIn.asStateFlow()

    /** The token sent as `Authorization: Bearer …`, or null when signed out. */
    val token: String? get() = cached

    fun save(token: String) {
        // A Keystore that cannot seal the token keeps it for this run only, instead of stopping the app (T-026
        // required 5); the next start asks to sign in again.
        runCatching { cipher.encrypt(token) }
            .onSuccess { store.putString(KEY_TOKEN, it) }
            .onFailure { store.remove(KEY_TOKEN) }
        cached = token
        signedIn.value = true
    }

    fun clear() {
        store.remove(KEY_TOKEN)
        cached = null
        signedIn.value = false
    }

    private fun load(): String? {
        val encrypted = store.getString(KEY_TOKEN) ?: return null
        // A token that cannot be read any more is dropped, so the app asks to sign in again.
        val token = runCatching { cipher.decrypt(encrypted) }.getOrNull()
        return token.also { if (it == null) store.remove(KEY_TOKEN) }
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
