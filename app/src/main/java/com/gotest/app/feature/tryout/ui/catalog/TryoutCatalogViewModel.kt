package com.gotest.app.feature.tryout.ui.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.practice.domain.StartAccess
import com.gotest.app.feature.practice.ui.catalog.DateFilter
import com.gotest.app.feature.practice.ui.catalog.matchesDate
import com.gotest.app.feature.ruang.data.parseServerTime
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.TryoutCatalogPage
import com.gotest.app.feature.tryout.domain.TryoutPackage
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutSession
import com.gotest.app.feature.tryout.domain.TryoutWindow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A refusal, as the web words it: the server's sentence, no connection, or the fallback. */
sealed interface TryoutProblem {
    data class Server(
        val message: String,
    ) : TryoutProblem

    data object Offline : TryoutProblem

    data object Fallback : TryoutProblem
}

/** The start sheet of one package (web TryoutStartModal). */
@Immutable
data class TryoutSheet(
    val item: TryoutPackage,
    val isStarting: Boolean = false,
    val error: TryoutProblem? = null,
)

/** The sheet of one session (web SessionDetail), with the session ranking once it ended. */
@Immutable
data class SessionSheet(
    val sessionId: Long,
    val busy: Boolean = false,
    val error: TryoutProblem? = null,
    /** The server's sentence after Daftar or Batalkan (web flash). */
    val message: String? = null,
    val leaderboard: SessionLeaderboard? = null,
)

@Immutable
data class TryoutCatalogUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: TryoutCatalogPage? = null,
    val access: StartAccess? = null,
    val categoryId: Long? = null,
    val draftActivityCategory: String? = null,
    val draftDateFilter: DateFilter = DateFilter.All,
    val activityCategory: String? = null,
    val dateFilter: DateFilter = DateFilter.All,
    val sheet: TryoutSheet? = null,
    val sessionSheet: SessionSheet? = null,
    /** Seconds to the end of the running session, or the start of the next one (Seasonal Try Out). */
    val countdown: Long? = null,
) {
    /** Only categories with a Try Out show. */
    val categories: List<Pair<com.gotest.app.feature.practice.domain.CatalogCategory, List<TryoutPackage>>>
        get() =
            page
                ?.categories
                ?.map { category -> category to page.packages.filter { it.categoryId == category.id } }
                ?.filter { it.second.isNotEmpty() }
                .orEmpty()

    /** Running first, then coming, then finished (web SeasonalTryout). */
    val sessions: List<TryoutSession>
        get() =
            page?.sessions.orEmpty().sortedWith(
                compareBy<TryoutSession> { STATE_ORDER[it.state] ?: STATE_ORDER.size }
                    .thenBy { if (it.state == TryoutSession.RUNNING) it.endsAt else it.startsAt },
            )

    val countdownTarget: TryoutSession?
        get() =
            sessions.firstOrNull { it.state == TryoutSession.RUNNING }
                ?: sessions.firstOrNull { it.state == TryoutSession.SCHEDULED }

    val openSession: TryoutSession? get() = page?.sessions?.firstOrNull { it.id == sessionSheet?.sessionId }

    val activity: List<RecentAttempt>
        get() =
            page?.recentAttempts.orEmpty().filter { attempt ->
                (activityCategory == null || attempt.categoryName == activityCategory) &&
                    matchesDate(attempt.submittedAt, dateFilter)
            }

    private companion object {
        val STATE_ORDER = mapOf(TryoutSession.RUNNING to 0, TryoutSession.SCHEDULED to 1, TryoutSession.CLOSED to 2)
    }
}

sealed interface TryoutCatalogEffect {
    data class OpenRunner(
        val attemptId: Long,
        val packageName: String,
    ) : TryoutCatalogEffect

    data class OpenResult(
        val attemptId: Long,
    ) : TryoutCatalogEffect
}

