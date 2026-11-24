package com.gotest.app.feature.auth.ui.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.auth.domain.ConsentDelivery
import com.gotest.app.feature.auth.domain.ConsentState
import com.gotest.app.feature.auth.domain.ConsentStatus
import com.gotest.app.feature.auth.domain.GuardianChannel
import com.gotest.app.feature.auth.domain.canResend
import com.gotest.app.feature.auth.domain.emailClosed
import com.gotest.app.feature.auth.domain.offersEmail
import com.gotest.app.feature.auth.ui.components.AuthCopyright
import com.gotest.app.feature.auth.ui.components.AuthFormCard
import com.gotest.app.feature.auth.ui.components.GuardianChannelChoice
import com.gotest.app.feature.auth.ui.components.GuardianContactField
import java.time.Instant

@Composable
fun GuardianConsentRoute(
    onSignOutSuccess: () -> Unit,
    onApprove: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianConsentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // As the web reads the page again when it loads: also when the app comes back from the background.
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(GuardianConsentUiEvent.Refresh)
        onPauseOrDispose { }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            GuardianConsentEffect.SignedOut -> onSignOutSuccess()
            GuardianConsentEffect.Approved -> onApprove()
        }
    }
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.onEvent(GuardianConsentUiEvent.Refresh) },
        modifier = modifier,
    ) {
        GuardianConsentScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onDeleteAccount = onDeleteAccount,
        )
    }
}

@Composable
fun GuardianConsentScreen(
    uiState: GuardianConsentUiState,
    onEvent: (GuardianConsentUiEvent) -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val consent = uiState.consent
    AuthFormCard(
        title =
            stringResource(
                when (consent?.status) {
                    ConsentStatus.Declined -> R.string.consent_title_declined
                    ConsentStatus.Revoked -> R.string.consent_title_revoked
                    else -> R.string.consent_title_pending
                },
            ),
        subtitle = consent?.let { consentMessage(it) }.orEmpty(),
        onBack = null,
        modifier = modifier,
    ) {
        if (consent == null) {
            GotestLoadingView(modifier = Modifier.fillMaxWidth())
            return@AuthFormCard
        }
        uiState.emailVerifiedReason?.let { NoticeBox(stringResource(R.string.email_verified_notice, it)) }
        uiState.notice?.let { NoticeBox(it) }
        if (consent.canResend) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                GotestButton(
                    text =
                        stringResource(
                            if (uiState.isResending) R.string.auth_processing else R.string.consent_resend,
                        ),
                    onClick = { onEvent(GuardianConsentUiEvent.Resend) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isResending,
                )
                uiState.resendError?.let { ErrorText(it) }
            }
        }
        val change = uiState.change
        if (change != null) {
            ChangeContactForm(consent = consent, form = change, isSubmitting = uiState.isChanging, onEvent = onEvent)
        } else {
            GotestButton(
                text =
                    stringResource(
                        if (consent.offersEmail) R.string.consent_change_contact else R.string.consent_change_number,
                    ),
                onClick = { onEvent(GuardianConsentUiEvent.OpenChange) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GotestButton(
                text = stringResource(R.string.consent_sign_out),
                onClick = { onEvent(GuardianConsentUiEvent.SignOut) },
                variant = GotestButtonVariant.Text,
                enabled = !uiState.isSigningOut,
            )
            GotestButton(
                text = stringResource(R.string.consent_delete_account),
                onClick = onDeleteAccount,
                variant = GotestButtonVariant.Text,
            )
        }
        AuthCopyright()
    }
}

/** The web's message for each status and where the link is (Auth/GuardianConsentWaiting.tsx). */
@Composable
private fun consentMessage(consent: ConsentState): String {
    val byEmail = consent.channel == GuardianChannel.Email
    val number = consent.contact ?: stringResource(R.string.consent_guardian)
    val where = stringResource(if (byEmail) R.string.consent_where_email else R.string.consent_where_whatsapp, number)
    val contactWord =
        stringResource(if (consent.offersEmail) R.string.consent_word_contact else R.string.consent_word_number)
    val channelWord = stringResource(if (byEmail) R.string.consent_word_email else R.string.consent_word_number)
    val limited = stringResource(R.string.consent_limited, number, contactWord)
    return when (consent.status) {
        ConsentStatus.Pending -> {
            pendingMessage(consent, where, number, limited)
        }

        // Sending again is held back for the day, so the page says so instead of asking for it.
        ConsentStatus.Expired -> {
            if (consent.delivery ==
                ConsentDelivery.Limited
            ) {
                limited
            } else {
                stringResource(R.string.consent_expired, number)
            }
        }

        ConsentStatus.Declined -> {
            stringResource(R.string.consent_declined, channelWord, number, contactWord)
        }

        ConsentStatus.Revoked -> {
            stringResource(R.string.consent_revoked, channelWord, number)
        }
    }
}

@Composable
private fun pendingMessage(
    consent: ConsentState,
    where: String,
    number: String,
    limited: String,
): String =
    when (consent.delivery) {
        ConsentDelivery.Queued -> {
            stringResource(R.string.consent_queued, where)
        }

        ConsentDelivery.Sent -> {
            stringResource(R.string.consent_sent, where, consent.expiresAt?.let(GotestFormat::windowInstant).orEmpty())
        }

        ConsentDelivery.Limited -> {
            limited
        }

        ConsentDelivery.Failed -> {
            stringResource(
                if (consent.emailClosed) R.string.consent_failed_email_closed else R.string.consent_failed,
                number,
            )
        }
    }

@Composable
private fun ChangeContactForm(
    consent: ConsentState,
    form: ConsentChangeForm,
    isSubmitting: Boolean,
    onEvent: (GuardianConsentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (consent.channels.size > 1) {
            GuardianChannelChoice(
                channels = consent.channels,
                selected = form.channel,
                onSelect = { onEvent(GuardianConsentUiEvent.ChannelPicked(it)) },
            )
        }
        GuardianContactField(
            channel = form.channel,
            value = form.contact,
            onValueChange = { onEvent(GuardianConsentUiEvent.ContactChanged(it)) },
            error = form.errorMessage,
            showHelper = false,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.consent_cancel),
                onClick = { onEvent(GuardianConsentUiEvent.CancelChange) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text =
                    stringResource(
                        when {
                            isSubmitting -> R.string.auth_processing
                            form.channel == GuardianChannel.Email -> R.string.consent_send_email
                            else -> R.string.consent_send_number
                        },
                    ),
                onClick = { onEvent(GuardianConsentUiEvent.SubmitChange) },
                modifier = Modifier.weight(1f),
                enabled = !isSubmitting,
            )
        }
    }
}

