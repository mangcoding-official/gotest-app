package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameRules

/** "State" of the design's "MB/Code Box". */
enum class CodeBoxState { Empty, Filled, Focus, Error }

/** One character cell of a game code ("MB/Code Box"). */
@Composable
fun MbCodeBox(
    character: Char?,
    state: CodeBoxState,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.small
    val (stroke, strokeColor) =
        when (state) {
            CodeBoxState.Empty, CodeBoxState.Filled -> GotestTheme.sizes.strokeThin to colors.borderStrong
            CodeBoxState.Focus -> MultiplayerDimens.codeBoxFocusStroke to colors.borderFocus
            CodeBoxState.Error -> MultiplayerDimens.errorStroke to colors.statusDanger
        }
    val ring =
        when (state) {
            CodeBoxState.Focus -> colors.focusRing
            CodeBoxState.Error -> colors.dangerRing
            else -> null
        }
    Box(
        modifier =
            modifier
                .height(MultiplayerDimens.codeBoxHeight)
                .then(if (ring != null) Modifier.outerRing(ring, GotestTheme.spacing.xs, shape) else Modifier)
                .background(if (state == CodeBoxState.Empty) colors.bgSubtle else colors.bgApp, shape)
                .border(stroke, strokeColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = character?.toString().orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (state == CodeBoxState.Error) colors.statusDanger else colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Read-only row of six code boxes, e.g. the waiting room's game code or the hub's empty boxes. */
@Composable
fun MbCodeBoxes(
    code: String,
    modifier: Modifier = Modifier,
    spacing: Dp = GotestTheme.spacing.sm,
    isError: Boolean = false,
    focusedIndex: Int? = null,
) {
    val description = stringResource(R.string.multiplayer_cd_game_code, code.toList().joinToString(" "))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(GameRules.CODE_LENGTH) { index ->
            val character = code.getOrNull(index)
            val state =
                when {
                    isError -> CodeBoxState.Error
                    index == focusedIndex -> CodeBoxState.Focus
                    character != null -> CodeBoxState.Filled
                    else -> CodeBoxState.Empty
                }
            MbCodeBox(character = character, state = state, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Six boxes backed by one hidden text field. Accepts letters and digits only, upper-cased, and
 * highlights the next box to fill while focused.
 */
@Composable
fun MbCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val label = stringResource(R.string.multiplayer_cd_code_input)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(GameRules.normalizeCode(it)) },
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = label },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                MbCodeBoxes(
                    code = value,
                    isError = isError,
                    focusedIndex =
                        if (focused && !isError) value.length.coerceAtMost(GameRules.CODE_LENGTH - 1) else null,
                )
                // The real field stays composed (input, paste) but invisible; the boxes show its text.
                Box(modifier = Modifier.matchParentSize().alpha(0f)) { innerTextField() }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MbCodeBoxPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
                CodeBoxState.entries.forEach { state ->
                    MbCodeBox(
                        character = if (state == CodeBoxState.Empty) null else 'K',
                        state = state,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            MbCodeBoxes(code = "", spacing = GotestTheme.spacing.sm)
            MbCodeBoxes(code = "KTR4", focusedIndex = 4)
            MbCodeBoxes(code = "KTR4QX", isError = true)
        }
    }
}
