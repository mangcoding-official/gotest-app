package com.gotest.app.feature.ruang.ui.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.components.RuangScaffold

/** A page of these screens: the web's lede, then one card. */
@Composable
internal fun JoinPage(
    lede: String?,
    padding: PaddingValues,
    card: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        lede?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        GotestCard(modifier = Modifier.fillMaxWidth(), content = card)
    }
}

@Composable
internal fun CardHeading(
    title: String,
    text: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        text?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
internal fun ActionErrorText(error: ActionError?) {
    error?.let {
        NoticeBox(
            text = it.message ?: stringResource(R.string.state_error_message),
            tone = NoticeTone.Danger,
        )
    }
}

@Composable
fun JoinWithCodeRoute(
    onBack: () -> Unit,
    onOpenCode: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinWithCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.found, onOpenCode)
    JoinWithCodeScreen(
        uiState = uiState,
        onCodeChange = viewModel::onCodeChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun JoinWithCodeScreen(
    uiState: JoinWithCodeUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RuangScaffold(title = stringResource(R.string.join_title), onBack = onBack, modifier = modifier) { padding ->
        JoinPage(lede = stringResource(R.string.join_lede), padding = padding) {
            CardHeading(stringResource(R.string.join_title), stringResource(R.string.join_code_helper))
            GotestTextField(
                value = uiState.code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.join_code_label),
                isError = uiState.error != null,
                supportingText = uiState.error?.let { it.message ?: stringResource(R.string.state_error_message) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
            GotestButton(
                text = stringResource(if (uiState.isSubmitting) R.string.join_processing else R.string.join_continue),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
            )
        }
    }
}

@Composable
fun JoinCodeRoute(
    onBack: () -> Unit,
    onJoinSuccess: (JoinedMitra) -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Kept when the screen turns, so the joined dialog does not vanish (A2b review ulang).
    var staffJoin by rememberSaveable(stateSaver = JoinedMitraSaver) { mutableStateOf<JoinedMitra?>(null) }
    ObserveAsEvents(viewModel.joined) { joined ->
        if (joined.role == STUDENT) onJoinSuccess(joined) else staffJoin = joined
    }
    staffJoin?.let { joined ->
        StaffJoinedDialog(mitraName = uiState.page?.organizationName.orEmpty(), onClose = { onJoinSuccess(joined) })
    }
    JoinCodeScreen(
        uiState = uiState,
        actions =
            JoinCodeActions(
                onJoin = viewModel::join,
                onRetry = viewModel::load,
                onSignIn = onSignIn,
                onRegister = onRegister,
            ),
        onBack = onBack,
        modifier = modifier,
    )
}

data class JoinCodeActions(
    val onJoin: () -> Unit,
    val onRetry: () -> Unit,
    val onSignIn: () -> Unit,
    val onRegister: () -> Unit,
)

@Composable
fun JoinCodeScreen(
    uiState: JoinCodeUiState,
    actions: JoinCodeActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    RuangScaffold(
        title =
            page?.let {
                joinHeading(it)
            } ?: stringResource(R.string.join_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
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

            page.kind == "event" -> {
                EventCodeContent(
                    page = page,
                    uiState = uiState,
                    actions = actions,
                    padding = padding,
                )
            }

            else -> {
                ClassCodeContent(page = page, uiState = uiState, actions = actions, padding = padding)
            }
        }
    }
}

/** "Kelas 8A" or "Kode kelas"; for an event, its label, the Mitra, or "Gabung ke kelas" (Join.tsx). */
@Composable
private fun joinHeading(page: JoinCodePage): String =
    if (page.kind == "event") {
        page.label ?: page.organizationName ?: stringResource(R.string.join_title)
    } else {
        page.className?.let { stringResource(R.string.join_class_heading, it) }
            ?: stringResource(R.string.join_class_heading_plain)
    }

@Composable
private fun ClassCodeContent(
    page: JoinCodePage,
    uiState: JoinCodeUiState,
    actions: JoinCodeActions,
    padding: PaddingValues,
) {
    val heading = joinHeading(page)
    val mitra = page.organizationName
    val lede =
        mitra?.let { stringResource(R.string.join_class_lede, it) } ?: stringResource(R.string.join_class_lede_plain)
    JoinPage(lede = lede, padding = padding) {
        when (page.state) {
            "guest" -> {
                val from = mitra?.let { stringResource(R.string.join_class_guest_from, it) }.orEmpty()
                CardHeading(
                    stringResource(R.string.join_class_title, heading),
                    stringResource(R.string.join_class_guest, from),
                )
                GuestButtons(actions)
            }

            "ready", "joined" -> {
                val joined = page.state == "joined"
                CardHeading(
                    title =
                        if (joined) {
                            stringResource(
                                R.string.join_class_joined,
                            )
                        } else {
                            stringResource(R.string.join_class_title, heading)
                        },
                    text =
                        when {
                            joined -> page.message
                            mitra != null -> stringResource(R.string.join_class_ready_at, mitra)
                            else -> stringResource(R.string.join_class_ready)
                        },
                )
                ActionErrorText(uiState.error)
                SubmitButton(
                    text = stringResource(if (joined) R.string.join_open_space else R.string.join_class_submit),
                    busy = uiState.isJoining,
                    onClick = actions.onJoin,
                )
            }

            else -> {
                CardHeading(stringResource(refusedTitle(page.state)), page.message)
            }
        }
    }
}

/** The title of a class code that cannot be used (Join.tsx `REFUSED_TITLES`). */
private fun refusedTitle(state: String): Int =
    when (state) {
        "inactive", "blocked" -> R.string.join_refused_inactive
        "full" -> R.string.join_refused_full
        else -> R.string.join_refused_invalid
    }

@Composable
private fun EventCodeContent(
    page: JoinCodePage,
    uiState: JoinCodeUiState,
    actions: JoinCodeActions,
    padding: PaddingValues,
) {
    val heading = joinHeading(page)
    val mitra = page.organizationName
    val lede =
        mitra?.let { stringResource(R.string.join_event_lede, it) } ?: stringResource(R.string.join_event_lede_plain)
    JoinPage(lede = lede, padding = padding) {
        when (page.state) {
            "guest" -> {
                CardHeading(
                    stringResource(R.string.join_event_title, heading),
                    stringResource(R.string.join_event_guest),
                )
                GuestButtons(actions)
            }

            "ready" -> {
                CardHeading(
                    stringResource(R.string.join_event_title, heading),
                    mitra?.let {
                        stringResource(R.string.join_event_ready, it)
                    },
                )
                ActionErrorText(uiState.error)
                SubmitButton(
                    text = stringResource(R.string.join_event_submit),
                    busy = uiState.isJoining,
                    onClick = actions.onJoin,
                )
            }

            "expired" -> {
                CardHeading(
                    stringResource(R.string.join_event_expired_title),
                    stringResource(R.string.join_event_expired, page.code),
                )
            }

            "full" -> {
                CardHeading(stringResource(R.string.join_event_full_title), stringResource(R.string.join_event_full))
            }

            else -> {
                CardHeading(
                    stringResource(R.string.join_event_invalid_title),
                    stringResource(R.string.join_event_invalid),
                )
            }
        }
    }
}

@Composable
private fun GuestButtons(actions: JoinCodeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
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
}

@Composable
internal fun SubmitButton(
    text: String,
    busy: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    GotestButton(
        text = if (busy) stringResource(R.string.join_processing) else text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !busy,
    )
}

/** The joined Mitra as plain text, so it survives the screen being recreated. */
internal val JoinedMitraSaver: Saver<JoinedMitra?, Any> =
    Saver(
        save = { joined -> joined?.let { arrayListOf(it.code, it.role, it.className) } },
        restore = { saved ->
            (saved as? List<*>)?.let { JoinedMitra(it[0] as String, it[1] as String, it[2] as String?) }
        },
    )

/**
 * An event code of the account's own Mitra joins it as staff, whose portal the app does not have yet
 * (A4): the web's portal is pointed to instead (T-026 A2b-2, decided by Ardy).
 */
@Composable
internal fun StaffJoinedDialog(
    mitraName: String,
    onClose: () -> Unit,
) {
    GotestDialog(
        title = stringResource(R.string.join_staff_title),
        message = stringResource(R.string.join_staff_message, mitraName),
        confirmText = stringResource(R.string.join_staff_close),
        onConfirm = onClose,
        onDismissRequest = onClose,
    )
}

internal const val STUDENT = "siswa"

private val previewActions = JoinCodeActions({}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun JoinWithCodeScreenPreview() {
    GotestTheme {
        JoinWithCodeScreen(
            uiState =
                JoinWithCodeUiState(
                    code = "KELAS9",
                    error = ActionError("Kode tidak ditemukan. Periksa lagi kodenya, lalu coba sekali lagi."),
                ),
            onCodeChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinCodeScreenClassPreview() {
    GotestTheme {
        JoinCodeScreen(
            uiState =
                JoinCodeUiState(
                    isLoading = false,
                    page = JoinCodePage("KELAS8A", "ready", "SMP 2 Cimahi", kind = "class", className = "8A"),
                ),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinCodeScreenFullPreview() {
    GotestTheme {
        JoinCodeScreen(
            uiState =
                JoinCodeUiState(
                    isLoading = false,
                    page =
                        JoinCodePage(
                            "PENUH01",
                            "full",
                            "SMP 2 Cimahi",
                            kind = "class",
                            className = "8B",
                            message = "Kursi siswa SMP 2 Cimahi sudah penuh. Hubungi pengajarmu.",
                        ),
                ),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinCodeScreenEventPreview() {
    GotestTheme {
        JoinCodeScreen(
            uiState =
                JoinCodeUiState(
                    isLoading = false,
                    page = JoinCodePage("EVENT01", "ready", "Bimbel Juara", "Try Out Akbar Desember", "event"),
                ),
            actions = previewActions,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinCodeScreenLoadingPreview() {
    GotestTheme { JoinCodeScreen(uiState = JoinCodeUiState(), actions = previewActions, onBack = {}) }
}

@Preview(showBackground = true)
@Composable
private fun JoinCodeScreenErrorPreview() {
    GotestTheme { JoinCodeScreen(uiState = JoinCodeUiState(isLoading = false), actions = previewActions, onBack = {}) }
}
