package com.gotest.app.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** 426 `update_required` (API preflight 4.2): this version can no longer talk to the server. */
@Composable
fun UpdateRequiredRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    UpdateRequiredScreen(onOpenStore = { openStore(context) }, modifier = modifier)
}

@Composable
fun UpdateRequiredScreen(
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgApp)
                .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        GotestEmptyView(
            icon = GotestIcons.Linear.InfoCircle,
            title = stringResource(R.string.update_required_title),
            message = stringResource(R.string.update_required_message),
            actionText = stringResource(R.string.update_required_action),
            onAction = onOpenStore,
        )
    }
}

private fun openStore(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(web)
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateRequiredScreenPreview() {
    GotestTheme { UpdateRequiredScreen(onOpenStore = {}) }
}
