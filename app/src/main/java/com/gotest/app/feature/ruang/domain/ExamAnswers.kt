package com.gotest.app.feature.ruang.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** The five Ujian item types (DEC-148). */
object ExamItemType {
    const val SINGLE = "single_choice"
    const val MULTIPLE = "multiple_select"
    const val TRUE_FALSE = "true_false"
    const val MATCHING = "matching"
    const val SHORT = "short_answer"
}

data class ExamOption(
    val id: String,
    /** HTML. */
    val text: String,
    val mediaUrl: String?,
)

data class ExamEntry(
    val id: String,
    val text: String,
)

/** An item's answerable part, already in this student's order. */
sealed interface ExamContent {
    data class Choice(
        val options: List<ExamOption>,
        val multiple: Boolean,
    ) : ExamContent

    data class TrueFalse(
        /** Two labels, "Benar" and "Salah" unless the teacher changed them. */
        val labels: List<String>,
        val statements: List<ExamEntry>,
    ) : ExamContent

    data class Matching(
        val left: List<ExamEntry>,
        val right: List<ExamEntry>,
    ) : ExamContent

    data class ShortAnswer(
        val numeric: Boolean,
    ) : ExamContent

    companion object {
        private val DEFAULT_LABELS = listOf("Benar", "Salah")

        fun of(
            type: String,
            content: JsonObject,
        ): ExamContent =
            when (type) {
                ExamItemType.SINGLE, ExamItemType.MULTIPLE -> {
                    Choice(
                        options =
                            content.objects("options").map {
                                ExamOption(it.string("id"), it.string("text"), it.stringOrNull("media_url"))
                            },
                        multiple = type == ExamItemType.MULTIPLE,
                    )
                }

                ExamItemType.TRUE_FALSE -> {
                    TrueFalse(
                        labels =
                            (content["labels"] as? JsonArray)
                                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                                ?.takeIf { it.size == 2 } ?: DEFAULT_LABELS,
                        statements = content.entries("statements"),
                    )
                }

                ExamItemType.MATCHING -> {
                    Matching(content.entries("left"), content.entries("right"))
                }

                else -> {
                    ShortAnswer(numeric = content.stringOrNull("answer_type") == "number")
                }
            }
    }
}

/** An answer as the server keeps it; [toJson] gives the shape `PUT …/jawaban` expects. */
sealed interface ExamAnswer {
    data class Single(
        val selected: String,
    ) : ExamAnswer

    data class Multiple(
        val selected: List<String>,
    ) : ExamAnswer

    /** Statement id to the index (0 or 1) of the chosen label. */
    data class TrueFalse(
        val answers: Map<String, Int>,
    ) : ExamAnswer

    /** Left id to right id; one right entry for one left entry. */
    data class Matching(
        val pairs: Map<String, String>,
    ) : ExamAnswer

    data class Text(
        val text: String,
    ) : ExamAnswer

    fun toJson(): JsonObject =
        when (this) {
            is Single -> JsonObject(mapOf("selected" to JsonPrimitive(selected)))
            is Multiple -> JsonObject(mapOf("selected" to JsonArray(selected.map(::JsonPrimitive))))
            is TrueFalse -> JsonObject(mapOf("answers" to JsonObject(answers.mapValues { JsonPrimitive(it.value) })))
            is Matching -> JsonObject(mapOf("pairs" to JsonObject(pairs.mapValues { JsonPrimitive(it.value) })))
            is Text -> JsonObject(mapOf("text" to JsonPrimitive(text)))
        }

    companion object {
        /**
         * An answer with nothing in it is null, the way the server clears it (`cleanExamAnswer` on the
         * web): no option, no statement, no pair, or text that is only spaces. Text is kept as typed.
         */
        fun clean(answer: ExamAnswer?): ExamAnswer? =
            when (answer) {
                null -> null
                is Single -> answer
                is Multiple -> answer.takeIf { it.selected.isNotEmpty() }
                is TrueFalse -> answer.takeIf { it.answers.isNotEmpty() }
                is Matching -> answer.takeIf { it.pairs.isNotEmpty() }
                is Text -> answer.takeIf { it.text.isNotBlank() }
            }

        /** Reads a saved answer of an item of [type]; an unreadable one counts as not answered. */
        fun fromJson(
            type: String,
            json: JsonElement?,
        ): ExamAnswer? {
            val obj = json as? JsonObject ?: return null
            val answer =
                when (type) {
                    ExamItemType.SINGLE -> {
                        obj.stringOrNull("selected")?.let(::Single)
                    }

                    ExamItemType.MULTIPLE -> {
                        (obj["selected"] as? JsonArray)
                            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                            ?.let(::Multiple)
                    }

                    ExamItemType.TRUE_FALSE -> {
                        (obj["answers"] as? JsonObject)
                            ?.mapNotNull { (id, value) -> value.jsonPrimitive.intOrNull?.let { id to it } }
                            ?.toMap()
                            ?.let(::TrueFalse)
                    }

                    ExamItemType.MATCHING -> {
                        (obj["pairs"] as? JsonObject)
                            ?.mapNotNull { (id, value) -> value.jsonPrimitive.contentOrNull?.let { id to it } }
                            ?.toMap()
                            ?.let(::Matching)
                    }

                    else -> {
                        obj.stringOrNull("text")?.let(::Text)
                    }
                }
            return clean(answer)
        }
    }
}

private fun JsonObject.objects(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

private fun JsonObject.entries(key: String): List<ExamEntry> =
    objects(key).map {
        ExamEntry(it.string("id"), it.string("text"))
    }

private fun JsonObject.stringOrNull(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.string(key: String): String = stringOrNull(key).orEmpty()
