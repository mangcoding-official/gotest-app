package com.gotest.app.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialValidatorTest {
    private val validator = CredentialValidator()

    @Test
    fun `email is required and must look like an address`() {
        assertEquals(CredentialError.Required, validator.validateEmail("  "))
        assertEquals(CredentialError.InvalidEmail, validator.validateEmail("budi@"))
        assertEquals(CredentialError.InvalidEmail, validator.validateEmail("budi mail@contoh.id"))
        assertNull(validator.validateEmail(" budi@contoh.id "))
    }

    @Test
    fun `password needs eight characters`() {
        assertEquals(CredentialError.Required, validator.validatePassword(""))
        assertEquals(CredentialError.PasswordTooShort, validator.validatePassword("1234567"))
        assertNull(validator.validatePassword("12345678"))
    }

    @Test
    fun `confirmation must match the password`() {
        assertEquals(CredentialError.Required, validator.validateConfirmation("rahasia123", ""))
        assertEquals(CredentialError.PasswordMismatch, validator.validateConfirmation("rahasia123", "rahasia124"))
        assertNull(validator.validateConfirmation("rahasia123", "rahasia123"))
    }

    @Test
    fun `a required field may not be blank`() {
        assertEquals(CredentialError.Required, validator.validateRequired(" "))
        assertNull(validator.validateRequired("Budi"))
    }
}
