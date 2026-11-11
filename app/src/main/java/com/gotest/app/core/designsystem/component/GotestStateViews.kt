package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Full-area spinner for UiState.Loading. */
@Composable
fun GotestLoadingView(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = GotestTheme.colors.bgBrandDark)
    }
}

/**
 * Empty state from the design (Notifikasi / Kosong): grey icon circle, title, message and an
 * optional action.
 */
@Composable
fun GotestEmptyView(
    @DrawableRes icon: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier = modifier.padding(horizontal = spacing.xl, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(GotestTheme.sizes.emptyStateIcon)
                    .background(colors.bgGrey, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconLg),
                tint = colors.iconDefault,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionText != null) {
            GotestButton(text = actionText, onClick = onAction, size = GotestButtonSize.Medium)
        }
    }
}

/** UiState.Error: the empty-state layout with a retry button. */
@Composable
fun GotestErrorView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_error_message),
) {
    GotestEmptyView(
        icon = GotestIcons.Linear.InfoCircle,
        title = stringResource(R.string.state_error_title),
        message = message,
        modifier = modifier,
        actionText = stringResource(R.string.state_error_retry),
        onAction = onRetry,
    )
}

@Preview(showBackground = true)
@Composable
private fun GotestEmptyViewPreview() {
    GotestTheme {
        GotestEmptyView(
            icon = GotestIcons.Bold.Notification,
            title = "Belum ada notifikasi",
            message = "Semua notifikasi tentang latihan, tryout, dan peringkatmu akan muncul di sini.",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestErrorViewPreview() {
    GotestTheme {
        GotestErrorView(onRetry = {}, modifier = Modifier.fillMaxWidth())
    }
}
