package com.gotest.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Light only: the design defines no dark palette. See README.md (TODO) before adding one.
 */
@Composable
fun GotestTheme(content: @Composable () -> Unit) {
    val colors = LightGotestColors
    CompositionLocalProvider(
        LocalGotestColors provides colors,
        LocalGotestTypography provides DefaultGotestTypography,
        LocalGotestShapes provides DefaultGotestShapes,
        LocalGotestSpacing provides GotestSpacing(),
        LocalGotestSizes provides GotestSizes(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = GotestMaterialTypography,
            shapes = GotestMaterialShapes,
            content = content,
        )
    }
}

/** Design tokens that Material's theme has no slot for. */
object GotestTheme {
    val colors: GotestColors
        @Composable @ReadOnlyComposable
        get() = LocalGotestColors.current

    val typography: GotestTypography
        @Composable @ReadOnlyComposable
        get() = LocalGotestTypography.current

    val shapes: GotestShapes
        @Composable @ReadOnlyComposable
        get() = LocalGotestShapes.current

    val spacing: GotestSpacing
        @Composable @ReadOnlyComposable
        get() = LocalGotestSpacing.current

    val sizes: GotestSizes
        @Composable @ReadOnlyComposable
        get() = LocalGotestSizes.current
}
