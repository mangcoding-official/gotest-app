package com.gotest.app.feature.auth.ui.register

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.feature.auth.domain.GuardianQuestion
import com.gotest.app.feature.auth.domain.JoinNotice
import com.gotest.app.feature.auth.ui.components.GuardianFormState
import com.gotest.app.feature.auth.ui.components.asksFor

/** "Cek email kamu": the account is made after the code sent to the email (web: EmailCodeForm). */
@Immutable
data class RegisterCodeStep(
    val sentTo: String,
    val code: String = "",
    val codeMessage: String? = null,
    val resendInSeconds: Int = 0,
)

@Immutable
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val level: EducationLevel? = null,
    val guardian: GuardianFormState = GuardianFormState(),
    val guardianQuestion: GuardianQuestion? = null,
    val emailCode: Boolean = false,
    val joinNotice: JoinNotice? = null,
    @param:StringRes val fullNameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmationError: Int? = null,
    @param:StringRes val levelError: Int? = null,
    /** The server's own texts by the web's field names: name, email, password, education_level, form. */
    val messages: Map<String, String> = emptyMap(),
    val codeStep: RegisterCodeStep? = null,
    val isSubmitting: Boolean = false,
) {
    val asksGuardian: Boolean get() = guardianQuestion.asksFor(level)
}

sealed interface RegisterUiEvent {
    data class FullNameChanged(
        val value: String,
    ) : RegisterUiEvent

    data class EmailChanged(
        val value: String,
    ) : RegisterUiEvent

    data class PasswordChanged(
        val value: String,
    ) : RegisterUiEvent

    data class ConfirmationChanged(
        val value: String,
    ) : RegisterUiEvent

    data class LevelPicked(
        val level: EducationLevel,
    ) : RegisterUiEvent

    data class GuardianChanged(
        val state: GuardianFormState,
    ) : RegisterUiEvent

    data class CodeChanged(
        val value: String,
    ) : RegisterUiEvent

    data object Submit : RegisterUiEvent

    data object ConfirmCode : RegisterUiEvent

    data object ResendCode : RegisterUiEvent

    /** "Ganti email": back to the form, filled in as it was sent. */
    data object ChangeEmail : RegisterUiEvent

    /** "Bukan, daftar biasa": sign up without the Mitra code. */
    data object DropJoinCode : RegisterUiEvent

    data object GoogleSignUp : RegisterUiEvent
}

sealed interface RegisterEffect {
    data object Registered : RegisterEffect

    /** An under-age account waits for the parent's approval (Aturan 2.15). */
    data object AwaitingConsent : RegisterEffect
}
