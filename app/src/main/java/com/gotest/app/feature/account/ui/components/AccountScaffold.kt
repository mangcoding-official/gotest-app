package com.gotest.app.feature.account.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * Detail-screen frame shared by the account screens: centred "app-bar" with the back button,
 * grey background, optional pinned [bottomBar] and a snackbar host above it. [content] gets the
 * padding of both bars and draws edge to edge otherwise.
 */
@Composable
fun AccountDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(GotestTheme.spacing.none),
        content = content,
    )
}

/** White "bottom-bar" with a hairline on top; it rides above the keyboard and the navigation bar. */
@Composable
fun AccountBottomActions(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(GotestTheme.colors.bgApp)) {
        GotestDivider()
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 400)
@Composable
private fun AccountDetailScaffoldPreview() {
    GotestTheme {
        AccountDetailScaffold(
            title = "Edit Informasi Akun",
            onBack = {},
            bottomBar = {
                AccountBottomActions {
                    GotestButton(text = "Simpan", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            },
        ) { padding ->
            Text(
                text = "Konten",
                modifier = Modifier.fillMaxSize().padding(padding).padding(GotestTheme.spacing.screenHorizontal),
            )
        }
    }
}
