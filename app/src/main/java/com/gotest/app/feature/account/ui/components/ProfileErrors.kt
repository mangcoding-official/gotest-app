package com.gotest.app.feature.account.ui.components

import com.gotest.app.core.network.ApiError

/**
 * The server's message for [field], or its first message; null when nothing came back (no connection),
 * so the screen shows its own "try again" text.
 */
internal fun Throwable.fieldMessage(field: String): String? =
    when (this) {
        is ApiError -> fieldError(field) ?: message
        is PhotoRejected -> message
        else -> null
    }

/** A photo the app refuses before sending it, with the web's own text (UpdateAvatarModal.tsx). */
class PhotoRejected(
    override val message: String,
) : Exception(message)
