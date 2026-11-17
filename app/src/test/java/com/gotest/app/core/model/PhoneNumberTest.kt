package com.gotest.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `every way of typing an Indonesian number becomes 628`() {
        assertEquals("6281234567890", PhoneNumber.normalize("0812 3456 7890"))
        assertEquals("6281234567890", PhoneNumber.normalize("812-3456-7890"))
        assertEquals("6281234567890", PhoneNumber.normalize("+62 812 3456 7890"))
        assertEquals("6281234567890", PhoneNumber.normalize("6281234567890"))
    }

    @Test
    fun `what is not a mobile number is refused`() {
        assertNull(PhoneNumber.normalize("12345"))
        assertNull(PhoneNumber.normalize("021 555 1234"))
        assertNull(PhoneNumber.normalize("0812"))
        assertNull(PhoneNumber.normalize(""))
    }

    @Test
    fun `shown as the web shows it`() {
        assertEquals("0812-3456-7890", PhoneNumber.display("6281234567890"))
        assertEquals("0812-3456-789", PhoneNumber.display("628123456789"))
    }
}
