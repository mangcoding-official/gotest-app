package com.gotest.app.feature.assistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

private const val INPUT_MAX_LINES = 4

/**
 * Bottom "composer": optional prompt chips (conversation only) above the "Mobile/Chat Input"
 * pill. The round button sends when there is text and opens the voice overlay otherwise.
 */
@Composable
internal fun AssistantComposer(
    input: String,
    prompts: List<String>,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onPromptClick: (String) -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = GotestTheme.colors.bgApp,
            border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                if (prompts.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(prompts, key = { it }) { prompt ->
                            AssistantPromptChip(
                                text = prompt,
                                onClick = { onPromptClick(prompt) },
                                containerColor = GotestTheme.colors.bgSubtle,
                            )
                        }
                    }
                }
                ChatInput(
                    input = input,
                    focusRequester = focusRequester,
                    onInputChange = onInputChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                    onAttachClick = onAttachClick,
                )
            }
        }
    }
}

@Composable
private fun ChatInput(
    input: String,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val canSend = input.isNotBlank()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = GotestTheme.shapes.full,
        color = colors.bgApp,
        border =
            if (focused) {
                BorderStroke(AssistantDimens.chatInputFocusedBorder, colors.borderFocus)
            } else {
                BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
            },
        shadowElevation = GotestTheme.sizes.elevationShadow,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = AssistantDimens.chatInputMinHeight)
                    .padding(start = GotestTheme.spacing.xs, end = GotestTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.AddCircle),
                contentDescription = stringResource(R.string.assistant_attach),
                modifier =
                    Modifier
                        .clip(GotestTheme.shapes.full)
                        .clickable(role = Role.Button, onClick = onAttachClick)
                        .minimumInteractiveComponentSize()
                        .size(AssistantDimens.chatInputIcon),
                tint = colors.iconDefault,
            )
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.borderFocus),
                maxLines = INPUT_MAX_LINES,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (input.isEmpty()) {
                            Text(
                                text = stringResource(R.string.assistant_input_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPlaceholder,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Surface(
                onClick = if (canSend) onSend else onMicClick,
                shape = GotestTheme.shapes.full,
                color = colors.bgBrandDark,
                contentColor = colors.iconOnBrand,
            ) {
                Box(modifier = Modifier.size(AssistantDimens.sendButton), contentAlignment = Alignment.Center) {
                    Icon(
                        painter =
                            painterResource(if (canSend) GotestIcons.Bold.Send else GotestIcons.Linear.Microphone),
                        contentDescription =
                            stringResource(if (canSend) R.string.assistant_send else R.string.assistant_voice_start),
                        modifier = Modifier.size(GotestTheme.sizes.iconMd),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantComposerPreview() {
    GotestTheme {
        Column {
            AssistantComposer(
                input = "",
                prompts = listOf("Latihan yang belum selesai", "Lihat peringkatku", "Tampilkan tryout"),
                focusRequester = remember { FocusRequester() },
                onInputChange = {},
                onSend = {},
                onPromptClick = {},
                onMicClick = {},
                onAttachClick = {},
            )
            AssistantComposer(
                input = "Rekomendasi latihan aljabar",
                prompts = emptyList(),
                focusRequester = remember { FocusRequester() },
                onInputChange = {},
                onSend = {},
                onPromptClick = {},
                onMicClick = {},
                onAttachClick = {},
            )
        }
    }
}
