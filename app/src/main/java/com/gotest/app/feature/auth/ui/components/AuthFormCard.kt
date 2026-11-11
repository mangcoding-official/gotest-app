package com.gotest.app.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * Full-height card used by "Auth / Daftar" and "Auth / Lupa Password": back button and heading
 * on top, then the scrollable form.
 */
@Composable
fun AuthFormCard(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = GotestTheme.spacing
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
    ) {
        GotestCard(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(spacing.xl),
            verticalSpacing = spacing.xl,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GotestBackButton(onClick = onBack)
                AuthHeading(title = title, subtitle = subtitle)
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
                content = content,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 600)
@Composable
private fun AuthFormCardPreview() {
    GotestTheme {
        AuthFormCard(title = "Lupa Password ?", subtitle = "Masukkan email untuk mereset password", onBack = {}) {
            AuthCopyright()
        }
    }
}
