package com.gotest.app.feature.assistant.data

import com.gotest.app.core.data.fake.PackageIds
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantSender
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** A seeded conversation; its messages are already timed. */
internal data class SeedConversation(
    val id: String,
    val title: String,
    val messages: List<AssistantMessage>,
)

/**
 * The five conversations of "Riwayat Percakapan" (frame 21622), dated relative to today like the
 * design: two today (09.12 and 07.40), one yesterday (20.15) and two earlier this week. A time
 * that is still ahead today moves back to just before now, so nothing is dated in the future.
 */
internal object AssistantSeedConversations {
    const val RECOMMENDATION_REPLY =
        "Dari hasil terakhirmu, akurasi topik Aljabar masih 25%. Coba mulai dari latihan ini ya:"
    const val TRYOUT_REPLY =
        "Ada! Simulasi UTBK-SNBT Session 1 tersedia sampai 3 hari lagi. Mau langsung mulai?"

    private const val VOICE_QUESTION_SECONDS = 4

    /** Minutes between two seeded messages. */
    private const val MESSAGE_GAP_MINUTES = 1L

    private const val YESTERDAY = 1L
    private const val ALJABAR_DAYS_AGO = 2L
    private const val IRT_DAYS_AGO = 5L
    private val utbkTime = LocalTime.of(9, 12)
    private val logaritmaTime = LocalTime.of(7, 40)
    private val timeManagementTime = LocalTime.of(20, 15)
    private val aljabarTime = LocalTime.of(16, 5)
    private val irtTime = LocalTime.of(19, 30)

    /** Fallback ages for today's two conversations while their design time is still ahead. */
    private const val UTBK_FALLBACK_MINUTES = 5L
    private const val LOGARITMA_FALLBACK_MINUTES = 97L

    fun greetingFor(firstName: String) = "Halo $firstName! Aku asisten belajarmu. Mau latihan apa hari ini?"

    fun create(
        firstName: String,
        nowEpochMillis: Long,
        aljabarPackages: List<StudyPackage>,
        logaritmaPackages: List<StudyPackage>,
        newMessageId: () -> String,
    ): List<SeedConversation> {
        val clock = SeedClock(nowEpochMillis)
        val builder = SeedBuilder(greetingFor(firstName), newMessageId)
        return listOf(
            builder.conversation(
                id = "conv_seed_utbk",
                title = "Persiapan UTBK minggu ini",
                lastMessageAt = clock.at(daysAgo = 0, utbkTime, UTBK_FALLBACK_MINUTES),
                SeedMessage.user("Aku bingung mau latihan apa buat persiapan UTBK"),
                SeedMessage.assistant(RECOMMENDATION_REPLY, packages = aljabarPackages),
                SeedMessage.user("Ada tryout UTBK minggu ini?", voiceDurationSeconds = VOICE_QUESTION_SECONDS),
                SeedMessage.assistant(
                    TRYOUT_REPLY,
                    actions =
                        listOf(AssistantAction.StartTryout(PackageIds.TRYOUT_UTBK_1), AssistantAction.BrowseTryouts),
                ),
            ),
            builder.conversation(
                id = "conv_seed_logaritma",
                title = "Pembahasan soal logaritma",
                lastMessageAt = clock.at(daysAgo = 0, logaritmaTime, LOGARITMA_FALLBACK_MINUTES),
                SeedMessage.user("Bantu bahas soal ini dong: ²log 8 + ²log 4 hasilnya berapa?"),
                SeedMessage.assistant(
                    "²log 8 = 3 karena 2³ = 8, dan ²log 4 = 2 karena 2² = 4. Jadi hasilnya 3 + 2 = 5. " +
                        "Mau coba soal logaritma yang mirip?",
                    packages = logaritmaPackages,
                ),
            ),
            builder.conversation(
                id = "conv_seed_waktu",
                title = "Tips manajemen waktu tryout",
                lastMessageAt = clock.at(daysAgo = YESTERDAY, timeManagementTime),
                SeedMessage.user("Gimana cara bagi waktu pas tryout biar nggak kehabisan waktu?"),
                SeedMessage.assistant(
                    "Kerjakan dulu soal yang paling kamu kuasai, tandai soal yang ragu, lalu kembali setelahnya. " +
                        "Usahakan sekitar 1–2 menit per soal. Mau langsung latihan dengan timer di tryout?",
                    actions =
                        listOf(AssistantAction.StartTryout(PackageIds.TRYOUT_UTBK_1), AssistantAction.BrowseTryouts),
                ),
            ),
            builder.conversation(
                id = "conv_seed_aljabar",
                title = "Rekomendasi latihan Aljabar",
                lastMessageAt = clock.at(daysAgo = ALJABAR_DAYS_AGO, aljabarTime),
                SeedMessage.user("Rekomendasi latihan Aljabar dong"),
                SeedMessage.assistant(RECOMMENDATION_REPLY, packages = aljabarPackages),
            ),
            builder.conversation(
                id = "conv_seed_irt",
                title = "Arti skor IRT di tryout",
                lastMessageAt = clock.at(daysAgo = IRT_DAYS_AGO, irtTime),
                SeedMessage.user("Skor IRT di tryout itu maksudnya apa?"),
                SeedMessage.assistant(
                    "IRT (Item Response Theory) memberi bobot sesuai tingkat kesulitan soal. Soal yang jarang " +
                        "dijawab benar oleh peserta lain bernilai lebih tinggi, jadi skormu bukan sekadar jumlah " +
                        "jawaban benar.",
                    actions = listOf(AssistantAction.OpenLeaderboard),
                ),
            ),
        )
    }

