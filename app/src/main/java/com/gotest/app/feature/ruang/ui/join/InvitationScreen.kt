package com.gotest.app.feature.ruang.ui.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.domain.EmailProof
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.ui.components.RuangScaffold

data class InvitationActions(
    val onRetry: () -> Unit,
    val onSendCode: () -> Unit,
    val onCodeChange: (String) -> Unit,
    val onCancelCode: () -> Unit,
    val onAccept: () -> Unit,
    val onSignIn: () -> Unit,
    val onRegister: () -> Unit,
    val onSignOut: () -> Unit,
)

@Composable
fun InvitationRoute(
    onBack: () -> Unit,
    onJoinSuccess: (JoinedMitra) -> Unit,
    onSignOutSuccess: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var staffJoin by remember { mutableStateOf<JoinedMitra?>(null) }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is InvitationEffect.Joined -> {
                if (effect.joined.role == STUDENT) onJoinSuccess(effect.joined) else staffJoin = effect.joined
            }

            InvitationEffect.SignedOut -> {
                onSignOutSuccess()
            }
        }
    }
    staffJoin?.let { joined ->
        StaffJoinedDialog(mitraName = uiState.page?.organizationName.orEmpty(), onClose = { onJoinSuccess(joined) })
    }
    InvitationScreen(
        uiState = uiState,
        actions =
            InvitationActions(
                onRetry = viewModel::load,
                onSendCode = viewModel::sendCode,
                onCodeChange = viewModel::onCodeChange,
                onCancelCode = viewModel::cancelCode,
                onAccept = viewModel::accept,
                onSignIn = onSignIn,
                onRegister = onRegister,
                onSignOut = viewModel::signOut,
            ),
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun InvitationScreen(
    uiState: InvitationUiState,
    actions: InvitationActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    val heading =
        page?.organizationName?.let { stringResource(R.string.invite_heading, it) }
            ?: stringResource(R.string.invite_heading_plain)
    RuangScaffold(title = heading, onBack = onBack, modifier = modifier) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.fillMaxSize().padding(padding))
            }

            page == null -> {
                GotestErrorView(
                    onRetry = actions.onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            else -> {
                JoinPage(lede = invitationLede(page), padding = padding) { InvitationCard(page, uiState, actions) }
            }
        }
    }
}

/** "pengelola", "pengajar" (Keluarga: "orang tua dan tutor"), or "siswa" (Keluarga: "anak"). */
@Composable
private fun roleLabel(page: InvitationPage): String {
    val family = page.organizationType == FAMILY
    return stringResource(
        when (page.role) {
            "pengelola" -> R.string.invite_role_manager
            "pengajar" -> if (family) R.string.invite_role_teacher_family else R.string.invite_role_teacher
            else -> if (family) R.string.invite_role_student_family else R.string.invite_role_student
        },
    )
}

@Composable
private fun invitationLede(page: InvitationPage): String =
    when (page.role) {
        "pengelola" -> {
            stringResource(
                R.string.invite_lede_manager,
                stringResource(
                    if (page.organizationType ==
                        FAMILY
                    ) {
                        R.string.invite_role_student_family
                    } else {
                        R.string.invite_role_student
                    },
                ),
            )
        }

        "pengajar" -> {
            stringResource(
                if (page.organizationType ==
                    FAMILY
                ) {
                    R.string.invite_lede_teacher_family
                } else {
                    R.string.invite_lede_teacher
                },
            )
        }

        else -> {
            stringResource(R.string.invite_lede_student)
        }
    }

