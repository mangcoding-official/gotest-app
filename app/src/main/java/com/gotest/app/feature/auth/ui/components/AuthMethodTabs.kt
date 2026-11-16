package com.gotest.app.feature.auth.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestSegmentedControl

/** "Nomor HP" | "Email" on Masuk and Daftar, shown only while WhatsApp codes are available (web: PhoneOtpForm). */
@Composable
fun AuthMethodTabs(
    phoneSelected: Boolean,
    onSelectPhone: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestSegmentedControl(
        options = listOf(stringResource(R.string.auth_tab_phone), stringResource(R.string.auth_tab_email)),
        selectedIndex = if (phoneSelected) 0 else 1,
        onSelect = { onSelectPhone(it == 0) },
        modifier = modifier,
    )
}
