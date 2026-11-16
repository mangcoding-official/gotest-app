package com.gotest.app.core.config

/**
 * Features the app keeps hidden until the web has them (K-AN2, K-AN3; T-018). Their code stays for
 * later, but while a flag is false its screens are not in the navigation graph and nothing in the
 * app leads to them: no tab, button, banner, link, or snackbar action.
 */
object AppFeatures {
    /** Asisten AI (the round button next to the bottom bar). */
    const val ASSISTANT: Boolean = false

    /** Upgrade, Langganan, promo, and paying in the app (VA, QRIS, e-wallet). */
    const val PAYMENTS: Boolean = false

    /** The Notifikasi inbox behind the bell on Beranda. */
    const val NOTIFICATION_INBOX: Boolean = false

    /** Paket Tersimpan and the bookmark buttons that fill it. */
    const val SAVED_PACKAGES: Boolean = false

    /** "Pengingat Belajar" in Pengaturan. */
    const val STUDY_REMINDER: Boolean = false

    /** "Unduh Data Akun" in Pengaturan. */
    const val ACCOUNT_DATA_DOWNLOAD: Boolean = false
}
