package com.gotest.app.feature.multiplayer.domain

import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.domain.ResponseKind
import com.gotest.app.feature.practice.domain.isAnswered
import com.gotest.app.feature.practice.domain.responseKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** The saved answer of a question (an index, a list, or a label per statement), as the student picks it. */
fun GameQuestion.savedValue(): AnswerValue? {
    val saved = answer ?: return null
    return when (responseKind(questionType)) {
        ResponseKind.Multiple -> {
            (saved as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }
                ?.takeIf { it.isNotEmpty() }
                ?.let(AnswerValue::Multiple)
        }

        ResponseKind.Statement -> {
            (saved as? JsonArray)?.map { (it as? JsonPrimitive)?.intOrNull }?.let(AnswerValue::Statement)
        }

        ResponseKind.Single -> {
            (saved as? JsonPrimitive)?.intOrNull?.let(AnswerValue::Single)
        }
    }
}

/** Web `isItemAnswered` for a game question. */
fun GameQuestion.isAnswered(value: AnswerValue?): Boolean =
    isAnswered(PracticeItem(id = id, questionId = id, displayOrder = order, questionType = questionType), value)

/**
 * A refused game request as the web's `toGameError` reads it: a rule of the game (`kind`, with the server's
 * sentence), a game that is not there (404), a form field, or anything else as a dropped connection.
 */
sealed interface GameProblem {
    val kind: String

    data class Server(
        override val kind: String,
        val message: String,
    ) : GameProblem

    /** 404: "Permainan tidak ditemukan." */
    data object NotFound : GameProblem {
        override val kind = NOT_FOUND
    }

    /** "Isi 6 huruf kode dari temanmu." before looking a code up. */
    data object ShortCode : GameProblem {
        override val kind = NOT_FOUND
    }

    data class Invalid(
        val message: String,
    ) : GameProblem {
        override val kind = "invalid"
    }

    /** "Koneksi terputus. Periksa internetmu lalu coba lagi." */
    data object Connection : GameProblem {
        override val kind = "connection"
    }

    companion object {
        const val NOT_FOUND = "not_found"
        const val BUSY = "busy"
        const val STOCK = "stock"
        private const val MISSING = 404
        private val KINDS =
            setOf(
                NOT_FOUND,
                "started",
                "full",
                "closed",
                BUSY,
                "not_premium",
                "not_member",
                STOCK,
                "crowded",
                "invalid",
            )

        fun of(error: Throwable?): GameProblem {
            if (error !is ApiError) return Connection
            val field =
                error.fieldErrors.values
                    .firstOrNull()
                    ?.firstOrNull()
            return when {
                error.code in KINDS && error.status != MISSING -> Server(error.code.orEmpty(), error.message)
                error.status == MISSING -> NotFound
                field != null -> Invalid(field)
                else -> Connection
            }
        }
    }
}