@Composable
private fun InvitationCard(
    page: InvitationPage,
    uiState: InvitationUiState,
    actions: InvitationActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val mitra = page.organizationName.orEmpty()
        val contact = page.email.orEmpty()
        when (page.state) {
            "invalid" -> {
                CardHeading(stringResource(R.string.invite_invalid_title), stringResource(R.string.invite_invalid))
            }

            "used" -> {
                CardHeading(stringResource(R.string.invite_used_title), stringResource(R.string.invite_used, mitra))
            }

            "expired" -> {
                CardHeading(
                    stringResource(R.string.invite_expired_title),
                    stringResource(R.string.invite_expired, mitra),
                )
            }

            "guest" -> {
                CardHeading(
                    stringResource(R.string.invite_join_as, roleLabel(page), mitra),
                    stringResource(
                        if (page.byPhone) R.string.invite_guest_phone else R.string.invite_guest_email,
                        contact,
                    ),
                )
                GotestButton(
                    text = stringResource(R.string.join_sign_in),
                    onClick = actions.onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
                GotestButton(
                    text = stringResource(R.string.join_register),
                    onClick = actions.onRegister,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                )
            }

            "other_account" -> {
                CardHeading(
                    stringResource(R.string.invite_other_title),
                    stringResource(
                        if (page.byPhone) R.string.invite_other_phone else R.string.invite_other_email,
                        contact,
                    ),
                )
                GotestButton(
                    text = stringResource(R.string.invite_sign_out),
                    onClick = actions.onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Plain,
                )
            }

            else -> {
                ReadyInvitation(page = page, uiState = uiState, actions = actions)
            }
        }
    }
}

@Composable
private fun ReadyInvitation(
    page: InvitationPage,
    uiState: InvitationUiState,
    actions: InvitationActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        val title = stringResource(R.string.invite_join_as, roleLabel(page), page.organizationName.orEmpty())
        val contact = page.email.orEmpty()
        if (page.emailProof == null) {
            CardHeading(title, stringResource(R.string.invite_with_account, contact))
            ActionErrorText(uiState.error)
            SubmitButton(
                text = stringResource(R.string.invite_accept),
                busy = uiState.isBusy,
                onClick = actions.onAccept,
            )
            return
        }
        CardHeading(title, stringResource(R.string.invite_proof, contact))
        if (!uiState.codeStep) {
            ActionErrorText(uiState.error)
            SubmitButton(
                text = stringResource(R.string.invite_send_code),
                busy = uiState.isBusy,
                onClick = actions.onSendCode,
            )
            return
        }
        // The email code form of F-c (web: Components/Auth/EmailCodeForm.tsx).
        CardHeading(stringResource(R.string.register_code_sent, uiState.sentTo.orEmpty()), null)
        GotestTextField(
            value = uiState.code,
            onValueChange = actions.onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.register_code_label),
            placeholder = stringResource(R.string.code_hint),
            isError = uiState.error != null,
            supportingText = uiState.error?.let { it.message ?: stringResource(R.string.state_error_message) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        GotestButton(
            text =
                if (uiState.resendIn > 0) {
                    stringResource(R.string.code_resend_in, uiState.resendIn)
                } else {
                    stringResource(R.string.code_resend)
                },
            onClick = actions.onSendCode,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Text,
            enabled = uiState.resendIn == 0 && !uiState.isBusy,
        )
        SubmitButton(text = stringResource(R.string.invite_accept), busy = uiState.isBusy, onClick = actions.onAccept)
        GotestButton(
            text = stringResource(R.string.invite_cancel),
            onClick = actions.onCancelCode,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

private const val FAMILY = "keluarga"

private val previewActions = InvitationActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun InvitationScreenReadyPreview() {
    GotestTheme {
        InvitationScreen(
            uiState =
                InvitationUiState(
                    isLoading = false,
                    page = InvitationPage("t", "ready", "SMA 3 Bandung", "gilangpermana@mail.com", role = "siswa"),
                ),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvitationScreenCodePreview() {
    GotestTheme {
        InvitationScreen(
            uiState =
                InvitationUiState(
                    isLoading = false,
                    page =
                        InvitationPage(
                            "t",
                            "ready",
                            "Bimbel Juara",
                            "gilangpermana@mail.com",
                            role = "siswa",
                            emailProof = EmailProof("gi*************@mail.com"),
                        ),
                    codeStep = true,
                    sentTo = "gi*************@mail.com",
                    resendIn = 42,
                ),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvitationScreenUsedPreview() {
    GotestTheme {
        InvitationScreen(
            uiState = InvitationUiState(isLoading = false, page = InvitationPage("t", "used", "SMA 3 Bandung")),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvitationScreenLoadingPreview() {
    GotestTheme { InvitationScreen(uiState = InvitationUiState(), actions = previewActions, onBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun InvitationScreenErrorPreview() {
    GotestTheme {
        InvitationScreen(
            uiState = InvitationUiState(isLoading = false),
            actions = previewActions,
            onBack = {},
        )
    }
}
