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

    /**
     * Pengaturan, with Jenjang Utama and Data Pribadi, which the web does not have (A2e-8 = b): the gear on
     * Profil and the Pengaturan row are hidden, the screens stay.
     */
    const val ACCOUNT_SETTINGS: Boolean = false

    /** "Unduh Data Akun" in Pengaturan. */
    const val ACCOUNT_DATA_DOWNLOAD: Boolean = false

    /** The Notifikasi switches in Pengaturan and the Notifikasi row on Profil; the app sends none yet (T-018 D1). */
    const val NOTIFICATION_SETTINGS: Boolean = false

    /** The "Email Promosi" switch in Pengaturan, which the web does not have (T-018 D1). */
    const val PROMO_EMAILS: Boolean = false

    /**
     * The Latihan screens of T-018 that the web does not have (A2c-2 = a): the subject and topic lists, the old
     * package list, Detail Paket and Detail Try Out, the old runner and result, Pembahasan, and Ringkasan
     * Pembahasan. Latihan and Try Out follow the web's catalog since A2c; the screens stay, out of the graph.
     * Notifikasi and Asisten still lead to Detail Paket, so they need this on when they are switched on.
     */
    const val OLD_STUDY_SCREENS: Boolean = false
}
