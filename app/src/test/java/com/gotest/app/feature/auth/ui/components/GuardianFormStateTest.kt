package com.gotest.app.feature.auth.ui.components

import com.gotest.app.R
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.auth.domain.GuardianAnswer
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianFormStateTest {
    private val both =
        GuardianQuestion(
            age = 18,
            levels = setOf(EducationLevel.Sd, EducationLevel.Smp, EducationLevel.SmaSmk),
            channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email),
        )

    @Test
    fun `the question shows only for the levels the server names`() {
        assertTrue(both.asksFor(EducationLevel.Smp))
        assertFalse(both.asksFor(EducationLevel.Kuliah))
        assertFalse(both.asksFor(null))
        assertFalse(null.asksFor(EducationLevel.Smp))
    }

    @Test
    fun `no answer asks for Ya or Belum`() {
        val (answer, state) = GuardianFormState().check(both)
        assertNull(answer)
        assertEquals(R.string.guardian_answer_required, state.answerErrorRes)
    }

    @Test
    fun `Ya needs no contact`() {
        val (answer, _) = GuardianFormState(adult = true, contact = "abaikan").check(both)
        assertEquals(GuardianAnswer(adult = true, channel = null, contact = null), answer)
    }

    @Test
    fun `Belum needs a valid WhatsApp number on the first channel`() {
        assertEquals(
            R.string.guardian_phone_required,
            GuardianFormState(adult = false).check(both).second.contactErrorRes,
        )
        assertEquals(
            R.string.phone_invalid,
            GuardianFormState(adult = false, contact = "12").check(both).second.contactErrorRes,
        )
        val (answer, state) = GuardianFormState(adult = false, contact = " 0812 3456 7890 ").check(both)
        assertNull(state.contactErrorRes)
        assertEquals(
            GuardianAnswer(adult = false, channel = GuardianChannel.WhatsApp, contact = "0812 3456 7890"),
            answer,
        )
    }

    @Test
    fun `Belum by email needs a valid email`() {
        val email = GuardianFormState(adult = false, channel = GuardianChannel.Email)
        assertEquals(R.string.guardian_email_required, email.check(both).second.contactErrorRes)
        assertEquals(
            R.string.guardian_email_invalid,
            email
                .copy(contact = "ibu@contoh")
                .check(both)
                .second.contactErrorRes,
        )
        assertEquals(
            GuardianChannel.Email,
            email
                .copy(contact = "ibu@contoh.id")
                .check(both)
                .first
                ?.channel,
        )
    }

    @Test
    fun `a channel that is not ready falls back to the first one ready`() {
        val whatsappOnly = both.copy(channels = listOf(GuardianChannel.WhatsApp))
        assertEquals(GuardianChannel.WhatsApp, GuardianFormState(channel = null).channelFor(whatsappOnly))
        assertEquals(GuardianChannel.Email, GuardianFormState(channel = GuardianChannel.Email).channelFor(both))
    }
}
