package com.gotest.app.feature.account.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestPasswordField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.account.domain.ActiveSession
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import java.time.Instant

@Composable
fun ActiveSessionsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveSessionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ActiveSessionsScreen(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    uiState: ActiveSessionsUiState,
    onEvent: (ActiveSessionsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    AccountDetailScaffold(
        title = stringResource(R.string.account_sessions_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when {
            uiState.isLoading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            uiState.loadFailed -> {
                GotestErrorView(
                    onRetry = { onEvent(ActiveSessionsUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    // The web's lede, above the list: the app bar keeps one line only.
                    Text(
                        text = stringResource(R.string.account_sessions_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GotestTheme.colors.textSecondary,
                    )
                    if (uiState.otherCount > 0) {
                        GotestButton(
                            text = stringResource(R.string.account_sessions_revoke),
                            onClick = { onEvent(ActiveSessionsUiEvent.OpenRevoke) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = GotestButtonVariant.Plain,
                        )
                    }
                    SessionList(sessions = uiState.sessions)
                }
            }
        }
    }
    uiState.revoke?.let { form ->
        GotestBottomSheet(onDismissRequest = { onEvent(ActiveSessionsUiEvent.DismissRevoke) }) {
            RevokeSessionsContent(form = form, otherCount = uiState.otherCount, onEvent = onEvent)
        }
    }
}

@Composable
private fun SessionList(
    sessions: List<ActiveSession>,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        if (sessions.isEmpty()) {
            Muted(stringResource(R.string.account_sessions_empty))
        }
        sessions.forEachIndexed { index, session ->
            if (index > 0) HorizontalDivider(color = GotestTheme.colors.borderSubtle)
            SessionRow(session)
        }
    }
}

@Composable
private fun SessionRow(
    session: ActiveSession,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.device,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            if (session.current) {
                GotestChip(
                    text = stringResource(R.string.account_sessions_current),
                    tone = GotestChipTone.Violet,
                )
            }
        }
        Muted(session.ip ?: stringResource(R.string.account_sessions_unknown_ip))
        val lastActive = session.lastActiveAt
        when {
            session.current -> {
                Muted(stringResource(R.string.account_sessions_in_use))
            }

            lastActive != null -> {
                Muted(
                    stringResource(R.string.account_sessions_last_active, GotestFormat.windowInstant(lastActive)),
                )
            }
        }
    }
}

/** "Keluar dari perangkat lain?" (web: RevokeSessionsModal). */
@Composable
private fun RevokeSessionsContent(
    form: RevokeSessionsForm,
    otherCount: Int,
    onEvent: (ActiveSessionsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier = modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.account_sessions_confirm_title),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Muted(stringResource(R.string.account_sessions_confirm_message, otherCount))
        }
        GotestPasswordField(
            value = form.password,
            onValueChange = { onEvent(ActiveSessionsUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.account_sessions_password_label),
            supportingText = form.errorMessage,
            isError = form.errorMessage != null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.account_sessions_cancel),
                onClick = { onEvent(ActiveSessionsUiEvent.DismissRevoke) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text =
                    stringResource(
                        if (form.isSubmitting) R.string.account_sessions_revoking else R.string.account_sessions_revoke,
                    ),
                onClick = { onEvent(ActiveSessionsUiEvent.ConfirmRevoke) },
                modifier = Modifier.weight(1f),
                enabled = !form.isSubmitting,
            )
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

private val PreviewSessions =
    listOf(
        ActiveSession("this", "Gotest di Android", "103.10.66.12", null, current = true),
        ActiveSession(
            "web",
            "Chrome di Windows",
            "36.71.140.5",
            Instant.parse("2026-11-16T05:30:00Z"),
            current = false,
        ),
        ActiveSession("iphone", "Safari di iPhone", null, Instant.parse("2026-11-13T09:00:00Z"), current = false),
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun ActiveSessionsPreview() {
    GotestTheme {
        ActiveSessionsScreen(
            ActiveSessionsUiState(isLoading = false, sessions = PreviewSessions),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun ActiveSessionsOnlyThisPreview() {
    GotestTheme {
        ActiveSessionsScreen(ActiveSessionsUiState(isLoading = false, sessions = PreviewSessions.take(1)), onEvent = {
        }, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun ActiveSessionsEmptyPreview() {
    GotestTheme {
        ActiveSessionsScreen(ActiveSessionsUiState(isLoading = false), onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun ActiveSessionsLoadingPreview() {
    GotestTheme { ActiveSessionsScreen(ActiveSessionsUiState(), onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 800)
@Composable
private fun ActiveSessionsErrorPreview() {
    GotestTheme {
        ActiveSessionsScreen(ActiveSessionsUiState(isLoading = false, loadFailed = true), onEvent = {}, onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun RevokeSessionsPreview() {
    GotestTheme {
        RevokeSessionsContent(
            form = RevokeSessionsForm(errorMessage = "Password saat ini tidak sesuai."),
            otherCount = 2,
            onEvent = {},
        )
    }
}
