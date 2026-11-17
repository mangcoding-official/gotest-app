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
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Whether the signed-in account is a child account, for the screens a child may not open (Aturan 11.30). */
@HiltViewModel
class AccountAccessViewModel
    @Inject
    constructor(
        userRepository: UserRepository,
    ) : ViewModel() {
        /** Null until the profile is known; a profile that fails to load leaves the decision to the screen. */
        val isChild: StateFlow<Boolean?> =
            userRepository
                .observeProfile()
                .map<_, Boolean?> { it.isChild }
                .catch { emit(false) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/**
 * Shows [content] only to an account that is not a child account. A child account that reaches one of
 * these screens (edit account, password, number, Sesi Aktif, delete) is sent back (T-018 review 2).
 */
@Composable
fun AdultOnly(
    onChildAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val isChild by viewModel.isChild.collectAsStateWithLifecycle()
    val currentOnChildAccount by rememberUpdatedState(onChildAccount)
    when (isChild) {
        null -> GotestLoadingView(modifier = modifier)
        true -> LaunchedEffect(Unit) { currentOnChildAccount() }
        false -> content()
    }
}
