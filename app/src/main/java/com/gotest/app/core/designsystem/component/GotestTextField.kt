package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

private const val DISABLED_ALPHA = 0.5f

/**
 * The design's "input" component: label, 44dp field with optional icons, and a helper line.
 * [isError] switches the field and helper to the "danger" variant.
 */
@Composable
fun GotestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = GotestTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().fieldFrame(focused = focused, isError = isError, enabled = enabled),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = if (isError) colors.statusDanger else colors.textPrimary,
                ),
            cursorBrush = SolidColor(colors.borderFocus),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                FieldDecoration(
                    showPlaceholder = value.isEmpty(),
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingContent = trailingContent,
                    innerTextField = innerTextField,
                )
            },
        )
        if (supportingText != null) {
            SupportingLine(text = supportingText, isError = isError)
        }
    }
}

/** Background, hairline border and the outer focus/danger ring of the field box. */
@Composable
private fun Modifier.fieldFrame(
    focused: Boolean,
    isError: Boolean,
    enabled: Boolean,
): Modifier {
    val colors = GotestTheme.colors
    val sizes = GotestTheme.sizes
    val shape = MaterialTheme.shapes.small
    val borderColor =
        when {
            !enabled -> Color.Transparent
            isError -> colors.borderDanger
            focused -> colors.borderFocus
            else -> colors.borderSubtle
        }
    val ringColor =
        when {
            isError -> colors.dangerRing
            focused -> colors.focusRing
            else -> Color.Transparent
        }
    return drawBehind {
        // The design draws the ring as an outer spread that takes no layout space.
        val ring = sizes.focusRingWidth.toPx()
        val outline = shape.createOutline(Size(size.width + ring * 2, size.height + ring * 2), layoutDirection, this)
        translate(-ring, -ring) { drawOutline(outline, ringColor) }
    }.background(if (enabled) colors.bgApp else colors.bgSubtle, shape)
        .border(sizes.strokeThin, borderColor, shape)
}

@Composable
private fun FieldDecoration(
    showPlaceholder: Boolean,
    placeholder: String?,
    @DrawableRes leadingIcon: Int?,
    trailingContent: (@Composable () -> Unit)?,
    innerTextField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = GotestTheme.sizes
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .heightIn(min = sizes.inputHeight)
                .padding(horizontal = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(sizes.iconMd),
                tint = GotestTheme.colors.iconDefault,
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (showPlaceholder && placeholder != null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GotestTheme.colors.textPlaceholder,
                )
            }
            innerTextField()
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun SupportingLine(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isError) GotestTheme.colors.statusDanger else GotestTheme.colors.textPlaceholder
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.InfoCircle),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = color,
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/** [GotestTextField] with the masked text and eye toggle used by every password field. */
@Composable
fun GotestPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    GotestTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        leadingIcon = GotestIcons.Linear.Lock,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardActions = keyboardActions,
        trailingContent = {
            // Smaller than 48dp on purpose to keep the 44dp field; Compose widens the touch area.
            Icon(
                painter = painterResource(GotestIcons.Linear.Eye),
                contentDescription =
                    stringResource(
                        if (visible) R.string.cd_hide_password else R.string.cd_show_password,
                    ),
                modifier =
                    Modifier
                        .size(GotestTheme.sizes.iconMd)
                        .clickable(role = Role.Button) { visible = !visible },
                tint = if (visible) GotestTheme.colors.iconBrandDark else GotestTheme.colors.iconDefault,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun GotestTextFieldPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            GotestTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "nama@email.com",
                leadingIcon = GotestIcons.Linear.User,
            )
            GotestPasswordField(value = "rahasia1", onValueChange = {}, label = "Password")
            GotestTextField(
                value = "gotest",
                onValueChange = {},
                label = "Konfirmasi",
                supportingText = "Teks tidak sesuai",
                isError = true,
            )
        }
    }
}
