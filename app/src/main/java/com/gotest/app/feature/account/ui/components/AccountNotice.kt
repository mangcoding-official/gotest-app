package com.gotest.app.feature.account.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import com.gotest.app.R
import kotlinx.coroutines.launch

/**
 * Results a closed account screen hands back to the screen below it (Profil, Pengaturan or
 * Data Pribadi), shown there as a snackbar.
 */
enum class AccountNotice(
    @param:StringRes val messageRes: Int,
) {
    PasswordChanged(R.string.account_notice_password_changed),
    MainLevelSaved(R.string.account_notice_main_level_saved),
    DataCopyRequested(R.string.account_notice_data_copy_requested),
}

/**
 * Shows [notice], or [text] (a server's message handed back, such as "Email baru tersimpan."), in
 * [snackbarHostState] once, then [onConsume]s it so it does not repeat.
 */
@Composable
fun AccountNoticeEffect(
    notice: AccountNotice?,
    onConsume: () -> Unit,
    snackbarHostState: SnackbarHostState,
    text: String? = null,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val currentOnConsume by rememberUpdatedState(onConsume)
    LaunchedEffect(notice, text) {
        val message = notice?.let { resources.getString(it.messageRes) } ?: text
        if (message != null) {
            // Launched outside this effect: consuming the notice restarts it.
            scope.launch { snackbarHostState.showSnackbar(message) }
            currentOnConsume()
        }
    }
}