/**
 * Paket Try Out (web Tryout/Index, TryoutCatalog): categories and their packages, Seasonal Try Out with its
 * countdown on the server clock, and Aktivitas Terkini. The start and session sheets work on every level.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class TryoutCatalogViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TryoutRepository,
        private val accounts: PracticeRepository,
    ) : ViewModel() {
        private val categoryId: Long? = savedStateHandle[ARG_CATEGORY]

        private val _uiState = MutableStateFlow(TryoutCatalogUiState(categoryId = categoryId))
        val uiState: StateFlow<TryoutCatalogUiState> = _uiState.asStateFlow()

        private val _effects = Channel<TryoutCatalogEffect>(Channel.BUFFERED)
        val effects: Flow<TryoutCatalogEffect> = _effects.receiveAsFlow()

        private var request: Job? = null
        private var ticker: Job? = null

        init {
            load()
        }

        fun retry() = load()

        fun refresh() {
            if (_uiState.value.page != null) fetch()
        }

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            fetch()
        }

        private fun fetch() {
            request?.cancel()
            request =
                viewModelScope.launch {
                    val access = async { accounts.startAccess() }
                    val requestedAt = System.currentTimeMillis()
                    repository
                        .catalog()
                        .onSuccess { page ->
                            val known = access.await().getOrNull()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadFailed = false,
                                    page = page,
                                    access =
                                        known ?: it.access,
                                )
                            }
                            startCountdown(page, requestedAt)
                        }.onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = it.page == null) } }
                }
        }

        /** Counts on the server clock and reloads at zero, so states and buttons follow (web SM-20). */
        private fun startCountdown(
            page: TryoutCatalogPage,
            requestedAt: Long,
        ) {
            ticker?.cancel()
            val target = _uiState.value.countdownTarget ?: return _uiState.update { it.copy(countdown = null) }
            val at = if (target.state == TryoutSession.RUNNING) target.endsAt else target.startsAt
            val offset = page.serverNow?.let { parseServerTime(it).toEpochMilli() - requestedAt } ?: 0L
            val end = parseServerTime(at).toEpochMilli()
            ticker =
                viewModelScope.launch {
                    var left = secondsTo(end, offset)
                    while (isActive && left > 0) {
                        _uiState.update { it.copy(countdown = left) }
                        delay(SECOND)
                        left = secondsTo(end, offset)
                    }
                    _uiState.update { it.copy(countdown = 0) }
                    fetch()
                }
        }

        private fun secondsTo(
            end: Long,
            offset: Long,
        ): Long = ((end - (System.currentTimeMillis() + offset)) / SECOND).coerceAtLeast(0)

        fun onDraftActivityCategory(name: String?) = _uiState.update { it.copy(draftActivityCategory = name) }

        fun onDraftDateFilter(filter: DateFilter) = _uiState.update { it.copy(draftDateFilter = filter) }

        fun applyActivityFilter() =
            _uiState.update {
                it.copy(activityCategory = it.draftActivityCategory, dateFilter = it.draftDateFilter)
            }

        fun openSheet(item: TryoutPackage) = _uiState.update { it.copy(sheet = TryoutSheet(item)) }

        fun closeSheet() = _uiState.update { if (it.sheet?.isStarting == true) it else it.copy(sheet = null) }

        /** Lihat Hasil, Lanjutkan, or a new start (web TryoutStartModal `start`). */
        fun start() {
            val sheet = _uiState.value.sheet?.takeUnless { it.isStarting } ?: return
            val item = sheet.item
            val resultId = item.resultOnlyId
            val continueId = item.continueAttemptId
            when {
                resultId != null -> {
                    _uiState.update { it.copy(sheet = null) }
                    _effects.trySend(TryoutCatalogEffect.OpenResult(resultId))
                }

                continueId != null -> {
                    openRunner(continueId, item.name)
                }

                else -> {
                    startNew(sheet)
                }
            }
        }

        private fun startNew(sheet: TryoutSheet) {
            _uiState.update { it.copy(sheet = sheet.copy(isStarting = true, error = null)) }
            viewModelScope.launch {
                repository
                    .start(sheet.item.id)
                    .onSuccess { attempt -> openRunner(attempt.id, sheet.item.name) }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                sheet = it.sheet?.copy(isStarting = false, error = problemOf(error)),
                            )
                        }
                    }
            }
        }

        private fun openRunner(
            attemptId: Long,
            name: String,
        ) {
            _uiState.update { it.copy(sheet = null, sessionSheet = null) }
            _effects.trySend(TryoutCatalogEffect.OpenRunner(attemptId, name))
        }

        fun openSession(session: TryoutSession) {
            _uiState.update { it.copy(sessionSheet = SessionSheet(session.id)) }
            if (session.state == TryoutSession.CLOSED) loadLeaderboard(session.id)
        }

        fun closeSession() = _uiState.update { if (it.sessionSheet?.busy == true) it else it.copy(sessionSheet = null) }

        private fun loadLeaderboard(sessionId: Long) {
            viewModelScope.launch {
                repository.sessionLeaderboard(sessionId).onSuccess { board ->
                    _uiState.update { state ->
                        state.copy(
                            sessionSheet =
                                state.sessionSheet
                                    ?.takeIf {
                                        it.sessionId ==
                                            sessionId
                                    }?.copy(leaderboard = board),
                        )
                    }
                }
            }
        }

        fun register() = sessionWrite { repository.register(it) }

        fun unregister() = sessionWrite { repository.unregister(it) }

        /** Daftar or Batalkan, then the sessions again with the server's sentence (web reloads props). */
        private fun sessionWrite(write: suspend (Long) -> Result<String?>) {
            val sheet = _uiState.value.sessionSheet?.takeUnless { it.busy } ?: return
            _uiState.update { it.copy(sessionSheet = sheet.copy(busy = true, error = null, message = null)) }
            viewModelScope.launch {
                write(sheet.sessionId)
                    .onSuccess { message ->
                        _uiState.update {
                            it.copy(
                                sessionSheet = it.sessionSheet?.copy(busy = false, message = message),
                            )
                        }
                        fetch()
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                sessionSheet = it.sessionSheet?.copy(busy = false, error = problemOf(error)),
                            )
                        }
                    }
            }
        }

        /** Mulai Sesi or Lanjutkan Sesi; Lihat Hasil opens the session's result. */
        fun startSession() {
            val state = _uiState.value
            val session = state.openSession
            val sheet = state.sessionSheet?.takeUnless { it.busy }
            if (session == null || sheet == null) return
            val done = session.attemptId?.takeIf { session.attemptDone }
            if (done != null) {
                _uiState.update { it.copy(sessionSheet = null) }
                _effects.trySend(TryoutCatalogEffect.OpenResult(done))
            } else {
                _uiState.update { it.copy(sessionSheet = sheet.copy(busy = true, error = null)) }
                viewModelScope.launch {
                    repository
                        .startSession(session.id)
                        .onSuccess { attemptId -> openRunner(attemptId, session.packageName) }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    sessionSheet = it.sessionSheet?.copy(busy = false, error = sessionProblem(error)),
                                )
                            }
                        }
                }
            }
        }

        companion object {
            const val ARG_CATEGORY = "categoryId"
            private const val SECOND = 1_000L
            private const val UNPROCESSABLE = 422

            /** Web `requestMessage`: a 422 says why, no connection says so, anything else the fallback. */
            fun problemOf(error: Throwable): TryoutProblem =
                when {
                    error !is ApiError -> {
                        TryoutProblem.Offline
                    }

                    error.status == UNPROCESSABLE -> {
                        TryoutProblem.Server(
                            error.fieldErrors.values
                                .firstOrNull()
                                ?.firstOrNull() ?: error.message,
                        )
                    }

                    else -> {
                        TryoutProblem.Fallback
                    }
                }

            /** Web SessionDetail `start`: the server's message for any refusal, otherwise the fallback. */
            fun sessionProblem(error: Throwable): TryoutProblem =
                if (error is ApiError &&
                    error.message.isNotBlank()
                ) {
                    TryoutProblem.Server(error.message)
                } else {
                    TryoutProblem.Fallback
                }
        }
    }

/** A package's window in words (web `windowNotice`); the screen formats the times. */
fun windowBlocked(item: TryoutPackage): Boolean =
    item.continueAttemptId == null && item.resultOnlyId == null &&
        (item.windowState == TryoutWindow.STATE_UPCOMING || item.windowState == TryoutWindow.STATE_CLOSED)
