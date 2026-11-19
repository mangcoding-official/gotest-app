package com.gotest.app.feature.ruang.data

import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

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
 *   sign-in that ends mid-Ujian does not force a take-over; when the Keystore fails, the token stays
 *   in memory for this run instead of crashing (T-026 butir wajib 5);
 * - the answers not yet sent, so answers given without a signal survive closing the app;
 * - when the student left the Ujian, so leaving counts even if the app is closed meanwhile, and the
 *   violation reports not sent yet (T-026 A2b review 2, A2b-4).
 * Plain Kotlin over [KeyValueStore], so it runs in unit tests.
 */
class ExamLocalStore(
    private val store: KeyValueStore,
    private val cipher: TokenCipher,
    private val json: Json,
) {
    private val queueSerializer = MapSerializer(Long.serializer(), PendingAnswer.serializer())
    private val violationsSerializer = ListSerializer(ExamViolationRequest.serializer())
    private val memoryTokens = mutableMapOf<Long, String>()

    fun deviceToken(attemptId: Long): String? {
        val encrypted = store.getString(tokenKey(attemptId))
        val stored = encrypted?.let { value -> keystore { cipher.decrypt(value) } }
        if (encrypted != null && stored == null) store.remove(tokenKey(attemptId))
        return memoryTokens[attemptId] ?: stored
    }

    fun saveDeviceToken(
        attemptId: Long,
        token: String,
    ) {
        val encrypted = keystore { cipher.encrypt(token) }
        if (encrypted == null) {
            memoryTokens[attemptId] = token
        } else {
            memoryTokens.remove(attemptId)
            store.putString(tokenKey(attemptId), encrypted)
        }
    }

    fun pending(attemptId: Long): Map<Long, PendingAnswer> = read(queueKey(attemptId), queueSerializer).orEmpty()

    fun savePending(
        attemptId: Long,
        pending: Map<Long, PendingAnswer>,
    ) = write(queueKey(attemptId), pending.takeIf { it.isNotEmpty() }, queueSerializer)

    /** The server time the student left the Ujian (ISO 8601), until they are back. */
    fun awaySince(attemptId: Long): String? = store.getString(awayKey(attemptId))

    fun saveAwaySince(
        attemptId: Long,
        since: String?,
    ) {
        if (since == null) store.remove(awayKey(attemptId)) else store.putString(awayKey(attemptId), since)
    }

    /** Violation reports not taken by the server yet, oldest first. */
    fun pendingViolations(attemptId: Long): List<ExamViolationRequest> =
        read(violationsKey(attemptId), violationsSerializer).orEmpty()

    fun savePendingViolations(
        attemptId: Long,
        violations: List<ExamViolationRequest>,
    ) = write(violationsKey(attemptId), violations.takeIf { it.isNotEmpty() }, violationsSerializer)

    /** After the attempt is submitted: nothing of it stays on the phone. */
    fun forget(attemptId: Long) {
        memoryTokens.remove(attemptId)
        listOf(
            tokenKey(attemptId),
            queueKey(attemptId),
            awayKey(attemptId),
            violationsKey(attemptId),
        ).forEach(store::remove)
    }

    private fun <T> read(
        key: String,
        serializer: KSerializer<T>,
    ): T? {
        val raw = store.getString(key) ?: return null
        return try {
            json.decodeFromString(serializer, raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun <T : Any> write(
        key: String,
        value: T?,
        serializer: KSerializer<T>,
    ) {
        if (value == null) store.remove(key) else store.putString(key, json.encodeToString(serializer, value))
    }

    /** The Android Keystore can fail on some phones (ProviderException, IOException); null then. */
    private fun keystore(block: () -> String?): String? =
        try {
            block()
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: ProviderException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: IllegalStateException) {
            null
        }

    private fun tokenKey(attemptId: Long) = "device.$attemptId"

    private fun queueKey(attemptId: Long) = "pending.$attemptId"

    private fun awayKey(attemptId: Long) = "away.$attemptId"

    private fun violationsKey(attemptId: Long) = "violations.$attemptId"
}
