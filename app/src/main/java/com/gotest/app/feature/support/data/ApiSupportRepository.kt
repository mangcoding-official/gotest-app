package com.gotest.app.feature.support.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.support.domain.NewTicket
import com.gotest.app.feature.support.domain.SupportPage
import com.gotest.app.feature.support.domain.SupportRepository
import com.gotest.app.feature.support.domain.SupportTopic
import com.gotest.app.feature.support.domain.SupportTopicPage
import com.gotest.app.feature.support.domain.SupportWriteReply
import com.gotest.app.feature.support.domain.TicketReplyRequest
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton

/** Bantuan routes of API module F. */
interface SupportApi {
    @GET("api/v1/bantuan")
    suspend fun page(): SupportPage

    @GET("api/v1/bantuan/{slug}")
    suspend fun topic(
        @Path("slug") slug: String,
    ): SupportTopicPage

    @POST("api/v1/bantuan/tiket")
    suspend fun sendTicket(
        @Body ticket: NewTicket,
    ): SupportWriteReply

    @POST("api/v1/bantuan/tiket/{ticket}/balas")
    suspend fun reply(
        @Path("ticket") ticketId: Long,
        @Body body: TicketReplyRequest,
    ): SupportWriteReply
}

@Singleton
class ApiSupportRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : SupportRepository {
        private val api = retrofit.create(SupportApi::class.java)

        override suspend fun page(): Result<SupportPage> = apiCall(json) { api.page() }

        override suspend fun topic(slug: String): Result<SupportTopic> = apiCall(json) { api.topic(slug).topic }

        override suspend fun sendTicket(ticket: NewTicket): Result<SupportWriteReply> =
            apiCall(json) { api.sendTicket(ticket) }

        override suspend fun reply(
            ticketId: Long,
            body: String,
        ): Result<SupportWriteReply> = apiCall(json) { api.reply(ticketId, TicketReplyRequest(body)) }
    }
