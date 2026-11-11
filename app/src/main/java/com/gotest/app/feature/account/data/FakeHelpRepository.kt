package com.gotest.app.feature.account.data

import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.account.domain.HelpArticle
import com.gotest.app.feature.account.domain.HelpCategory
import com.gotest.app.feature.account.domain.HelpCategoryKind
import com.gotest.app.feature.account.domain.HelpCenter
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpContactType
import com.gotest.app.feature.account.domain.HelpFaq
import com.gotest.app.feature.account.domain.HelpRepository
import com.gotest.app.feature.account.domain.HelpTopic
import com.gotest.app.feature.account.domain.HelpTopicTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** Help content from the "Bantuan & Support" and "Bantuan / Detail Topik" frames, plus filler for the other topics. */
@Singleton
class FakeHelpRepository
    @Inject
    constructor() : HelpRepository {
        private val emailContact =
            HelpContact(
                type = HelpContactType.Email,
                displayValue = "support@gotest.id",
                uri = "mailto:support@gotest.id",
            )

        private val helpCenter =
            HelpCenter(
                categories =
                    listOf(
                        HelpCategory(
                            id = "cat_account",
                            kind = HelpCategoryKind.Account,
                            title = "Akun & Profil",
                            subtitle = "Kelola akun, profil, dan keamanan",
                            topicId = TOPIC_ACCOUNT,
                        ),
                        HelpCategory(
                            id = "cat_practice",
                            kind = HelpCategoryKind.Practice,
                            title = "Paket Latihan & Tryout",
                            subtitle = "Mengerjakan, report latihan, dan tryout",
                            topicId = TOPIC_PRACTICE,
                        ),
                        HelpCategory(
                            id = "cat_payment",
                            kind = HelpCategoryKind.Payment,
                            title = "Pembayaran & Berlangganan",
                            subtitle = "Upgrade akun, metode bayar, dan invoice",
                            topicId = TOPIC_PAYMENT,
                        ),
                    ),
                faqs =
                    listOf(
                        HelpFaq(
                            id = "faq_report",
                            question = "Bagaimana cara melihat report latihan?",
                            answer =
                                "Buka menu Paket, pilih paket latihan yang sudah dikerjakan, lalu ketuk " +
                                    "\"Lihat Hasil\" untuk melihat skor dan pembahasan.",
                        ),
                        HelpFaq(
                            id = "faq_tryout",
                            question = "Bagaimana cara mengikuti tryout?",
                            answer =
                                "Buka menu Paket lalu pilih Paket Tryout. Pilih tryout yang tersedia dan ketuk " +
                                    "\"Mulai Tryout\". Pastikan koneksimu stabil sebelum mulai.",
                        ),
                        HelpFaq(
                            id = "faq_tryout_result",
                            question = "Di mana saya bisa melihat hasil tryout?",
                            answer =
                                "Semua hasil ada di menu Riwayat. Pilih tryout yang sudah selesai, lalu ketuk " +
                                    "\"Lihat Hasil\" untuk melihat skor dan peringkatmu.",
                        ),
                        HelpFaq(
                            id = "faq_discussion",
                            question = "Bagaimana cara melihat pembahasan soal?",
                            answer =
                                "Dari halaman hasil, ketuk \"Lihat Pembahasan\" untuk melihat jawaban benar " +
                                    "dan penjelasan setiap soal.",
                        ),
                        HelpFaq(
                            id = "faq_upgrade",
                            question = "Bagaimana cara upgrade ke akun berbayar?",
                            answer =
                                "Buka Profil, lalu ketuk \"Upgrade\" pada kartu paketmu. Pilih paket Premium " +
                                    "dan selesaikan pembayaran.",
                        ),
                    ),
                contacts =
                    listOf(
                        emailContact,
                        HelpContact(
                            type = HelpContactType.WhatsApp,
                            displayValue = "+62 3142 2453",
                            uri = "https://wa.me/6231422453",
                        ),
                    ),
                operatingHours = "Senin–Jumat, 08:00–17:00 (kecuali libur nasional)",
            )

        private val topics: Map<String, HelpTopic> =
            listOf(accountTopic(), practiceTopic(), paymentTopic()).associateBy { it.id }

        override fun observeHelpCenter(): Flow<HelpCenter> = flowOf(helpCenter).withFakeLatency()

        override fun observeTopic(topicId: String): Flow<HelpTopic> =
            flow { emit(checkNotNull(topics[topicId]) { "Unknown help topic $topicId" }) }.withFakeLatency()

        private fun accountTopic() =
            HelpTopic(
                id = TOPIC_ACCOUNT,
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
                            id = "akun_pengaturan",
                            tagIds = setOf("akun"),
                            question = "Pengaturan Akun",
                            answer =
                                "Pengaturan akun digunakan untuk mengelola informasi profil dan keamanan. " +
                                    "Buka Profil → Edit Informasi Akun untuk mengubah data.",
                        ),
                        HelpArticle(
                            id = "akun_preferensi",
                            tagIds = setOf("akun", "preferensi"),
                            question = "Pengaturan Preferensi",
                            answer =
                                "Sesuaikan pengalaman belajar seperti jenjang utama dan pengingat belajar " +
                                    "melalui Profil → Pengaturan.",
                        ),
                        HelpArticle(
                            id = "akun_foto",
                            tagIds = setOf("akun", "foto"),
                            question = "Cara mengganti foto profil",
                            answer =
                                "Buka Profil lalu ketuk foto profilmu. Pilih foto dari galeri, lalu ketuk " +
                                    "Simpan. Gunakan JPG, JPEG, atau PNG maksimal 5 MB.",
                        ),
                        HelpArticle(
                            id = "akun_password",
                            tagIds = setOf("akun", "password"),
                            question = "Cara mengganti password",
                            answer =
                                "Buka Profil → Ganti Password. Masukkan password sebelumnya, lalu password " +
                                    "baru minimal 8 karakter yang berisi huruf dan angka.",
                        ),
                        HelpArticle(
                            id = "akun_email",
                            tagIds = setOf("akun", "email"),
                            question = "Cara mengganti email",
                            answer =
                                "Buka Profil → Edit Informasi Akun, ubah email, lalu simpan. Kami akan " +
                                    "mengirim link verifikasi ke email baru.",
                        ),
                        HelpArticle(
                            id = "akun_display_name",
                            tagIds = setOf("akun", "display_name"),
                            question = "Cara mengganti display name",
                            answer =
                                "Buka Profil → Edit Informasi Akun, ubah Display Name, lalu ketuk " +
                                    "Simpan Informasi Akun.",
                        ),
                    ),
                supportContact = emailContact,
            )

        private fun practiceTopic() =
            HelpTopic(
                id = TOPIC_PRACTICE,
                title = "Paket Latihan & Tryout",
                subtitle = "Mengerjakan, report latihan, dan tryout",
                tags =
                    listOf(
                        HelpTopicTag("paket", "Paket"),
                        HelpTopicTag("latihan", "Latihan"),
                        HelpTopicTag("tryout", "Tryout"),
                        HelpTopicTag("hasil", "Hasil"),
                        HelpTopicTag("pembahasan", "Pembahasan"),
                    ),
                articles =
                    listOf(
                        HelpArticle(
                            id = "paket_mulai",
                            tagIds = setOf("paket", "latihan"),
                            question = "Cara memulai latihan",
                            answer =
                                "Buka menu Paket, pilih jenjang dan paket latihan, lalu ketuk Mulai Latihan. " +
                                    "Waktu berjalan otomatis setelah kamu mulai.",
                        ),
                        HelpArticle(
                            id = "paket_tryout",
                            tagIds = setOf("paket", "tryout"),
                            question = "Cara mengikuti tryout",
                            answer =
                                "Buka menu Paket → Paket Tryout, pilih tryout yang tersedia, lalu ketuk " +
                                    "Mulai Tryout.",
                        ),
                        HelpArticle(
                            id = "paket_simpan",
                            tagIds = setOf("paket", "latihan", "tryout"),
                            question = "Apakah jawaban tersimpan otomatis?",
                            answer =
                                "Ya. Jawabanmu tersimpan otomatis, jadi kamu bisa melanjutkan kapan saja " +
                                    "selama waktu masih tersedia.",
                        ),
                        HelpArticle(
                            id = "paket_hasil",
                            tagIds = setOf("paket", "hasil"),
                            question = "Cara melihat report latihan",
                            answer =
                                "Buka menu Riwayat, pilih paket yang sudah dikerjakan, lalu ketuk Lihat Hasil " +
                                    "untuk melihat skor dan akurasi per topik.",
                        ),
                        HelpArticle(
                            id = "paket_pembahasan",
                            tagIds = setOf("paket", "pembahasan"),
                            question = "Cara melihat pembahasan soal",
                            answer =
                                "Dari halaman hasil, ketuk Lihat Pembahasan untuk membaca jawaban benar dan " +
                                    "penjelasan setiap soal.",
                        ),
                    ),
                supportContact = emailContact,
            )

        private fun paymentTopic() =
            HelpTopic(
                id = TOPIC_PAYMENT,
                title = "Pembayaran & Berlangganan",
                subtitle = "Upgrade akun, metode bayar, dan invoice",
                tags =
                    listOf(
                        HelpTopicTag("pembayaran", "Pembayaran"),
                        HelpTopicTag("upgrade", "Upgrade"),
                        HelpTopicTag("metode", "Metode Bayar"),
                        HelpTopicTag("invoice", "Invoice"),
                        HelpTopicTag("perpanjang", "Perpanjang"),
                    ),
                articles =
                    listOf(
                        HelpArticle(
                            id = "bayar_upgrade",
                            tagIds = setOf("pembayaran", "upgrade"),
                            question = "Cara upgrade ke Premium",
                            answer =
                                "Buka Profil lalu ketuk Upgrade pada kartu paketmu. Pilih paket Premium dan " +
                                    "metode pembayaran, lalu selesaikan pembayaran.",
                        ),
                        HelpArticle(
                            id = "bayar_metode",
                            tagIds = setOf("pembayaran", "metode"),
                            question = "Metode pembayaran yang tersedia",
                            answer =
                                "Kamu bisa membayar melalui Virtual Account, QRIS, atau e-wallet. Biaya admin " +
                                    "ditampilkan sebelum kamu membayar.",
                        ),
                        HelpArticle(
                            id = "bayar_belum_masuk",
                            tagIds = setOf("pembayaran", "metode"),
                            question = "Pembayaran belum terkonfirmasi",
                            answer =
                                "Konfirmasi biasanya masuk dalam beberapa menit. Jika lebih dari 1 jam, " +
                                    "hubungi kami dan sertakan bukti pembayaranmu.",
                        ),
                        HelpArticle(
                            id = "bayar_invoice",
                            tagIds = setOf("pembayaran", "invoice"),
                            question = "Cara melihat riwayat pembayaran",
                            answer =
                                "Buka Profil → Pengaturan → Langganan untuk melihat detail langganan dan " +
                                    "riwayat pembayaranmu.",
                        ),
                        HelpArticle(
                            id = "bayar_perpanjang",
                            tagIds = setOf("pembayaran", "perpanjang"),
                            question = "Cara memperpanjang langganan",
                            answer =
                                "Buka halaman Langganan lalu ketuk Perpanjang Langganan sebelum masa aktifmu " +
                                    "berakhir.",
                        ),
                    ),
                supportContact = emailContact,
            )

        companion object {
            /** Topic ids used by HelpTopicDestination. */
            const val TOPIC_ACCOUNT = "akun"
            const val TOPIC_PRACTICE = "paket"
            const val TOPIC_PAYMENT = "pembayaran"
        }
    }
