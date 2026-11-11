package com.gotest.app.feature.account.ui.components

import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.HelpArticle
import com.gotest.app.feature.account.domain.HelpCategory
import com.gotest.app.feature.account.domain.HelpCategoryKind
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpContactType
import com.gotest.app.feature.account.domain.HelpFaq
import com.gotest.app.feature.account.domain.HelpTopic
import com.gotest.app.feature.account.domain.HelpTopicTag
import com.gotest.app.feature.account.domain.MainLevel
import com.gotest.app.feature.account.domain.PersonalDetails
import com.gotest.app.feature.account.domain.SettingSwitch
import java.time.LocalDate
import java.time.LocalTime

/** Sample data for the account @Preview functions only (texts and dates from the design frames). */
internal object AccountPreviewData {
    val trialSubscription =
        Subscription(
            tier = PlanTier.FreeTrial,
            planName = "Paket Free Trial",
            activatedOn = LocalDate.of(2026, 9, 23),
            activeUntil = LocalDate.of(2026, 10, 23),
            paymentMethod = null,
        )

    val premiumSubscription =
        Subscription(
            tier = PlanTier.Premium,
            planName = "GoTest Premium Tahunan",
            activatedOn = LocalDate.of(2026, 10, 21),
            activeUntil = LocalDate.of(2027, 10, 28),
            paymentMethod = "Virtual Account BCA",
        )

    val profile =
        UserProfile(
            id = "user_gilang",
            fullName = "Gilang Permana",
            email = "gilangpermana@mail.com",
            avatarUrl = null,
            stats =
                UserStats(
                    questionsAnswered = 100,
                    accuracyPercent = 86,
                    packagesCompleted = 412,
                    questionsTrendPercent = 0,
                    accuracyTrendPercent = 0,
                    packagesTrendPercent = 0,
                ),
            subscription = trialSubscription,
        )

    val payments =
        listOf(
            PaymentRecord(
                id = "pay_2026",
                planName = "Premium Tahunan",
                paidOn = LocalDate.of(2026, 10, 21),
                methodLabel = "VA BCA",
                amountRupiah = 110_000,
                succeeded = true,
            ),
            PaymentRecord(
                id = "pay_2025",
                planName = "Premium Tahunan",
                paidOn = LocalDate.of(2025, 10, 21),
                methodLabel = "VA BCA",
                amountRupiah = 110_000,
                succeeded = true,
            ),
        )

    val settings =
        AccountSettings(
            switches =
                mapOf(
                    SettingSwitch.ActivityResults to true,
                    SettingSwitch.RankingUpdates to true,
                    SettingSwitch.NewPackages to false,
                    SettingSwitch.SubscriptionStatus to true,
                    SettingSwitch.StudyReminder to true,
                    SettingSwitch.PromoEmails to false,
                ),
            reminderTime = LocalTime.of(19, 0),
            mainLevel = MainLevel.UtbkSnbt,
        )

    val personalDetails =
        PersonalDetails(
            phoneNumber = "0812-3456-7890",
            birthDate = LocalDate.of(2008, 5, 14),
            school = "SMA Negeri 3 Bandung",
            city = "Bandung, Jawa Barat",
        )

    val helpCategories =
        listOf(
            HelpCategory(
                "cat_account",
                HelpCategoryKind.Account,
                "Akun & Profil",
                "Kelola akun, profil, dan keamanan",
                "akun",
            ),
            HelpCategory(
                "cat_practice",
                HelpCategoryKind.Practice,
                "Paket Latihan & Tryout",
                "Mengerjakan, report latihan, dan tryout",
                "paket",
            ),
            HelpCategory(
                "cat_payment",
                HelpCategoryKind.Payment,
                "Pembayaran & Berlangganan",
                "Upgrade akun, metode bayar, dan invoice",
                "pembayaran",
            ),
        )

    val helpFaqs =
        listOf(
            HelpFaq(
                "faq_report",
                "Bagaimana cara melihat report latihan?",
                "Buka menu Paket, pilih paket latihan yang sudah dikerjakan, lalu ketuk \"Lihat Hasil\" " +
                    "untuk melihat skor dan pembahasan.",
            ),
            HelpFaq("faq_tryout", "Bagaimana cara mengikuti tryout?", "Buka menu Paket lalu pilih Paket Tryout."),
            HelpFaq("faq_result", "Di mana saya bisa melihat hasil tryout?", "Semua hasil ada di menu Riwayat."),
            HelpFaq("faq_discussion", "Bagaimana cara melihat pembahasan soal?", "Ketuk \"Lihat Pembahasan\"."),
            HelpFaq("faq_upgrade", "Bagaimana cara upgrade ke akun berbayar?", "Ketuk \"Upgrade\" di Profil."),
        )

    val emailContact = HelpContact(HelpContactType.Email, "support@gotest.id", "mailto:support@gotest.id")

    val helpContacts =
        listOf(
            emailContact,
            HelpContact(HelpContactType.WhatsApp, "+62 3142 2453", "https://wa.me/6231422453"),
        )

    val helpTopic =
        HelpTopic(
            id = "akun",
            title = "Akun & Profil",
            subtitle = "Kelola akun, profil, dan keamanan",
            tags =
                listOf(
                    HelpTopicTag("akun", "Akun"),
                    HelpTopicTag("preferensi", "Preferensi"),
                    HelpTopicTag("foto", "Foto Profil"),
                    HelpTopicTag("password", "Password"),
                    HelpTopicTag("email", "Email"),
                    HelpTopicTag("display_name", "Display Name"),
                ),
            articles =
                listOf(
                    HelpArticle(
                        "akun_pengaturan",
                        setOf("akun"),
                        "Pengaturan Akun",
                        "Pengaturan akun digunakan untuk mengelola informasi profil dan keamanan. Buka Profil → " +
                            "Edit Informasi Akun untuk mengubah data.",
                    ),
                    HelpArticle(
                        "akun_preferensi",
                        setOf("akun", "preferensi"),
                        "Pengaturan Preferensi",
                        "Sesuaikan pengalaman belajar seperti jenjang utama dan pengingat belajar melalui Profil → " +
                            "Pengaturan.",
                    ),
                    HelpArticle(
                        "akun_foto",
                        setOf("akun", "foto"),
                        "Cara mengganti foto profil",
                        "Ketuk foto profilmu.",
                    ),
                    HelpArticle(
                        "akun_password",
                        setOf("akun", "password"),
                        "Cara mengganti password",
                        "Buka Ganti Password.",
                    ),
                    HelpArticle(
                        "akun_email",
                        setOf("akun", "email"),
                        "Cara mengganti email",
                        "Buka Edit Informasi Akun.",
                    ),
                    HelpArticle(
                        "akun_display_name",
                        setOf("akun", "display_name"),
                        "Cara mengganti display name",
                        "Buka Edit Informasi Akun.",
                    ),
                ),
            supportContact = emailContact,
        )
}
