package com.gotest.app.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** A photo saved on the server shows in the app (T-026 A2a, staging test on a phone). */
class GotestAvatarTest {
    private val base = "https://staging.gotest.id/"

    @Test
    fun aStoredPhotoIsJoinedWithTheServer() {
        assertEquals(
            "https://staging.gotest.id/storage/avatars/a1.jpg",
            serverImage("/storage/avatars/a1.jpg", base),
        )
    }

    @Test
    fun fullAddressesPickedPhotosAndDrawablesStayAsTheyAre() {
        assertEquals("https://cdn.contoh.id/a.jpg", serverImage("https://cdn.contoh.id/a.jpg", base))
        val captured = "file:///data/user/0/com.gotest.app/cache/a.jpg"
        assertEquals(captured, serverImage(captured, base))
        assertEquals("content://media/external/images/1", serverImage("content://media/external/images/1", base))
        assertEquals("//cdn.contoh.id/a.jpg", serverImage("//cdn.contoh.id/a.jpg", base))
        assertEquals(42, serverImage(42, base))
        assertEquals(null, serverImage(null, base))
    }
}
