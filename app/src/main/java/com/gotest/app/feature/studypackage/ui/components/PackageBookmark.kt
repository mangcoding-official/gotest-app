package com.gotest.app.feature.studypackage.ui.components

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

// "Lihat" in the toast is a raw #CCC1F0 in the design, with no colour variable behind it.
private val ToastActionColor = Color(0xFFCCC1F0)

/** Top-right bookmark of Detail Paket / Detail Tryout: outline when free, filled once saved. */
@Composable
internal fun BookmarkButton(
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestIconButton(
        icon = if (isSaved) GotestIcons.Bold.ArchiveTick else GotestIcons.Linear.ArchiveAdd,
        contentDescription =
            stringResource(if (isSaved) R.string.studypackage_cd_unbookmark else R.string.studypackage_cd_bookmark),
        onClick = onClick,
        modifier = modifier,
    )
}

/** The designed "Paket disimpan" toast; [actionLabel] ("Lihat") opens Paket Tersimpan. */
internal data class SavedToastVisuals(
    override val message: String,
    val supportingText: String,
    override val actionLabel: String,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * Replaces any message on screen with the result of a bookmark tap: the "Paket disimpan" toast
 * after saving, whose "Lihat" calls [onOpenSaved], or a plain message after removing.
 */
internal suspend fun SnackbarHostState.showBookmarkResult(
    saved: Boolean,
    resources: Resources,
    onOpenSaved: () -> Unit,
) {
    currentSnackbarData?.dismiss()
    if (saved) {
        val result =
            showSnackbar(
                SavedToastVisuals(
                    message = resources.getString(R.string.studypackage_saved_toast_title),
                    supportingText = resources.getString(R.string.studypackage_saved_toast_message),
                    actionLabel = resources.getString(R.string.studypackage_saved_toast_action),
                ),
            )
        if (result == SnackbarResult.ActionPerformed) onOpenSaved()
    } else {
        showSnackbar(resources.getString(R.string.studypackage_unsaved_message))
    }
}

/**
 * Snackbar host of the Paket screens. Every message uses the dark toast from the "Tersimpan"
 * frames; only [SavedToastVisuals] adds the icon, second line and "Lihat".
 */
@Composable
internal fun PackageSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val visuals = data.visuals
        if (visuals is SavedToastVisuals) {
            PackageToast(
                title = visuals.message,
                message = visuals.supportingText,
                icon = GotestIcons.Bold.ArchiveTick,
                actionText = visuals.actionLabel,
                onAction = data::performAction,
            )
        } else {
            PackageToast(title = visuals.message)
        }
    }
}

/** Dark brand toast with an optional leading [icon], second line and trailing text action. */
@Composable
internal fun PackageToast(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    @DrawableRes icon: Int? = null,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.bgBrandDark,
        contentColor = colors.textOnBrand,
        shadowElevation = StudyPackageDimens.toastElevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm + spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconMd),
                    tint = colors.iconOnBrand,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                message?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
            actionText?.let {
                Text(
                    text = it,
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(role = Role.Button, onClick = onAction),
                    style = MaterialTheme.typography.labelLarge,
                    color = ToastActionColor,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun PackageToastPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            PackageToast(
                title = "Paket disimpan",
                message = "Buka kapan saja dari Paket Tersimpan.",
                icon = GotestIcons.Bold.ArchiveTick,
                actionText = "Lihat",
            )
            PackageToast(title = "Paket dihapus dari Paket Tersimpan.")
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                BookmarkButton(isSaved = false, onClick = {})
                BookmarkButton(isSaved = true, onClick = {})
            }
        }
    }
}
