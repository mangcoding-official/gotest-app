package com.gotest.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The shared error body of the API: `{"message","code"}` plus `errors` for form fields (API preflight 4.2). */
@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val code: String? = null,
    val errors: Map<String, List<String>> = emptyMap(),
)

/** A refused request; [message] is the server's own text, the same as on the web. */
class ApiError(
    val status: Int,
    override val message: String,
    val code: String?,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
) : Exception(message) {
    /** The first message for [field], shown under that form field. */
    fun fieldError(field: String): String? = fieldErrors[field]?.firstOrNull()

    companion object {
        const val UNAUTHORIZED = 401
        const val UPDATE_REQUIRED = 426

        /** Reads an error response; a body that is not the shared shape keeps the status with [fallbackMessage]. */
        fun from(
            status: Int,
            body: String?,
            json: Json,
            fallbackMessage: String,
        ): ApiError {
            val parsed =
                body
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        try {
                            json.decodeFromString(ApiErrorBody.serializer(), it)
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
            )
        }
    }
}
