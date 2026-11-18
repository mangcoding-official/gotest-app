package com.gotest.app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs one API call as a [Result]: a refused request becomes an [ApiError] with the server's `code`
 * and message, a dropped connection stays an [IOException] (screens show their offline text for it).
 */
suspend fun <T> apiCall(
    json: Json,
    block: suspend () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (error: HttpException) {
        val body = error.response()?.errorBody()?.string()
        Result.failure(ApiError.from(error.code(), body, json, SERVER_ERROR_MESSAGE))
    } catch (error: IOException) {
        Result.failure(error)
    } catch (error: SerializationException) {
        Result.failure(ApiError(status = 0, message = SERVER_ERROR_MESSAGE, code = null).apply { initCause(error) })
    }

/** The API's own 500 text (preflight API 4.2), for an answer that is not the shared error shape. */
const val SERVER_ERROR_MESSAGE = "Terjadi kesalahan di server. Coba lagi sebentar lagi."
