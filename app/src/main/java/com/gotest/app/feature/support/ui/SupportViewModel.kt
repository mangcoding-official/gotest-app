package com.gotest.app.feature.support.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.support.domain.NewTicket
import com.gotest.app.feature.support.domain.SupportPage
import com.gotest.app.feature.support.domain.SupportRepository
import com.gotest.app.feature.support.domain.TicketStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TicketForm(
    val subject: String = "",
    /** A key of the page's ticket categories; empty until one is chosen. */
    val category: String = "",
    val message: String = "",
    /** Server messages by field: subject, category, message. */
    val errors: Map<String, String> = emptyMap(),
    val sending: Boolean = false,
    /** "Tiket terkirim. Tim Gotest membalas di halaman ini." after a ticket is sent. */
    val sent: String? = null,
)

@Immutable
data class SupportUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: SupportPage? = null,
    /** The open FAQ item; the first is open at the start, as on the web. */
    val openFaq: Int? = 0,
    val form: TicketForm = TicketForm(),
    /** Ticket cards that are open; a ticket that is not resolved starts open. */
    val openTickets: Set<Long> = emptySet(),
    val replies: Map<Long, String> = emptyMap(),
    val replyErrors: Map<Long, String> = emptyMap(),
    val replying: Set<Long> = emptySet(),
)

/** A write failed without a field to show it under (no connection, server error). */
data class SupportFailed(
    val message: String?,
)

/** Bantuan (web Support/Index): topics, FAQ, a new ticket, and the account's tickets with replies. */
@HiltViewModel
class SupportViewModel
    @Inject
    constructor(
        private val repository: SupportRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SupportUiState())
        val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

        private val _effects = Channel<SupportFailed>(Channel.BUFFERED)
        val effects: Flow<SupportFailed> = _effects.receiveAsFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { it.copy(isLoading = it.page == null, loadFailed = false) }
            viewModelScope.launch {
                repository
                    .page()
                    .onSuccess { page ->
                        _uiState.update { state ->
                            val known =
                                state.page
                                    ?.tickets
                                    ?.map { it.id }
                                    ?.toSet()
                            // New tickets open like the web's first render; tickets already shown keep their state.
                            val fresh = page.tickets.filter { known == null || it.id !in known }
                            val opened = fresh.filter { it.status != TicketStatus.Resolved }.map { it.id }
                            state.copy(isLoading = false, page = page, openTickets = state.openTickets + opened)
                        }
                    }.onFailure {
                        val shown = _uiState.value.page != null
                        _uiState.update { it.copy(isLoading = false, loadFailed = !shown) }
                        if (shown) _effects.send(SupportFailed(null))
                    }
            }
        }

        fun toggleFaq(index: Int) = _uiState.update { it.copy(openFaq = if (it.openFaq == index) null else index) }

        fun toggleTicket(id: Long) =
            _uiState.update {
                it.copy(
                    openTickets =
                        if (id in
                            it.openTickets
                        ) {
                            it.openTickets - id
                        } else {
                            it.openTickets + id
                        },
                )
            }

        fun onFormChange(form: TicketForm) = _uiState.update { it.copy(form = form) }

        fun sendTicket() {
            val form = _uiState.value.form
            if (form.sending) return
            _uiState.update { it.copy(form = form.copy(sending = true, errors = emptyMap(), sent = null)) }
            viewModelScope.launch {
                repository
                    .sendTicket(NewTicket(subject = form.subject, category = form.category, message = form.message))
                    .onSuccess { reply ->
                        _uiState.update { it.copy(form = TicketForm(sent = reply.message)) }
                        load()
                    }.onFailure { error ->
                        val fields = (error as? ApiError)?.fieldErrors?.mapValues { it.value.first() }.orEmpty()
                        _uiState.update { it.copy(form = it.form.copy(sending = false, errors = fields)) }
                        if (fields.isEmpty()) _effects.send(SupportFailed((error as? ApiError)?.message))
                    }
            }
        }

        fun onReplyChange(
            ticketId: Long,
            body: String,
        ) = _uiState.update { it.copy(replies = it.replies + (ticketId to body)) }

        fun sendReply(ticketId: Long) {
            val state = _uiState.value
            if (ticketId in state.replying) return
            val body = state.replies[ticketId].orEmpty()
            _uiState.update { it.copy(replying = it.replying + ticketId, replyErrors = it.replyErrors - ticketId) }
            viewModelScope.launch {
                repository
                    .reply(ticketId, body)
                    .onSuccess {
                        _uiState.update { it.copy(replying = it.replying - ticketId, replies = it.replies - ticketId) }
                        load()
                    }.onFailure { error ->
                        val field = (error as? ApiError)?.fieldError(BODY)
                        _uiState.update {
                            it.copy(
                                replying = it.replying - ticketId,
                                replyErrors =
                                    if (field !=
                                        null
                                    ) {
                                        it.replyErrors + (ticketId to field)
                                    } else {
                                        it.replyErrors
                                    },
                            )
                        }
                        if (field == null) _effects.send(SupportFailed((error as? ApiError)?.message))
                    }
            }
        }

        private companion object {
            const val BODY = "body"
        }
    }
