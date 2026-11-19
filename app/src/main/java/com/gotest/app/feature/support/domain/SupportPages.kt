package com.gotest.app.feature.support.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Bantuan of API module F (T-033): the same props as the web's Support/Index and Support/Topic,
 * built by StudentSupport. The topic cards, FAQ and contacts are written in the web's page code, so
 * the app keeps its own copy of their texts (strings_support.xml).
 */

@Serializable
enum class TicketStatus {
    @SerialName("open")
    Open,

    @SerialName("in_progress")
    InProgress,

    @SerialName("resolved")
    Resolved,
}

@Serializable
data class TicketReply(
    @SerialName("id") val id: Long,
    @SerialName("fromTeam") val fromTeam: Boolean = false,
    /** "Tim Gotest", the account's name, or "Kamu". */
    @SerialName("author") val author: String,
    @SerialName("body") val body: String,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class SupportTicket(
    @SerialName("id") val id: Long,
    @SerialName("subject") val subject: String,
    /** The label, not the key. */
    @SerialName("category") val category: String,
    @SerialName("message") val message: String,
    @SerialName("status") val status: TicketStatus = TicketStatus.Open,
    @SerialName("statusLabel") val statusLabel: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("replies") val replies: List<TicketReply> = emptyList(),
)

/** `GET /bantuan`: the account's 20 latest tickets and the kinds of problem, key to label, in order. */
@Serializable
data class SupportPage(
    @SerialName("tickets") val tickets: List<SupportTicket> = emptyList(),
    @SerialName("ticketCategories") val ticketCategories: Map<String, String> = emptyMap(),
)

@Serializable
data class SupportSection(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
)

@Serializable
data class SupportTopic(
    @SerialName("slug") val slug: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("sections") val sections: List<SupportSection> = emptyList(),
)

@Serializable
data class SupportTopicPage(
    @SerialName("topic") val topic: SupportTopic,
)

@Serializable
data class NewTicket(
    @SerialName("subject") val subject: String,
    /** A key of [SupportPage.ticketCategories]. */
    @SerialName("category") val category: String,
    @SerialName("message") val message: String,
)

@Serializable
data class TicketReplyRequest(
    @SerialName("body") val body: String,
)

/** Write replies: `message` is set for a new ticket ("Tiket terkirim. ..."), null for a reply. */
@Serializable
data class SupportWriteReply(
    @SerialName("message") val message: String? = null,
    @SerialName("id") val id: Long? = null,
)

interface SupportRepository {
    suspend fun page(): Result<SupportPage>

    suspend fun topic(slug: String): Result<SupportTopic>

    suspend fun sendTicket(ticket: NewTicket): Result<SupportWriteReply>

    suspend fun reply(
        ticketId: Long,
        body: String,
    ): Result<SupportWriteReply>
}
