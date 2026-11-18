package com.gotest.app.feature.ruang.data

import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.TokenCipher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** An answer not yet confirmed by the server; only the latest per item is kept (web: localStorage). */
@Serializable
data class PendingAnswer(
    /** JsonNull clears the answer. */
    @SerialName("answer") val answer: JsonElement,
    @SerialName("seq") val seq: Long,
    @SerialName("answeredAt") val answeredAt: String,
)

/**
 * What one phone keeps per Ujian attempt (API module G):
 * - the Ujian device token, encrypted with the Keystore key and apart from the sign-in token, so a
 *   sign-in that ends mid-Ujian does not force a take-over;
 * - the answers not yet sent, so answers given without a signal survive closing the app and are
 *   sent when the connection is back.
 * Plain Kotlin over [KeyValueStore], so it runs in unit tests.
 */
class ExamLocalStore(
    private val store: KeyValueStore,
    private val cipher: TokenCipher,
    private val json: Json,
) {
    private val queueSerializer = MapSerializer(Long.serializer(), PendingAnswer.serializer())

    fun deviceToken(attemptId: Long): String? {
        val encrypted = store.getString(tokenKey(attemptId)) ?: return null
        return cipher.decrypt(encrypted).also { if (it == null) store.remove(tokenKey(attemptId)) }
    }

    fun saveDeviceToken(
        attemptId: Long,
        token: String,
    ) = store.putString(tokenKey(attemptId), cipher.encrypt(token))

    fun pending(attemptId: Long): Map<Long, PendingAnswer> {
        val raw = store.getString(queueKey(attemptId)) ?: return emptyMap()
        return try {
            json.decodeFromString(queueSerializer, raw)
        } catch (_: SerializationException) {
            emptyMap()
        } catch (_: IllegalArgumentException) {
            emptyMap()
        }
    }

    fun savePending(
        attemptId: Long,
        pending: Map<Long, PendingAnswer>,
    ) {
        if (pending.isEmpty()) {
            store.remove(queueKey(attemptId))
        } else {
            store.putString(queueKey(attemptId), json.encodeToString(queueSerializer, pending))
        }
    }

    /** After the attempt is submitted: nothing of it stays on the phone. */
    fun forget(attemptId: Long) {
        store.remove(tokenKey(attemptId))
        store.remove(queueKey(attemptId))
    }

    private fun tokenKey(attemptId: Long) = "device.$attemptId"

    private fun queueKey(attemptId: Long) = "pending.$attemptId"
}
