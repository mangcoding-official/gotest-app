package com.gotest.app.core.session

/**
 * What a sign-up needs to survive Android closing the app (T-026 required 4 and 10), kept in the app's private
 * preferences: the Mitra link opened before signing in (sent as `mitra_link`, Aturan 2.18), the
 * `registration_token` of a sign-up waiting for its email code, and that of a proven new phone number waiting for
 * its name ("Satu langkah lagi"); the server keeps both for 15 minutes.
 */
class SignUpDraftStore(
    private val store: KeyValueStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** A `/gabung/{kode}` or `/undangan/{token}` path, as the web accepts it. */
    var mitraLink: String?
        get() = store.getString(KEY_MITRA_LINK)
        set(value) = if (value == null) store.remove(KEY_MITRA_LINK) else store.putString(KEY_MITRA_LINK, value)

    /** The email sign-up waiting for its code, or null once it is done, dropped, or past 15 minutes. */
    val emailCode: PendingEmailCode?
        get() {
            val token = fresh(KEY_TOKEN, KEY_SAVED_AT) ?: return null.also { clearEmailCode() }
            return PendingEmailCode(token = token, sentTo = store.getString(KEY_SENT_TO).orEmpty())
        }

    /** The `registration_token` of a proven new number (422 `phone_not_registered`), within 15 minutes. */
    val phoneToken: String?
        get() = fresh(KEY_PHONE_TOKEN, KEY_PHONE_SAVED_AT) ?: null.also { clearPhoneToken() }

    fun savePhoneToken(token: String) {
        store.putString(KEY_PHONE_TOKEN, token)
        store.putString(KEY_PHONE_SAVED_AT, clock().toString())
    }

    fun clearPhoneToken() {
        store.remove(KEY_PHONE_TOKEN)
        store.remove(KEY_PHONE_SAVED_AT)
    }

    private fun fresh(
        tokenKey: String,
        savedAtKey: String,
    ): String? {
        val token = store.getString(tokenKey) ?: return null
        val savedAt = store.getString(savedAtKey)?.toLongOrNull() ?: 0L
        return token.takeIf { clock() - savedAt < TOKEN_LIFE_MILLIS }
    }

    fun saveEmailCode(
        token: String,
        sentTo: String,
    ) {
        store.putString(KEY_TOKEN, token)
        store.putString(KEY_SENT_TO, sentTo)
        store.putString(KEY_SAVED_AT, clock().toString())
    }

    fun clearEmailCode() {
        store.remove(KEY_TOKEN)
        store.remove(KEY_SENT_TO)
        store.remove(KEY_SAVED_AT)
    }

    private companion object {
        const val KEY_MITRA_LINK = "mitra_link"
        const val KEY_TOKEN = "email_registration_token"
        const val KEY_SENT_TO = "email_registration_sent_to"
        const val KEY_SAVED_AT = "email_registration_saved_at"
        const val KEY_PHONE_TOKEN = "phone_registration_token"
        const val KEY_PHONE_SAVED_AT = "phone_registration_saved_at"
        const val TOKEN_LIFE_MILLIS = 15 * 60 * 1000L
    }
}

data class PendingEmailCode(
    val token: String,
    val sentTo: String,
)
