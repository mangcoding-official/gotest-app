package com.gotest.app.feature.leaderboard.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import kotlinx.coroutines.launch

/** One rule of "Tentang Leaderboard": icon, title and explanation. */
private data class InfoItem(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

private val infoItems =
    listOf(
        InfoItem(
            GotestIcons.Linear.Star,
            R.string.leaderboard_info_points_title,
            R.string.leaderboard_info_points_body,
        ),
        InfoItem(
            GotestIcons.Linear.Calendar,
            R.string.leaderboard_info_time_title,
            R.string.leaderboard_info_time_body,
        ),
        InfoItem(
            GotestIcons.Linear.Leaderboard,
            R.string.leaderboard_info_scope_title,
            R.string.leaderboard_info_scope_body,
        ),
        InfoItem(
            GotestIcons.Linear.ArrowCircleUp,
            R.string.leaderboard_info_schedule_title,
            R.string.leaderboard_info_schedule_body,
        ),
    )

/** The static "Tentang Leaderboard" sheet (frame 21007); "Mengerti" slides it away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardInfoSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        LeaderboardInfoSheetContent(
            onConfirm = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        )
    }
}

/** Sheet body without the modal window, so previews can render it. */
@Composable
fun LeaderboardInfoSheetContent(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.leaderboard_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.leaderboard_info_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle, MaterialTheme.shapes.large)
                    .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, MaterialTheme.shapes.large)
                    .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            infoItems.forEach { item -> InfoRow(item = item) }
        }
        GotestButton(
            text = stringResource(R.string.leaderboard_info_confirm),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoRow(
    item: InfoItem,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(
            modifier =
                Modifier
                    .size(LeaderboardDimens.infoTile)
                    .background(colors.bgApp, RoundedCornerShape(LeaderboardDimens.infoTileRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = colors.iconDefault,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(item.body),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun LeaderboardInfoSheetContentPreview() {
    GotestTheme {
        LeaderboardInfoSheetContent(
            onConfirm = {},
            modifier = Modifier.padding(top = GotestTheme.spacing.xl),
        )
    }
}
