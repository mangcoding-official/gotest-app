package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** "variant" property of the design's Button component. */
enum class GotestButtonVariant {
    /** Dark brand fill; the primary action. */
    Filled,

    /** White fill with a hairline border; secondary actions. */
    Plain,

    /** No container ("nobackground"); tertiary and inline actions. */
    Text,
}

/** "Size" property of the design's Button component. */
enum class GotestButtonSize { Large, Medium, Small }

@Composable
fun GotestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GotestButtonVariant = GotestButtonVariant.Filled,
    size: GotestButtonSize = GotestButtonSize.Large,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val colors = GotestTheme.colors
    val metrics = buttonMetrics(size)
    val border =
        if (enabled && variant == GotestButtonVariant.Plain && containerColor == null) {
            BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle)
        } else {
            null
        }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = containerColorFor(variant, enabled, containerColor),
        contentColor = contentColorFor(variant, enabled, contentColor),
        border = border,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = metrics.minHeight)
                    .padding(vertical = metrics.padding)
                    .shrinkableHorizontalPadding(max = metrics.padding, min = GotestTheme.spacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { ButtonIcon(it) }
            Text(
                text = text,
                // Design pads labels 8dp; medium/small buttons use 4dp so three fit in a row on 360–412dp phones.
                // On narrow phones the padding gives way first, then the label shrinks a little to stay on one line.
                modifier = Modifier.shrinkableHorizontalPadding(max = metrics.labelPadding),
                style = metrics.textStyle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                autoSize = singleLineAutoSize(metrics.textStyle),
            )
            trailingIcon?.let { ButtonIcon(it) }
        }
    }
}

private data class ButtonMetrics(
    val minHeight: Dp,
    val padding: Dp,
    val textStyle: TextStyle,
    val labelPadding: Dp,
)

@Composable
private fun buttonMetrics(size: GotestButtonSize): ButtonMetrics {
    val sizes = GotestTheme.sizes
    val spacing = GotestTheme.spacing
    val typography = GotestTheme.typography
    return when (size) {
        GotestButtonSize.Large -> {
            ButtonMetrics(
                minHeight = sizes.buttonLarge,
                padding = spacing.md,
                textStyle = typography.buttonLarge,
                labelPadding = spacing.sm,
            )
        }

        GotestButtonSize.Medium -> {
            ButtonMetrics(
                minHeight = sizes.buttonMedium,
                padding = spacing.sm,
                textStyle = typography.buttonLarge,
                labelPadding = spacing.xs,
            )
        }

        GotestButtonSize.Small -> {
            ButtonMetrics(
                minHeight = sizes.buttonSmall,
                padding = spacing.sm,
                textStyle = typography.buttonSmall,
                labelPadding = spacing.xs,
            )
        }
    }
}

@Composable
private fun containerColorFor(
    variant: GotestButtonVariant,
    enabled: Boolean,
    override: Color?,
): Color {
    val colors = GotestTheme.colors
    return when {
        !enabled && variant != GotestButtonVariant.Text -> colors.disabledContainer
        override != null -> override
        variant == GotestButtonVariant.Filled -> colors.bgBrandDark
        variant == GotestButtonVariant.Plain -> colors.bgApp
        else -> Color.Transparent
    }
}

@Composable
private fun contentColorFor(
    variant: GotestButtonVariant,
    enabled: Boolean,
    override: Color?,
): Color {
    val colors = GotestTheme.colors
    return when {
        !enabled && variant == GotestButtonVariant.Filled -> colors.disabledContent
        !enabled -> colors.disabledContentMuted
        override != null -> override
        variant == GotestButtonVariant.Filled -> colors.textOnBrand
        variant == GotestButtonVariant.Plain -> colors.textPrimary
        else -> colors.textBrandDark
    }
}

@Composable
private fun ButtonIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = modifier.size(GotestTheme.sizes.iconMd),
    )
}

@Preview(showBackground = true)
@Composable
private fun GotestButtonPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            GotestButton(text = "Masuk", onClick = {}, modifier = Modifier.fillMaxWidth())
            GotestButton(text = "Daftar Akun Baru", onClick = {
            }, modifier = Modifier.fillMaxWidth(), variant = GotestButtonVariant.Plain)
            GotestButton(text = "Lanjutkan", onClick = {
            }, size = GotestButtonSize.Small, trailingIcon = GotestIcons.Linear.ArrowRight)
            GotestButton(
                text = "Lihat Semua",
                onClick = {},
                variant = GotestButtonVariant.Text,
                size = GotestButtonSize.Small,
            )
            GotestButton(text = "Mulai Permainan", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        }
    }
}
