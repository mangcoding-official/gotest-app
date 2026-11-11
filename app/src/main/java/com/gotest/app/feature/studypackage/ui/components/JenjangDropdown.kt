package com.gotest.app.feature.studypackage.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * The design's "Semua Jenjang" select: an input-styled field with a chevron that opens a menu.
 * [selectedIndex] null means the "all" option ([allOptionLabel]); pass a null label to hide it.
 */
@Composable
internal fun JenjangDropdown(
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    allOptionLabel: String? = stringResource(R.string.studypackage_all_levels),
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val selectedLabel = selectedIndex?.let(options::getOrNull)
    val colors = GotestTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { anchorWidthPx = it.width }
                    .semantics { role = Role.DropdownList },
            shape = MaterialTheme.shapes.small,
            color = colors.bgApp,
            border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
        ) {
            Row(
                modifier =
                    Modifier
                        .heightIn(min = GotestTheme.sizes.inputHeight)
                        .padding(horizontal = GotestTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel ?: allOptionLabel.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedLabel == null) colors.textPlaceholder else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(GotestIcons.Bold.ChevronDown),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconMd),
                    tint = colors.iconMuted,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { anchorWidthPx.toDp() }),
            shape = MaterialTheme.shapes.small,
            containerColor = colors.bgApp,
        ) {
            if (allOptionLabel != null) {
                JenjangMenuItem(
                    label = allOptionLabel,
                    selected = selectedIndex == null,
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
            }
            options.forEachIndexed { index, label ->
                JenjangMenuItem(
                    label = label,
                    selected = index == selectedIndex,
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun JenjangMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        modifier = modifier,
        text = {
            Text(
                text = label,
                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = if (selected) GotestTheme.colors.textBrandDark else GotestTheme.colors.textPrimary,
            )
        },
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun JenjangDropdownPreview() {
    GotestTheme {
        JenjangDropdown(
            options = listOf("Perguruan Tinggi", "SLTA / Sederajat"),
            selectedIndex = null,
            onSelect = {},
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}
