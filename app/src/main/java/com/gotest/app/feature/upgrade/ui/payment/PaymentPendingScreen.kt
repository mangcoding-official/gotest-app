package com.gotest.app.feature.upgrade.ui.payment

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.upgrade.ui.components.UpgradeBottomBar
import com.gotest.app.feature.upgrade.ui.components.UpgradeCaption
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData
import com.gotest.app.feature.upgrade.ui.components.UpgradeSectionTitle
import com.gotest.app.feature.upgrade.ui.components.nameRes
import kotlinx.coroutines.launch

private const val PAYMENT_STEP = 2
private const val UPGRADE_STEPS = 2
private const val PREVIEW_REMAINING_SECONDS = 23_092L
private const val PREVIEW_QRIS_REMAINING_SECONDS = 892L
private const val QR_MIME_TYPE = "image/png"

@Composable
fun PaymentPendingRoute(
    onBack: () -> Unit,
    onPaymentSuccess: (orderId: String) -> Unit,
    onChangeMethod: (planId: String, promoId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentPendingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val showMessage: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
    // "Simpan QR": the student picks where the PNG goes, so no storage permission is needed.
    val saveQrLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(QR_MIME_TYPE)) { uri ->
            uri?.let { viewModel.onEvent(PaymentPendingUiEvent.SaveQr(it)) }
        }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is PaymentPendingEffect.PaymentConfirmed -> {
                onPaymentSuccess(effect.orderId)
            }

            is PaymentPendingEffect.ShareQrImage -> {
                context.shareQrImage(effect.uri)
            }

            is PaymentPendingEffect.OpenWalletApp -> {
                // Not installed: stay here; the screen explains what to do and "Buka … Lagi" offers the store.
                context.openWalletApp(effect.wallet, openStoreIfMissing = false)
            }

            is PaymentPendingEffect.ShowMessage -> {
                showMessage(resources.getString(effect.messageRes))
            }
        }
    }
    PaymentPendingScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onCopyVirtualAccount = { number ->
            scope.launch {
                val label = resources.getString(R.string.upgrade_payment_copy_label)
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, number)))
                snackbarHostState.showSnackbar(resources.getString(R.string.upgrade_payment_copied))
            }
        },
        onSaveQr = {
            runCatching { saveQrLauncher.launch(QrisImageExporter.SAVE_FILE_NAME) }
                .onFailure { showMessage(resources.getString(R.string.upgrade_qris_save_error)) }
        },
        onOpenWallet = { wallet ->
            if (!context.openWalletApp(wallet, openStoreIfMissing = true)) {
                val walletName = resources.getString(wallet.nameRes)
                showMessage(resources.getString(R.string.upgrade_wallet_open_error, walletName))
            }
        },
        onChangeMethod = onChangeMethod,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Hands the QR code PNG to the share sheet, with read access to the FileProvider [uri]. */
private fun Context.shareQrImage(uri: Uri) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = QR_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    runCatching { startActivity(Intent.createChooser(send, getString(R.string.upgrade_qris_share_chooser))) }
}

/**
 * Step 2 of 2, by the order's method: "Upgrade / Menunggu Pembayaran" (Virtual Account, frame
 * 11636), "Upgrade / Bayar QRIS" (frame 23375) or "Upgrade / Menunggu Pembayaran E-Wallet" (frame
 * 23705). All three count down to the order's expiry and then disable paying.
 */
