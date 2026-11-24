package com.gotest.app.core.session

/**
 * What a sign-up needs to survive Android closing the app (T-026 required 4 and 10), kept in the app's private
 * preferences: the Mitra link opened before signing in (sent as `mitra_link`, Aturan 2.18) and the
 * `registration_token` of a sign-up waiting for its email code, which the server keeps for 15 minutes.
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
            val token = store.getString(KEY_TOKEN) ?: return null
            val savedAt = store.getString(KEY_SAVED_AT)?.toLongOrNull() ?: 0L
            if (clock() - savedAt >= TOKEN_LIFE_MILLIS) {
                clearEmailCode()
                return null
            }
            return PendingEmailCode(token = token, sentTo = store.getString(KEY_SENT_TO).orEmpty())
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
        const val TOKEN_LIFE_MILLIS = 15 * 60 * 1000L
    }
}

data class PendingEmailCode(
    val token: String,
    val sentTo: String,
)
