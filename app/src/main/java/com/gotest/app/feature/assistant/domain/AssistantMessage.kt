package com.gotest.app.feature.assistant.domain

import com.gotest.app.core.model.StudyPackage

enum class AssistantSender { Assistant, User }

/** Shortcut the assistant attaches to an answer; the app decides where each one leads. */
sealed interface AssistantAction {
    /** "Mulai tryout": opens the tryout detail. */
    data class StartTryout(
        val packageId: String,
    ) : AssistantAction

    /** "Lihat semua tryout": the Paket tab. */
    data object BrowseTryouts : AssistantAction

    /** "Lihat peringkat": the Peringkat tab. */
    data object OpenLeaderboard : AssistantAction
}

data class AssistantMessage(
    val id: String,
    val sender: AssistantSender,
    val text: String,
    val sentAtEpochMillis: Long,
    /** Length of the recording when the student spoke the message; null for typed messages. */
    val voiceDurationSeconds: Int? = null,
    /** Packages the assistant recommends below its answer. */
    val packages: List<StudyPackage> = emptyList(),
    /** Quick actions shown as chips below the answer. */
    val actions: List<AssistantAction> = emptyList(),
)
