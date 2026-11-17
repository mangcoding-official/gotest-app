package com.gotest.app.feature.account.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** "Akun ini diatur oleh orang tua atau gurumu di {mitra}." on Profil and Pengaturan (Aturan 11.30). */
@Composable
fun ManagedAccountNote(
    mitra: String,
    modifier: Modifier = Modifier,
) {
    AccountNote(
        text = stringResource(R.string.account_managed_note, mitra),
        icon = GotestIcons.Linear.InfoCircle,
        containerColor = GotestTheme.colors.bgApp,
        iconColor = GotestTheme.colors.iconMuted,
        textColor = GotestTheme.colors.textSecondary,
        modifier = modifier,
    )
}
