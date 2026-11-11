package com.gotest.app.feature.assistant.ui.assistant

import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantSender
import java.time.LocalDate

/** The conversation from frame 11990, for previews. */
internal object AssistantPreviewData {
    private const val SENT_AT = 0L
    private const val DURATION_MINUTES = 45

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
}