@Composable
fun PaymentPendingScreen(
    uiState: PaymentPendingUiState,
    onEvent: (PaymentPendingUiEvent) -> Unit,
    onBack: () -> Unit,
    onCopyVirtualAccount: (number: String) -> Unit,
    onSaveQr: () -> Unit,
    onOpenWallet: (EWallet) -> Unit,
    onChangeMethod: (planId: String, promoId: String?) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.upgrade_payment_title),
                subtitle = stringResource(R.string.upgrade_step_progress, PAYMENT_STEP, UPGRADE_STEPS),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        bottomBar = {
            if (uiState is PaymentPendingUiState.Success) {
                PaymentPendingBottomBar(state = uiState, onEvent = onEvent, onChangeMethod = onChangeMethod)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState) {
            PaymentPendingUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            PaymentPendingUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(PaymentPendingUiEvent.Retry) }, modifier = contentModifier)
            }

            is PaymentPendingUiState.Success -> {
                val order = uiState.order
                val qris = order.qris
                val wallet = order.eWallet
                when {
                    order.method == PaymentMethodType.Qris && qris != null -> {
                        QrisPaymentContent(
                            state = uiState,
                            qris = qris,
                            onSaveQr = onSaveQr,
                            onShareQr = { onEvent(PaymentPendingUiEvent.ShareQr) },
                            modifier = contentModifier,
                        )
                    }

                    order.method == PaymentMethodType.EWallet && wallet != null -> {
                        EWalletPaymentContent(
                            state = uiState,
                            wallet = wallet,
                            onOpenWallet = onOpenWallet,
                            modifier = contentModifier,
                        )
                    }

                    else -> {
                        PaymentPendingContent(
                            state = uiState,
                            onCopyVirtualAccount = onCopyVirtualAccount,
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Saya Sudah Membayar" (VA, QRIS) or "Cek Status Pembayaran" (e-wallet), both confirming the
 * payment, plus the method's caption or "Ganti metode pembayaran". Only the VA footer has a hairline.
 */
@Composable
private fun PaymentPendingBottomBar(
    state: PaymentPendingUiState.Success,
    onEvent: (PaymentPendingUiEvent) -> Unit,
    onChangeMethod: (planId: String, promoId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val method = state.order.method
    UpgradeBottomBar(modifier = modifier, showTopBorder = method == PaymentMethodType.VirtualAccount) {
        GotestButton(
            text =
                stringResource(
                    if (method == PaymentMethodType.EWallet) {
                        R.string.upgrade_wallet_check_status
                    } else {
                        R.string.upgrade_payment_confirm
                    },
                ),
            onClick = { onEvent(PaymentPendingUiEvent.ConfirmPayment) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isConfirming && !state.isExpired,
        )
        when (method) {
            PaymentMethodType.VirtualAccount -> {
                UpgradeCaption(text = stringResource(R.string.upgrade_payment_status_note))
            }

            PaymentMethodType.Qris -> {
                UpgradeCaption(text = stringResource(R.string.upgrade_qris_status_note))
            }

            PaymentMethodType.EWallet -> {
                GotestButton(
                    text = stringResource(R.string.upgrade_wallet_change_method),
                    onClick = { onChangeMethod(state.order.planId, state.order.promoId) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = GotestButtonVariant.Text,
                    size = GotestButtonSize.Medium,
                    contentColor = GotestTheme.colors.bgBrand,
                )
            }
        }
    }
}

@Composable
private fun PaymentPendingContent(
    state: PaymentPendingUiState.Success,
    onCopyVirtualAccount: (number: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    top = spacing.sm,
                    end = spacing.screenHorizontal,
                    bottom = spacing.lg,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PaymentStatusCard(state = state)
        val bankName = state.order.bankName
        val number = state.order.virtualAccountNumber
        if (bankName != null && number != null) {
            VirtualAccountCard(
                bankName = bankName,
                number = number,
                onCopy = { onCopyVirtualAccount(number.filterNot(Char::isWhitespace)) },
            )
            PaymentSteps(steps = stringArrayResource(R.array.upgrade_payment_steps_bca).toList())
        }
    }
}

/** Yellow "status" card: amount and the Jam : Menit : Detik countdown. */
@Composable
private fun PaymentStatusCard(
    state: PaymentPendingUiState.Success,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.statusWarningSoft, GotestTheme.shapes.largeIncreased)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GotestChip(text = stringResource(R.string.upgrade_payment_waiting), tone = GotestChipTone.Yellow)
        Text(
            text = GotestFormat.rupiah(state.order.totalRupiah),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
        )
        val note =
            if (state.isExpired) R.string.upgrade_payment_expired_note else R.string.upgrade_payment_deadline_note
        Text(
            text = stringResource(note),
            style = MaterialTheme.typography.labelSmall,
            color = colors.bgYellowDark,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountdownBox(
                value = state.hours,
                unit = stringResource(R.string.upgrade_payment_hours),
                modifier = Modifier.weight(1f),
            )
            CountdownSeparator()
            CountdownBox(
                value = state.minutes,
                unit = stringResource(R.string.upgrade_payment_minutes),
                modifier = Modifier.weight(1f),
            )
            CountdownSeparator()
            CountdownBox(
                value = state.seconds,
                unit = stringResource(R.string.upgrade_payment_seconds),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CountdownBox(
    value: Long,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(GotestTheme.colors.bgApp, MaterialTheme.shapes.medium)
                .padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.upgrade_payment_countdown_value, value),
            style = MaterialTheme.typography.headlineSmall,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun CountdownSeparator(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.upgrade_payment_countdown_separator),
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        color = GotestTheme.colors.bgYellowDark,
    )
}

@Composable
private fun VirtualAccountCard(
    bankName: String,
    number: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .width(UpgradeDimens.bankBadgeWidth)
                        .height(UpgradeDimens.bankBadgeHeight)
                        .background(colors.bgBlueSoft, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = bankName, style = MaterialTheme.typography.labelMedium, color = colors.bgBlueDark)
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(R.string.upgrade_payment_bank_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.upgrade_payment_bank_name, bankName),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                    .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(R.string.upgrade_payment_va_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
                Text(text = number, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }
            CopyButton(onClick = onCopy)
        }
    }
}

@Composable
private fun CopyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    PaddingValues(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm),
                ),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.DocumentCopy),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconXs),
                tint = colors.iconBrand,
            )
            Text(
                text = stringResource(R.string.upgrade_payment_copy),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textBrandDark,
            )
        }
    }
}

/** "Cara Membayar": numbered instructions for the destination bank. */
@Composable
private fun PaymentSteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        UpgradeSectionTitle(text = stringResource(R.string.upgrade_payment_steps_title))
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
            verticalSpacing = spacing.xl,
        ) {
            steps.forEachIndexed { index, step -> PaymentStep(number = index + 1, text = step) }
        }
    }
}

@Composable
private fun PaymentStep(
    number: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(UpgradeDimens.stepBadge)
                    .background(GotestTheme.colors.bgBrandSoft, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = GotestFormat.number(number),
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textBrandDark,
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.order,
                    remainingSeconds = PREVIEW_REMAINING_SECONDS,
                    isConfirming = false,
                ),
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenQrisPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.qrisOrder,
                    remainingSeconds = PREVIEW_QRIS_REMAINING_SECONDS,
                    isConfirming = false,
                ),
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenQrisExpiredPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.qrisOrder,
                    remainingSeconds = 0,
                    isConfirming = false,
                ),
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenEWalletPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.eWalletOrder,
                    remainingSeconds = PREVIEW_QRIS_REMAINING_SECONDS,
                    isConfirming = false,
                ),
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenExpiredPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.order,
                    remainingSeconds = 0,
                    isConfirming = false,
                ),
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenLoadingPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState = PaymentPendingUiState.Loading,
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentPendingScreenErrorPreview() {
    GotestTheme {
        PaymentPendingScreen(
            uiState = PaymentPendingUiState.Error,
            onEvent = {},
            onBack = {},
            onCopyVirtualAccount = {},
            onSaveQr = {},
            onOpenWallet = {},
            onChangeMethod = { _, _ -> },
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
