package com.gotest.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

/** The shared error body of the API: `{"message","code"}` plus `errors` for form fields (API preflight 4.2). */
@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val code: String? = null,
    val errors: Map<String, List<String>> = emptyMap(),
)

/**
 * A refused request; [message] is the server's own text, the same as on the web. The whole body stays in
 * [details], so a screen can read what some refusals carry (T-026 required 1): `registration_token`, `phone`,
 * `name`, `email` of 422 `phone_not_registered` and `google_not_registered`, `min_version` of 426, `kind` of the
 * Main Bareng rules. [retryAfterSeconds] is the `Retry-After` header of a 429.
 */
class ApiError(
    val status: Int,
    override val message: String,
    val code: String?,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val details: JsonObject = JsonObject(emptyMap()),
    val retryAfterSeconds: Long? = null,
) : Exception(message) {
    /** The first message for [field], shown under that form field. */
    fun fieldError(field: String): String? = fieldErrors[field]?.firstOrNull()

    /** A text or number of the body, such as `registration_token` or `phone`; null when absent or not a value. */
    fun detail(key: String): String? = (details[key] as? JsonPrimitive)?.contentOrNull

    /** The `kind` the web reads (Main Bareng rules), next to `code`. */
    val kind: String? get() = detail("kind")

    /** 426: the lowest app version the server still serves. */
    val minVersion: Int? get() = (details["min_version"] as? JsonPrimitive)?.intOrNull

    companion object {
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val UPDATE_REQUIRED = 426

        /** 403: the account waits for, or lost, the parent's approval (DEC-141). */
        const val GUARDIAN_CONSENT = "guardian_consent"

        /** 403: the account was made while its sign-up code could not be sent; its email is not proven (DEC-154). */
        const val EMAIL_VERIFICATION_PENDING = "email_verification_pending"

        /** Reads an error response; a body that is not the shared shape keeps the status with [fallbackMessage]. */
        fun from(
            status: Int,
            body: String?,
            json: Json,
            fallbackMessage: String,
            retryAfter: String? = null,
        ): ApiError {
            val details =
                body
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        try {
                            json.parseToJsonElement(it) as? JsonObject
                        } catch (_: SerializationException) {
                            null
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    }
            val parsed =
                details?.let {
                    try {
                        json.decodeFromJsonElement<ApiErrorBody>(it)
                    } catch (_: SerializationException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            return ApiError(
                status = status,
                message = parsed?.message?.takeIf { it.isNotBlank() } ?: fallbackMessage,
                code = parsed?.code,
                fieldErrors = parsed?.errors.orEmpty(),
                details = details ?: JsonObject(emptyMap()),
                // Laravel sends seconds; an HTTP date is not used by the API.
                retryAfterSeconds = retryAfter?.trim()?.toLongOrNull(),
            )
        }
    }
}
