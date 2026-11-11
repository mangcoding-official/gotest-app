package com.gotest.app.feature.upgrade.ui.payment

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.gotest.app.core.model.EWallet

/** Android package of each wallet app. Keep in sync with `<queries>` in the manifest (Android 11+). */
private val EWallet.packageName: String
    get() =
        when (this) {
            EWallet.GoPay -> "com.gojek.gopay"
            EWallet.Ovo -> "ovo.id"
            EWallet.Dana -> "id.dana"
            EWallet.ShopeePay -> "com.shopee.id"
        }

/**
 * Opens [wallet]'s app. When it is not installed, [openStoreIfMissing] opens its Play Store page
 * instead (the Play Store app, else the web page). Returns false when nothing could be opened.
 */
internal fun Context.openWalletApp(
    wallet: EWallet,
    openStoreIfMissing: Boolean,
): Boolean {
    val launch = packageManager.getLaunchIntentForPackage(wallet.packageName)
    val intents =
        when {
            launch != null -> {
                listOf(launch)
            }

            openStoreIfMissing -> {
                listOf(
                    Intent(Intent.ACTION_VIEW, "market://details?id=${wallet.packageName}".toUri()),
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=${wallet.packageName}".toUri(),
                    ),
                )
            }

            else -> {
                emptyList()
            }
        }
    return intents.any { intent -> runCatching { startActivity(intent) }.isSuccess }
}
