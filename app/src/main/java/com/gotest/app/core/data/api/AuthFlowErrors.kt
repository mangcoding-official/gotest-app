package com.gotest.app.core.data.api

import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.auth.domain.AuthFlowError
import java.io.IOException

/**
 * The sign-in screens built on the fakes read a refusal as one field and its text ([AuthFlowError]). A refusal
 * of the API keeps the server's own text (the web's) under its first field, or under `form` when it names none
 * (a blocked account, a team account, too many tries); no connection says so in the app's words.
 */
internal fun <T> Result<T>.asAuthFlow(): Result<T> =
    fold({
        Result.success(it)
    }, { Result.failure(it.toAuthFlowError()) })

internal fun Throwable.toAuthFlowError(): AuthFlowError =
    when (this) {
        is AuthFlowError -> {
            this
        }

        is ApiError -> {
            fieldErrors.entries
                .firstNotNullOfOrNull { (field, texts) -> texts.firstOrNull()?.let { AuthFlowError(field, it) } }
                ?: AuthFlowError(FORM, message)
        }

        is IOException -> {
            AuthFlowError(FORM, OFFLINE)
        }

        else -> {
            AuthFlowError(FORM, SERVER_ERROR)
        }
    }

private const val FORM = "form"

/** The same sentence as `auth_error_offline`. */
private const val OFFLINE = "Koneksi terputus. Periksa internetmu lalu coba lagi."

/** The API's own sentence for a 500 (`server_error`). */
private const val SERVER_ERROR = "Terjadi kesalahan di server. Coba lagi sebentar lagi."
