package com.gotest.app.feature.ruang

import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamItemType
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shapes from API-CONTRACTS "API Android modul G" and the Ujian preflight 6.2. */
class ExamAnswersTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun obj(text: String): JsonObject = json.parseToJsonElement(text).jsonObject

    @Test
    fun workPageFromTheContractExampleIsRead() {
        val page =
            json.decodeFromString(
                ExamWorkPage.serializer(),
                """
                {"organization":{"name":"SMP 1 Bantargedung","code":"SMP1BTG"},"title":"PTS Matematika Kelas 8",
                 "attempt":{"id":9,"status":"in_progress","deadlineAt":"2026-12-01T02:30:00+00:00",
                  "serverNow":"2026-12-01T01:30:05+00:00","violations":0,"violationLimit":3,"deviceToken":"Hk3",
                  "lastClientSeq":0},"mode":"exam",
                 "blocks":[{"kind":"group","groupId":4,"title":"Bacaan 1","passage":"<p>…</p>","mediaUrl":null,
                  "itemIds":[55,56]}],
                 "items":[{"id":55,"number":1,"type":"single_choice","prompt":"<p>…</p>","mediaUrl":null,
                  "content":{"options":[{"id":"c","text":"…","media_url":null}]}}],
                 "answers":{"55":{"selected":"c"}}}
                """.trimIndent(),
            )
        assertEquals(3, page.attempt.violationLimit)
        assertEquals(listOf(55L, 56L), page.blocks.single().itemIds)
        val item = page.items.single()
        assertEquals(ExamAnswer.Single("c"), ExamAnswer.fromJson(item.type, page.answers["55"]))
        val content = ExamContent.of(item.type, item.content) as ExamContent.Choice
        assertEquals("c", content.options.single().id)
    }

    @Test
    fun everyTypeRoundTripsThroughTheServerShape() {
        listOf(
            ExamItemType.SINGLE to ExamAnswer.Single("b"),
            ExamItemType.MULTIPLE to ExamAnswer.Multiple(listOf("a", "c")),
            ExamItemType.TRUE_FALSE to ExamAnswer.TrueFalse(mapOf("s1" to 0, "s2" to 1)),
            ExamItemType.MATCHING to ExamAnswer.Matching(mapOf("l1" to "r3")),
            ExamItemType.SHORT to ExamAnswer.Text("dki jakarta"),
        ).forEach { (type, answer) -> assertEquals(type, answer, ExamAnswer.fromJson(type, answer.toJson())) }
        assertEquals("""{"answers":{"s1":0}}""", ExamAnswer.TrueFalse(mapOf("s1" to 0)).toJson().toString())
    }

    @Test
    fun emptyAnswersAreNotAnswers() {
        assertNull(ExamAnswer.clean(ExamAnswer.Multiple(emptyList())))
        assertNull(ExamAnswer.clean(ExamAnswer.TrueFalse(emptyMap())))
        assertNull(ExamAnswer.clean(ExamAnswer.Matching(emptyMap())))
        assertNull(ExamAnswer.clean(ExamAnswer.Text("   ")))
        // Text is sent as typed, spaces included, like the web.
        assertEquals(ExamAnswer.Text(" 12,5 "), ExamAnswer.clean(ExamAnswer.Text(" 12,5 ")))
        assertNull(ExamAnswer.fromJson(ExamItemType.SINGLE, obj("""{"text":"x"}""")))
    }

    @Test
    fun contentOfEveryTypeIsRead() {
        val trueFalse =
            ExamContent.of(
                ExamItemType.TRUE_FALSE,
                obj("""{"labels":["Fakta","Opini"],"statements":[{"id":"s1","text":"Satu"}]}"""),
            ) as ExamContent.TrueFalse
        assertEquals(listOf("Fakta", "Opini"), trueFalse.labels)
        val defaults =
            ExamContent.of(
                ExamItemType.TRUE_FALSE,
                obj("""{"labels":["X"],"statements":[]}"""),
            ) as ExamContent.TrueFalse
        assertEquals(listOf("Benar", "Salah"), defaults.labels)
        val matching =
            ExamContent.of(
                ExamItemType.MATCHING,
                obj("""{"left":[{"id":"l1","text":"A"}],"right":[{"id":"r2","text":"B"},{"id":"r1","text":"C"}]}"""),
            ) as ExamContent.Matching
        assertEquals(listOf("r2", "r1"), matching.right.map { it.id })
        assertTrue(
            (
                ExamContent.of(
                    ExamItemType.SHORT,
                    obj("""{"answer_type":"number"}"""),
                ) as ExamContent.ShortAnswer
            ).numeric,
        )
        val multiple = ExamContent.of(ExamItemType.MULTIPLE, obj("""{"options":[]}""")) as ExamContent.Choice
        assertTrue(multiple.multiple)
    }
}
