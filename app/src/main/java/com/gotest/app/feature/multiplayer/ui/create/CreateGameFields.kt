package com.gotest.app.feature.multiplayer.ui.create

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.outerRing

/** "Jenis permainan" option: icon tile, title, subtitle and a radio indicator. */
@Composable
fun GameTypeCard(
    title: String,
    subtitle: String,
    @DrawableRes selectedIcon: Int,
    @DrawableRes idleIcon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.small
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (selected) {
                        Modifier.outerRing(
                            colors.focusRing,
                            GotestTheme.sizes.focusRingWidth,
                            shape,
                        )
                    } else {
                        Modifier
                    },
                ).background(colors.bgApp, shape)
                .border(GotestTheme.sizes.strokeThin, if (selected) colors.bgBrand else colors.borderSubtle, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(GotestTheme.sizes.iconButton)
                    .background(if (selected) colors.bgBrandSoft else colors.bgSubtle, MaterialTheme.shapes.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (selected) selectedIcon else idleIcon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = if (selected) colors.iconBrandDark else colors.iconDefault,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.textBrandDark else colors.textPrimary,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        RadioIndicator(selected = selected)
    }
}

@Composable
private fun RadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Box(
        modifier =
            modifier
                .size(GotestTheme.sizes.iconMd)
                .background(colors.bgApp, GotestTheme.shapes.full)
                .border(
                    width = if (selected) MultiplayerDimens.radioSelectedStroke else MultiplayerDimens.radioStroke,
                    color = if (selected) colors.bgBrandDark else colors.borderStrong,
                    shape = GotestTheme.shapes.full,
                ),
    )
}

/** Field with a chevron that opens a menu of [options] ("Kategori", "Subtes (wajib)"). */
@Composable
fun SelectField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = GotestTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
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
                        text = value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                    Icon(
                        painter = painterResource(GotestIcons.Linear.ChevronDown),
                        contentDescription = null,
                        modifier = Modifier.size(GotestTheme.sizes.iconMd),
                        tint = colors.iconDefault,
                    )
                }
            }
            OptionsMenu(
                expanded = expanded,
                options = options,
                onSelect = {
                    expanded = false
                    onSelect(it)
                },
                onDismiss = { expanded = false },
            )
        }
    }
}

@Composable
private fun OptionsMenu(
    expanded: Boolean,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = GotestTheme.colors.bgApp,
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GotestTheme.colors.textPrimary,
                    )
                },
                onClick = { onSelect(option) },
            )
        }
    }
}

/** "Topik": chosen topics with a remove icon, plus "+ Tambah topik". */
@Composable
fun TopicPicker(
    selected: List<String>,
    addable: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .border(GotestTheme.sizes.strokeThin, colors.borderStrong, MaterialTheme.shapes.small)
                .padding(horizontal = spacing.md, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        selected.forEach { topic -> RemovableTopicChip(text = topic, onRemove = { onRemove(topic) }) }
        if (addable.isNotEmpty()) {
            Box {
                Surface(onClick = { expanded = true }, color = colors.bgApp, shape = GotestTheme.shapes.full) {
                    Text(
                        text = stringResource(R.string.multiplayer_create_add_topic),
                        modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.sm),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textBrandDark,
                    )
                }
                OptionsMenu(
                    expanded = expanded,
                    options = addable,
                    onSelect = {
                        expanded = false
                        onAdd(it)
                    },
                    onDismiss = { expanded = false },
                )
            }
        }
    }
}

@Composable
private fun RemovableTopicChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val description = stringResource(R.string.multiplayer_cd_remove_topic, text)
    Surface(
        onClick = onRemove,
        modifier = modifier.semantics { contentDescription = description },
        shape = GotestTheme.shapes.full,
        color = colors.illustrationBrandSoft,
        contentColor = colors.illustrationBrandDark,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.CloseCircle),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconSm),
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** "Jumlah soal": minus, value and plus. The value turns red when the stock is short. */
@Composable
fun QuestionStepper(
    count: Int,
    isError: Boolean,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = GotestIcons.Linear.Minus,
            contentDescription = stringResource(R.string.multiplayer_cd_fewer_questions),
            enabled = canDecrease,
            onClick = onDecrease,
        )
        Box(
            modifier =
                Modifier
                    .width(MultiplayerDimens.stepperValueWidth)
                    .heightIn(min = GotestTheme.sizes.inputHeight)
                    .then(
                        if (isError) Modifier.outerRing(colors.dangerRing, GotestTheme.spacing.xs, shape) else Modifier,
                    ).background(colors.bgApp, shape)
                    .border(
                        width = if (isError) MultiplayerDimens.errorStroke else GotestTheme.sizes.strokeThin,
                        color = if (isError) colors.statusDanger else colors.borderStrong,
                        shape = shape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isError) colors.statusDanger else colors.textPrimary,
            )
        }
        StepperButton(
            icon = GotestIcons.Linear.Add,
            contentDescription = stringResource(R.string.multiplayer_cd_more_questions),
            enabled = canIncrease,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepperButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = colors.bgApp,
        contentColor = if (enabled) colors.iconDefault else colors.iconMuted,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderStrong),
    ) {
        Box(modifier = Modifier.size(GotestTheme.sizes.inputHeight), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
            )
        }
    }
}

/** "Durasi" choice; disabled when it leaves too little time per question. */
@Composable
fun DurationChip(
    minutes: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val (container, content) =
        when {
            selected -> colors.bgBrandDark to colors.bgApp
            !enabled -> colors.bgSubtle to colors.textPlaceholder
            else -> colors.bgApp to colors.textPrimary
        }
    Surface(
        modifier =
            modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(GotestTheme.sizes.strokeThin, colors.borderStrong),
    ) {
        Text(
            text = durationLabel(minutes),
            modifier =
                Modifier.padding(
                    horizontal = GotestTheme.spacing.md + GotestTheme.spacing.xxs,
                    vertical = GotestTheme.spacing.sm + GotestTheme.spacing.xxs,
                ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateGameFieldsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            GameTypeCard(
                title = "Duel",
                subtitle = "1 lawan 1 • tepat 2 pemain",
                selectedIcon = GotestIcons.Bold.User,
                idleIcon = GotestIcons.Linear.User,
                selected = true,
                onClick = {},
            )
            SelectField(label = "Kategori", value = "UTBK-SNBT", options = listOf("UTBK-SNBT"), onSelect = {})
            TopicPicker(
                selected = listOf("Aljabar", "Logaritma"),
                addable = listOf("Peluang"),
                onAdd = {},
                onRemove = {},
            )
            QuestionStepper(
                count = 30,
                isError = true,
                canDecrease = true,
                canIncrease = false,
                onDecrease = {},
                onIncrease = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                DurationChip(minutes = 10, selected = false, enabled = false, onClick = {})
                DurationChip(minutes = 15, selected = true, enabled = true, onClick = {})
                DurationChip(minutes = 20, selected = false, enabled = true, onClick = {})
            }
        }
    }
}
