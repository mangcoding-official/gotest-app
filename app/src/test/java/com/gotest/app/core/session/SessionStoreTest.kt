package com.gotest.app.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStoreTest {
    private val disk = InMemoryStore()
    private val cipher = ReversingCipher()

    @Test
    fun `a saved token survives a restart and is not written in plain text`() {
        SessionStore(disk, cipher).save("12|rahasia")

        val reopened = SessionStore(disk, cipher)
        assertTrue(reopened.isSignedIn.value)
        assertEquals("12|rahasia", reopened.token)
        assertNotEquals("12|rahasia", disk.values.values.single())
    }

    @Test
    fun `signing out removes the token from the device`() {
        val session = SessionStore(disk, cipher).apply { save("12|rahasia") }
        session.clear()

        assertFalse(session.isSignedIn.value)
        assertNull(session.token)
        assertTrue(disk.values.isEmpty())
        assertFalse(SessionStore(disk, cipher).isSignedIn.value)
    }

    @Test
    fun `a token that can no longer be read signs out and is dropped`() {
        SessionStore(disk, cipher).save("12|rahasia")

        val reopened = SessionStore(disk, BrokenCipher())
        assertFalse(reopened.isSignedIn.value)
        assertTrue(disk.values.isEmpty())
    }

    @Test
    fun `nothing saved means signed out`() {
        assertFalse(SessionStore(disk, cipher).isSignedIn.value)
    }

    private class InMemoryStore : KeyValueStore {
        val values = mutableMapOf<String, String>()

        override fun getString(key: String): String? = values[key]

        override fun putString(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }

    private class ReversingCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain.reversed()

        override fun decrypt(encrypted: String): String = encrypted.reversed()
    }

    private class BrokenCipher : TokenCipher {
        override fun encrypt(plain: String): String = plain

        override fun decrypt(encrypted: String): String? = null
    }
}
