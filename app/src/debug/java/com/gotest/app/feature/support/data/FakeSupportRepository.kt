package com.gotest.app.feature.support.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.support.domain.NewTicket
import com.gotest.app.feature.support.domain.SupportPage
import com.gotest.app.feature.support.domain.SupportRepository
import com.gotest.app.feature.support.domain.SupportSection
import com.gotest.app.feature.support.domain.SupportTicket
import com.gotest.app.feature.support.domain.SupportTopic
import com.gotest.app.feature.support.domain.SupportWriteReply
import com.gotest.app.feature.support.domain.TicketReply
import com.gotest.app.feature.support.domain.TicketStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug Bantuan (debug builds only): two tickets, the server's rules and messages for a new ticket and a
 * reply (StudentSupport), and the three topics of StudentSupport::topics.
 */
@Singleton
class FakeSupportRepository
    @Inject
    constructor() : SupportRepository {
        private val categories =
            linkedMapOf(
                "akun" to "Akun dan masuk",
                "akses" to "Akses Premium atau Mitra",
                "soal" to "Soal atau pembahasan",
                "pengerjaan" to "Latihan, Try Out, atau hasil",
                "teknis" to "Kendala teknis",
                "lainnya" to "Lainnya",
            )
        private var nextId = FIRST_NEW_ID
        private val tickets = mutableListOf(answered(), resolved())

        override suspend fun page(): Result<SupportPage> {
            simulateLatency()
            return Result.success(
                SupportPage(
                    tickets = tickets.sortedByDescending(SupportTicket::id).take(LIMIT),
                    ticketCategories = categories,
                ),
            )
        }

        override suspend fun topic(slug: String): Result<SupportTopic> {
            simulateLatency()
            return topics[slug]?.let { Result.success(it) }
                ?: Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))
        }

        override suspend fun sendTicket(ticket: NewTicket): Result<SupportWriteReply> {
            simulateLatency()
            val errors = ticketErrors(ticket)
            if (errors.isNotEmpty()) return Result.failure(invalid(errors))
            val id = nextId++
            tickets +=
                SupportTicket(
                    id = id,
                    subject = ticket.subject.trim(),
                    category = categories.getValue(ticket.category),
                    message = ticket.message.trim(),
                    status = TicketStatus.Open,
                    statusLabel = "Baru",
                    createdAt = Instant.now().toString(),
                )
            return Result.success(
                SupportWriteReply(message = "Tiket terkirim. Tim Gotest membalas di halaman ini.", id = id),
            )
        }

        override suspend fun reply(
            ticketId: Long,
            body: String,
        ): Result<SupportWriteReply> {
            simulateLatency()
            val index = tickets.indexOfFirst { it.id == ticketId }
            if (index < 0) return Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))
            if (body.isBlank()) return Result.failure(invalid(mapOf("body" to "Tulis pesanmu.")))
            val ticket = tickets[index]
            val reply =
                TicketReply(
                    id = nextId++,
                    author = "Gilang Permana",
                    body = body.trim(),
                    createdAt = Instant.now().toString(),
                )
            // A reply on a resolved ticket opens it again (StudentSupport).
            tickets[index] =
                ticket.copy(
                    replies = ticket.replies + reply,
                    status = if (ticket.status == TicketStatus.Resolved) TicketStatus.Open else ticket.status,
                    statusLabel = if (ticket.status == TicketStatus.Resolved) "Baru" else ticket.statusLabel,
                )
            return Result.success(SupportWriteReply(id = reply.id))
        }

        private fun ticketErrors(ticket: NewTicket): Map<String, String> {
            val errors = mutableMapOf<String, String>()
            if (ticket.subject.isBlank()) errors["subject"] = "Tulis judul singkat kendalamu."
            if (ticket.category !in categories) errors["category"] = "Pilih jenis kendala."
            val open = tickets.count { it.status != TicketStatus.Resolved }
            when {
                ticket.message.isBlank() -> {
                    errors["message"] = "Tulis kendalamu."
                }

                ticket.message.trim().length < MIN_MESSAGE -> {
                    errors["message"] = "Ceritakan kendalamu minimal 10 karakter supaya tim bisa membantu."
                }

                open >= MAX_OPEN -> {
                    errors["message"] =
                        "Kamu masih punya $open tiket yang belum selesai. Tunggu balasan tim dulu, atau tambahkan keterangan di tiket yang ada."
                }
            }
            return errors
        }

        private fun invalid(errors: Map<String, String>): ApiError =
            ApiError(UNPROCESSABLE, errors.values.first(), "validation", errors.mapValues { listOf(it.value) })

        private fun ago(days: Long): String = Instant.now().minus(days, ChronoUnit.DAYS).toString()

        private fun answered() =
            SupportTicket(
                id = 31,
                subject = "Tidak bisa membuka hasil Try Out",
                category = "Latihan, Try Out, atau hasil",
                message =
                    "Setelah mengumpulkan Try Out UTBK-SNBT 1, halaman hasil tidak terbuka.\n" +
                        "Sudah coba keluar dan masuk lagi.",
                status = TicketStatus.InProgress,
                statusLabel = "Ditangani",
                createdAt = ago(2),
                replies =
                    listOf(
                        TicketReply(
                            90,
                            fromTeam = true,
                            author = "Tim Gotest",
                            body = "Terima kasih, kami sedang memeriksanya.",
                            createdAt = ago(1),
                        ),
                    ),
            )

        private fun resolved() =
            SupportTicket(
                id = 24,
                subject = "Ganti email akun",
                category = "Akun dan masuk",
                message = "Bagaimana cara mengganti email akun saya?",
                status = TicketStatus.Resolved,
                statusLabel = "Selesai",
                createdAt = ago(9),
                replies =
                    listOf(
                        TicketReply(
                            70,
                            fromTeam = true,
                            author = "Tim Gotest",
                            body = "Buka Profil, lalu Edit Informasi Akun.",
                            createdAt = ago(8),
                        ),
                    ),
            )

        private companion object {
            const val LIMIT = 20
            const val MAX_OPEN = 5
            const val MIN_MESSAGE = 10
            const val FIRST_NEW_ID = 100L
            const val NOT_FOUND = 404
            const val UNPROCESSABLE = 422

            val topics =
                listOf(
                    SupportTopic(
                        "akun-profil",
                        "Akun & Profil",
                        "Kelola Akun, profile, dan keamanan",
                        listOf(
                            SupportSection(
                                "akun",
                                "Akun",
                                "Pengaturan akun digunakan untuk mengelola informasi profil dan keamanan akun Anda. Di bagian ini, Anda dapat memperbarui nama yang ditampilkan, foto profil, dan password.",
                            ),
                            SupportSection(
                                "pengaturan-preferensi",
                                "Pengaturan Preferensi",
                                "Pengaturan preferensi memungkinkan Anda menyesuaikan pengalaman penggunaan sesuai kebutuhan. Tinjau pilihan yang tersedia dan atur preferensi agar aplikasi lebih nyaman digunakan.",
                            ),
                            SupportSection(
                                "cara-mengganti-foto",
                                "Cara mengganti Foto",
                                "Foto profil membantu pengguna lain mengenali Anda. Untuk menggantinya, pilih foto baru pada pengaturan profil, sesuaikan tampilannya, lalu simpan perubahan.",
                            ),
                            SupportSection(
                                "cara-mengganti-password",
                                "Cara mengganti Password",
                                "Ganti password untuk menjaga keamanan akun Anda. Masukkan password saat ini, buat password baru yang kuat, lalu konfirmasi password baru sebelum menyimpan perubahan.",
                            ),
                            SupportSection(
                                "cara-mengganti-email",
                                "cara mengganti email",
                                "Email dipakai untuk masuk dan menerima pemberitahuan akun. Buka Pengaturan, pilih Edit Informasi Akun, masukkan email baru, lalu simpan perubahan.",
                            ),
                            SupportSection(
                                "cara-mengganti-display-name",
                                "Cara mengganti display name",
                                "Display name adalah nama yang ditampilkan pada profil Anda. Untuk menggantinya, ubah nama pada pengaturan profil, lalu simpan perubahan agar nama baru ditampilkan.",
                            ),
                        ),
                    ),
                    SupportTopic(
                        "paket-latihan-tryout",
                        "Paket Latihan & Try Out",
                        "mengerjakan, report latihan, dan Try Out",
                        listOf(
                            SupportSection(
                                "report-latihan",
                                "Bagaimana cara melihat report latihan",
                                "Untuk melihat report latihan, Anda dapat mengakses menu \"Paket Latihan\" di dashboard. Setelah itu, pilih latihan yang ingin Anda lihat laporannya. Di halaman tersebut, Anda akan menemukan ringkasan hasil, termasuk skor dan analisis performa Anda.",
                            ),
                            SupportSection(
                                "ikuti-tryout",
                                "Bagaimana cara mengikuti Try Out",
                                "Buka menu \"Paket Try Out\", pilih paket yang tersedia, lalu tekan Mulai. Timer akan berjalan per subtes. Pastikan koneksi stabil sebelum memulai karena waktu tetap berjalan setelah tryout dimulai.",
                            ),
                            SupportSection(
                                "hasil-tryout",
                                "Di mana saya bisa melihat hasil Try Out",
                                "Setelah Try Out dikumpulkan, hasil muncul di halaman hasil paket tersebut. Kamu juga bisa membuka menu Riwayat untuk melihat skor, durasi, dan analisis percobaan sebelumnya.",
                            ),
                            SupportSection(
                                "pembahasan",
                                "Bagaimana cara melihat pembahasan soal",
                                "Buka hasil latihan yang sudah selesai, lalu pilih Pembahasan. Setiap soal menampilkan jawaban kamu, kunci, dan penjelasan. Try Out hanya menampilkan skor, hasil per subtes, dan peringkat, tanpa kunci dan pembahasan.",
                            ),
                        ),
                    ),
                    SupportTopic(
                        "pembayaran-berlangganan",
                        "Pembayaran & Berlangganan",
                        "Upgrade akun, metode pembayaran, dan invoice",
                        listOf(
                            SupportSection(
                                "upgrade",
                                "Bagaimana cara upgrade akun ke akun berbayar",
                                "Hubungi tim support melalui email atau WhatsApp untuk upgrade paket. Tim akan membantu metode pembayaran dan mengaktifkan langganan di akun kamu.",
                            ),
                        ),
                    ),
                ).associateBy(SupportTopic::slug)
        }
    }
