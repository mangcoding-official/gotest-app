package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Modal sheet with the design's 24dp top corners, small grey handle and 40% scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GotestBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = GotestTheme.shapes.bottomSheet,
        containerColor = GotestTheme.colors.bgApp,
        contentColor = GotestTheme.colors.textPrimary,
        scrimColor = GotestTheme.colors.scrim,
        dragHandle = { GotestSheetHandle() },
        content = content,
    )
}

@Composable
private fun GotestSheetHandle(modifier: Modifier = Modifier) {
    val sizes = GotestTheme.sizes
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = GotestTheme.spacing.sm, bottom = GotestTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(sizes.sheetHandleWidth, sizes.sheetHandleHeight)
                    .background(GotestTheme.colors.bgGrey, GotestTheme.shapes.full),
        )
    }
}
