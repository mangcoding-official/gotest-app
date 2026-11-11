package com.gotest.app.feature.assistant.domain

/** The open conversation. A new one has no messages and shows the welcome layout. */
data class AssistantConversation(
    val id: String,
    /** Oldest message first. */
    val messages: List<AssistantMessage>,
    /** True while the assistant prepares an answer in this conversation. */
    val isAwaitingReply: Boolean = false,
)

/** One row of "Riwayat Percakapan". */
data class ConversationSummary(
    val id: String,
    val title: String,
    val lastMessageAtEpochMillis: Long,
    /** The conversation on screen ("Sedang dibuka"). */
    val isOpen: Boolean,
)
