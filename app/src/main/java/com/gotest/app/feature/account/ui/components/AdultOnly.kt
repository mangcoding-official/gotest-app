package com.gotest.app.feature.account.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** What is known about the signed-in account for the screens a child account may not open. */
enum class AccountKind { Loading, Child, Adult, Failed }

/** Whether the signed-in account is a child account, for the screens a child may not open (Aturan 11.30). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountAccessViewModel
    @Inject
    constructor(
        userRepository: UserRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)

        /**
         * A profile that fails to load is [AccountKind.Failed], never read as an adult account, so a child
         * account cannot reach these screens through a dropped connection (T-026 required 14).
         */
        val kind: StateFlow<AccountKind> =
            retryKey
                .flatMapLatest {
                    userRepository
                        .observeProfile()
                        .map { if (it.isChild) AccountKind.Child else AccountKind.Adult }
                        .onStart { emit(AccountKind.Loading) }
                        .catch { emit(AccountKind.Failed) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AccountKind.Loading)

        fun retry() = retryKey.update { it + 1 }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/**
 * Shows [content] only to an account known not to be a child account. A child account that reaches one of
 * these screens (edit account, password, number, Sesi Aktif, delete) is sent back (T-018 review 2); while the
 * profile cannot be read, the screen shows the error with "Coba lagi" and decides once it is read.
 */
@Composable
fun AdultOnly(
    onChildAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val kind by viewModel.kind.collectAsStateWithLifecycle()
    val currentOnChildAccount by rememberUpdatedState(onChildAccount)
    when (kind) {
        AccountKind.Loading -> GotestLoadingView(modifier = modifier)
        AccountKind.Child -> LaunchedEffect(Unit) { currentOnChildAccount() }
        AccountKind.Adult -> content()
        AccountKind.Failed -> GotestErrorView(onRetry = viewModel::retry, modifier = modifier)
    }
}
