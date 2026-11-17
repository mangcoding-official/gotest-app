package com.gotest.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLinksTest {
    @Test
    fun `reset password link with the email`() {
        assertEquals(
            AppLink.ResetPassword(token = "abc123", email = "budi+1@contoh.id"),
            AppLinks.parse("https://gotest.id/reset-password/abc123?email=budi%2B1%40contoh.id"),
        )
        assertEquals(
            AppLink.ResetPassword(token = "abc123", email = null),
            AppLinks.parse("https://gotest.id/reset-password/abc123"),
        )
    }

    @Test
    fun `join link keeps the code upper-cased`() {
        assertEquals(AppLink.JoinCode("KLS7A2"), AppLinks.parse("https://gotest.id/gabung/kls7a2"))
    }

    @Test
    fun `main bareng link needs a six character code`() {
        assertEquals(AppLink.MainBarengCode("KTR4QZ"), AppLinks.parse("https://gotest.id/main-bareng?kode=ktr4qz"))
        assertNull(AppLinks.parse("https://gotest.id/main-bareng?kode=KTR4"))
        assertNull(AppLinks.parse("https://gotest.id/main-bareng"))
    }

    @Test
    fun `other links are not the app's`() {
        assertNull(AppLinks.parse("http://gotest.id/gabung/ABC"))
        assertNull(AppLinks.parse("https://contoh.id/gabung/ABC"))
        assertNull(AppLinks.parse("https://gotest.id/persetujuan/token"))
        assertNull(AppLinks.parse("bukan tautan"))
        assertNull(AppLinks.parse(null))
    }
}
