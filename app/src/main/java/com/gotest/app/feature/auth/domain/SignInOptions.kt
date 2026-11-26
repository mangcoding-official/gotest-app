package com.gotest.app.feature.auth.domain

/**
 * The ways of signing in and signing up the server has switched on (`GET /referensi`, T-026 required 12): the
 * Nomor HP tab, the Google button, and "Kode lewat email". Masuk with email and password is always there.
 * [passwordResetWhatsApp]: Lupa Sandi takes an email or a WhatsApp number and sends the link to WhatsApp.
 */
data class SignInOptions(
    val phone: Boolean = false,
    val google: Boolean = false,
    val emailCode: Boolean = false,
    val passwordResetWhatsApp: Boolean = false,
)

interface SignInOptionsRepository {
    /** Everything off while the server cannot be reached, so only email and password show. */
    suspend fun options(): SignInOptions
}
