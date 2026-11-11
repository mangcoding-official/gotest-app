package com.gotest.app.feature.assistant.ui.assistant

import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantAttachment
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantSender
import com.gotest.app.feature.assistant.domain.AttachmentType
import java.time.LocalDate
import java.time.LocalTime

/** The conversation from frame 11990 and the history from frame 21622, for previews. */
internal object AssistantPreviewData {
    private const val SENT_AT = 0L
    private const val DURATION_MINUTES = 45
    private const val PHOTO_BYTES = 1_200_000L
    private const val PDF_BYTES = 820_000L

    const val VOICE_TRANSCRIPT = "Aku mau latihan matematika buat persiapan UTBK"

    private fun practice(
        id: String,
        title: String,
        description: String,
        questionCount: Int,
    ) = StudyPackage(
        id = id,
        title = title,
        description = description,
        type = PackageType.Practice,
        examLabel = "UTBK-SNBT",
        levelLabel = "SMA",
        questionCount = questionCount,
        durationMinutes = DURATION_MINUTES,
        scoringLabel = "+1 / 0",
        progress = null,
        attempts = 0,
        lastAttemptOn = null,
        bestScore = null,
        rank = null,
        participants = null,
    )

    private fun message(
        id: String,
        sender: AssistantSender,
        text: String,
        voiceDurationSeconds: Int? = null,
        packages: List<StudyPackage> = emptyList(),
        actions: List<AssistantAction> = emptyList(),
    ) = AssistantChatItem.Message(
        AssistantMessage(
            id = id,
            sender = sender,
            text = text,
            sentAtEpochMillis = SENT_AT,
            voiceDurationSeconds = voiceDurationSeconds,
            packages = packages,
            actions = actions,
        ),
    )

    val conversation: List<AssistantChatItem> =
        listOf(
            AssistantChatItem.DayDivider(LocalDate.now()),
            message("1", AssistantSender.Assistant, "Halo Gilang! Aku asisten belajarmu. Mau latihan apa hari ini?"),
            message("2", AssistantSender.User, "Aku bingung mau latihan apa buat persiapan UTBK"),
            message(
                id = "3",
                sender = AssistantSender.Assistant,
                text = "Dari hasil terakhirmu, akurasi topik Aljabar masih 25%. Coba mulai dari latihan ini ya:",
                packages =
                    listOf(
                        practice(
                            "pkg_latihan_aljabar_dasar",
                            "Latihan Aljabar Dasar",
                            "20 soal untuk memperkuat konsep persamaan dan fungsi.",
                            20,
                        ),
                        practice(
                            "pkg_latihan_aljabar_linear",
                            "Latihan Aljabar Linear",
                            "15 soal matriks dan sistem persamaan linear.",
                            15,
                        ),
                    ),
            ),
            message("4", AssistantSender.User, "Ada tryout UTBK minggu ini?", voiceDurationSeconds = 4),
            message(
                id = "5",
                sender = AssistantSender.Assistant,
                text = "Ada! Simulasi UTBK-SNBT Session 1 tersedia sampai 3 hari lagi. Mau langsung mulai?",
                actions =
                    listOf(AssistantAction.StartTryout("pkg_tryout_utbk_sept_1"), AssistantAction.BrowseTryouts),
            ),
            AssistantChatItem.Typing,
        )

    /** A photo and a PDF, as picked into the composer. */
    val attachments: List<AssistantAttachment> =
        listOf(
            attachment("soal_aljabar.jpg", AttachmentType.Jpeg, PHOTO_BYTES),
            attachment("rangkuman_bab_3.pdf", AttachmentType.Pdf, PDF_BYTES),
        )

    private fun attachment(
        name: String,
        type: AttachmentType,
        sizeBytes: Long,
    ) = AssistantAttachment(uri = "content://preview/$name", name = name, type = type, sizeBytes = sizeBytes)

    private fun historyItem(
        id: String,
        title: String,
        daysAgo: Long,
        time: LocalTime,
        isOpen: Boolean = false,
    ) = HistoryItem(id, title, LocalDate.now().minusDays(daysAgo).atTime(time), isOpen)

    /** "Riwayat Percakapan" with "Persiapan UTBK minggu ini" open. */
    val history =
        ConversationHistoryUiState(
            isOpen = true,
            isLoading = false,
            hasHistory = true,
            groups =
                listOf(
                    HistoryGroup(
                        HistoryPeriod.Today,
                        listOf(
                            historyItem("1", "Persiapan UTBK minggu ini", 0, LocalTime.of(9, 12), isOpen = true),
                            historyItem("2", "Pembahasan soal logaritma", 0, LocalTime.of(7, 40)),
                        ),
                    ),
                    HistoryGroup(
                        HistoryPeriod.Yesterday,
                        listOf(historyItem("3", "Tips manajemen waktu tryout", 1, LocalTime.of(20, 15))),
                    ),
                    HistoryGroup(
                        HistoryPeriod.LastSevenDays,
                        listOf(
                            historyItem("4", "Rekomendasi latihan Aljabar", 2, LocalTime.of(16, 5)),
                            historyItem("5", "Arti skor IRT di tryout", 5, LocalTime.of(19, 30)),
                        ),
                    ),
                ),
        )
}