    private class SeedClock(
        private val nowEpochMillis: Long,
    ) {
        private val zone = ZoneId.systemDefault()
        private val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()

        fun at(
            daysAgo: Long,
            time: LocalTime,
            fallbackMinutesAgo: Long = 0,
        ): Long {
            val planned =
                today
                    .minusDays(daysAgo)
                    .atTime(time)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            return if (planned <= nowEpochMillis) {
                planned
            } else {
                nowEpochMillis - TimeUnit.MINUTES.toMillis(fallbackMinutesAgo)
            }
        }
    }

    private class SeedMessage(
        val sender: AssistantSender,
        val text: String,
        val voiceDurationSeconds: Int? = null,
        val packages: List<StudyPackage> = emptyList(),
        val actions: List<AssistantAction> = emptyList(),
    ) {
        companion object {
            fun user(
                text: String,
                voiceDurationSeconds: Int? = null,
            ) = SeedMessage(AssistantSender.User, text, voiceDurationSeconds = voiceDurationSeconds)

            fun assistant(
                text: String,
                packages: List<StudyPackage> = emptyList(),
                actions: List<AssistantAction> = emptyList(),
            ) = SeedMessage(AssistantSender.Assistant, text, packages = packages, actions = actions)
        }
    }

    /** Opens every conversation with the greeting and spaces the messages a minute apart. */
    private class SeedBuilder(
        private val greeting: String,
        private val newMessageId: () -> String,
    ) {
        fun conversation(
            id: String,
            title: String,
            lastMessageAt: Long,
            vararg messages: SeedMessage,
        ): SeedConversation {
            val all = listOf(SeedMessage.assistant(greeting)) + messages
            val gap = TimeUnit.MINUTES.toMillis(MESSAGE_GAP_MINUTES)
            return SeedConversation(
                id = id,
                title = title,
                messages =
                    all.mapIndexed { index, message ->
                        AssistantMessage(
                            id = newMessageId(),
                            sender = message.sender,
                            text = message.text,
                            sentAtEpochMillis = lastMessageAt - gap * (all.lastIndex - index),
                            voiceDurationSeconds = message.voiceDurationSeconds,
                            packages = message.packages,
                            actions = message.actions,
                        )
                    },
            )
        }
    }
}
