package com.gotest.app.feature.account.ui.personaldata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.account.domain.AccountDataRepository
import com.gotest.app.feature.account.domain.PersonalDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Name and email follow the profile, so edits made through "Ubah" show up on return. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonalDataViewModel
    @Inject
    constructor(
        userRepository: UserRepository,
        accountDataRepository: AccountDataRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)

        val uiState: StateFlow<PersonalDataUiState> =
            retryKey
                .flatMapLatest {
                    combine<UserProfile, PersonalDetails, PersonalDataUiState>(
                        userRepository.observeProfile(),
                        accountDataRepository.observePersonalDetails(),
                    ) { profile, details ->
                        PersonalDataUiState.Success(
                            fullName = profile.fullName,
                            email = profile.email,
                            details = details,
                        )
                    }.onStart { emit(PersonalDataUiState.Loading) }
                        .catch { emit(PersonalDataUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PersonalDataUiState.Loading)

        fun onEvent(event: PersonalDataUiEvent) {
            when (event) {
                PersonalDataUiEvent.Retry -> retryKey.update { it + 1 }
            }
        }
    }