@Composable
private fun NoticeBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = GotestTheme.colors.statusSuccessSoft,
        showBorder = false,
        contentPadding = PaddingValues(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.statusSuccess)
    }
}

@Composable
private fun ErrorText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = GotestTheme.colors.statusDanger,
    )
}

private val PreviewConsent =
    ConsentState(
        status = ConsentStatus.Pending,
        contact = "0812-****-890",
        channel = GuardianChannel.WhatsApp,
        channels = listOf(GuardianChannel.WhatsApp, GuardianChannel.Email),
        delivery = ConsentDelivery.Sent,
        expiresAt = Instant.parse("2026-11-23T07:30:00Z"),
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentLoadingPreview() {
    GotestTheme {
        GuardianConsentScreen(GuardianConsentUiState(), onEvent = {}, onDeleteAccount = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentSentPreview() {
    GotestTheme {
        GuardianConsentScreen(GuardianConsentUiState(consent = PreviewConsent), onEvent = {}, onDeleteAccount = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentResentPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(consent = PreviewConsent, notice = "Tautan baru dikirim ke WhatsApp 0812-****-890."),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentLimitedPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(consent = PreviewConsent.copy(delivery = ConsentDelivery.Limited)),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentChangePreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(
                consent = PreviewConsent,
                change =
                    ConsentChangeForm(
                        channel = GuardianChannel.Email,
                        contact = "ibu@contoh",
                        errorMessage = "Tulis email yang benar, misalnya nama@contoh.id.",
                    ),
            ),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentExpiredPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(
                consent =
                    PreviewConsent.copy(
                        status = ConsentStatus.Expired,
                        channels = listOf(GuardianChannel.WhatsApp),
                    ),
            ),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentDeclinedPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(
                consent =
                    PreviewConsent.copy(
                        status = ConsentStatus.Declined,
                        channel = GuardianChannel.Email,
                        contact = "r***@contoh.id",
                    ),
            ),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentRevokedPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(consent = PreviewConsent.copy(status = ConsentStatus.Revoked)),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GuardianConsentFailedEmailClosedPreview() {
    GotestTheme {
        GuardianConsentScreen(
            GuardianConsentUiState(
                consent =
                    PreviewConsent.copy(
                        channel = GuardianChannel.Email,
                        contact = "r***@contoh.id",
                        channels = listOf(GuardianChannel.WhatsApp),
                        delivery = ConsentDelivery.Failed,
                    ),
            ),
            onEvent = {},
            onDeleteAccount = {},
        )
    }
}
