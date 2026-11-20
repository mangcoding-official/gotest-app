package com.gotest.app.feature.tryout.ui.session

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.StartAccess
import com.gotest.app.feature.ruang.ui.components.RuangScaffold
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutSession
import com.gotest.app.feature.tryout.ui.catalog.SessionCallbacks
import com.gotest.app.feature.tryout.ui.catalog.SessionDetailContent
import com.gotest.app.feature.tryout.ui.catalog.SessionSheet
import com.gotest.app.feature.tryout.ui.catalog.TryoutCatalogViewModel
import com.gotest.app.feature.tryout.ui.catalog.tryoutAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
data class TryoutSessionUiState(
    val isLoading: Boolean = true,
    val session: TryoutSession? = null,
    val access: StartAccess? = null,
    val sheet: SessionSheet? = null,
)

sealed interface TryoutSessionEffect {
    data class OpenRunner(
        val attemptId: Long,
        val packageName: String,
    ) : TryoutSessionEffect

    data class OpenResult(
        val attemptId: Long,
    ) : TryoutSessionEffect
}

/** One session on its own page (web Tryout/Session), with its ranking once it ended. */
@HiltViewModel
class TryoutSessionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TryoutRepository,
        private val accounts: PracticeRepository,
    ) : ViewModel() {
        private val sessionId: Long = checkNotNull(savedStateHandle[ARG_SESSION])

        private val _uiState = MutableStateFlow(TryoutSessionUiState())
        val uiState: StateFlow<TryoutSessionUiState> = _uiState.asStateFlow()

        private val _effects = Channel<TryoutSessionEffect>(Channel.BUFFERED)
        val effects: Flow<TryoutSessionEffect> = _effects.receiveAsFlow()

        init {
            load()
        }

        fun load(message: String? = null) {
            viewModelScope.launch {
                val access = async { accounts.startAccess() }
                val page = repository.session(sessionId).getOrNull()
                val board =
                    page
                        ?.session
                        ?.takeIf {
                            it.state == TryoutSession.CLOSED
                        }?.let { repository.sessionLeaderboard(sessionId).getOrNull() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = page?.session,
                        access = access.await().getOrNull() ?: it.access,
                        sheet = SessionSheet(sessionId, message = message, leaderboard = board),
                    )
                }
            }
        }

        fun register() = write { repository.register(sessionId) }

        fun unregister() = write { repository.unregister(sessionId) }

        private fun write(call: suspend () -> Result<String?>) {
            val sheet = _uiState.value.sheet?.takeUnless { it.busy } ?: return
            _uiState.update { it.copy(sheet = sheet.copy(busy = true, error = null)) }
            viewModelScope.launch {
                call()
                    .onSuccess { message -> load(message) }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                sheet = it.sheet?.copy(busy = false, error = TryoutCatalogViewModel.problemOf(error)),
                            )
                        }
                    }
            }
        }

        fun start() {
            val session = _uiState.value.session ?: return
            val done = session.attemptId?.takeIf { session.attemptDone }
            if (done != null) {
                _effects.trySend(TryoutSessionEffect.OpenResult(done))
                return
            }
            _uiState.update { it.copy(sheet = it.sheet?.copy(busy = true, error = null)) }
            viewModelScope.launch {
                repository
                    .startSession(sessionId)
                    .onSuccess { attemptId ->
                        _effects.send(TryoutSessionEffect.OpenRunner(attemptId, session.packageName))
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                sheet =
                                    it.sheet?.copy(
                                        busy = false,
                                        error = TryoutCatalogViewModel.sessionProblem(error),
                                    ),
                            )
                        }
                    }
                _uiState.update { it.copy(sheet = it.sheet?.copy(busy = false)) }
            }
        }

        companion object {
            const val ARG_SESSION = "sessionId"
        }
    }

@Composable
fun TryoutSessionRoute(
    onBack: () -> Unit,
    onOpenRunner: (attemptId: Long, packageName: String) -> Unit,
    onOpenResult: (attemptId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TryoutSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is TryoutSessionEffect.OpenRunner -> onOpenRunner(effect.attemptId, effect.packageName)
            is TryoutSessionEffect.OpenResult -> onOpenResult(effect.attemptId)
        }
    }
    val session = uiState.session
    val sheet = uiState.sheet
    RuangScaffold(
        title = session?.title ?: stringResource(R.string.trun_kicker),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            session == null || sheet == null -> {
                GotestErrorView(onRetry = { viewModel.load() }, modifier = Modifier.fillMaxSize().padding(padding))
            }

            else -> {
                SessionDetailContent(
                    session = session,
                    sheet = sheet,
                    access = tryoutAccess(uiState.access, session.premiumLocked),
                    actions = SessionCallbacks(viewModel::register, viewModel::unregister, viewModel::start, onBack),
                    modifier =
                        Modifier
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = GotestTheme.spacing.screenHorizontal,
                                vertical = GotestTheme.spacing.lg,
                            ),
                )
            }
        }
    }
}
