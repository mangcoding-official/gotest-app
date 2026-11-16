package com.gotest.app.feature.auth.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentStateTest {
    private val sent =
        ConsentState(
            status = ConsentStatus.Pending,
            contact = "0812-****-890",
            channel = GuardianChannel.WhatsApp,
            channels = listOf(GuardianChannel.WhatsApp),
            delivery = ConsentDelivery.Sent,
            expiresAt = null,
        )

    @Test
    fun `sending again is offered unless the contact is held back for the day`() {
        assertTrue(sent.canResend)
        assertFalse(sent.copy(delivery = ConsentDelivery.Limited).canResend)
        assertFalse(sent.copy(status = ConsentStatus.Expired, delivery = ConsentDelivery.Limited).canResend)
    }

    @Test
    fun `an email link while email codes are off cannot go out again`() {
        val email =
            sent.copy(
                channel = GuardianChannel.Email,
                contact = "r***@contoh.id",
                delivery = ConsentDelivery.Failed,
            )
        assertTrue(email.emailClosed)
        assertFalse(email.canResend)
        assertFalse(email.copy(channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email)).emailClosed)
    }

    @Test
    fun `kontak when email reaches the parent, nomor otherwise`() {
        assertFalse(sent.offersEmail)
        assertTrue(sent.copy(channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email)).offersEmail)
        assertTrue(sent.copy(channel = GuardianChannel.Email).offersEmail)
    }
}
