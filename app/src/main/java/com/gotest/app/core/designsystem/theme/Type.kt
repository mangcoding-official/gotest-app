package com.gotest.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.gotest.app.R

val InterFontFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
    )

val BbhBogleFontFamily = FontFamily(Font(R.font.bbh_bogle_regular, FontWeight.Normal))

// Figma centres text inside its line box; Compose needs this to do the same.
private val CenteredLineHeight =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

private val TightTracking = (-1).sp

// Phones are narrower than the 440dp design frame, so headings that fit one line there wrap
// here. Balanced breaking splits them into even lines instead of leaving one word behind.
// Paragraphs keep the default breaker, which fills each line.
private val HeadingLineBreak = LineBreak.Heading

private fun inter(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f,
    lineBreak: LineBreak = LineBreak.Unspecified,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = CenteredLineHeight,
    lineBreak = lineBreak,
)

private fun bbhBogle(
    size: Int,
    lineHeight: Int,
) = TextStyle(
    fontFamily = BbhBogleFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    lineHeightStyle = CenteredLineHeight,
    lineBreak = HeadingLineBreak,
)

/** heading/xl–md: Inter Medium with tight tracking. */
private fun heading(
    size: Int,
    lineHeight: Int,
) = inter(
    FontWeight.Medium,
    size = size,
    lineHeight = lineHeight,
    letterSpacing = TightTracking.value,
    lineBreak = HeadingLineBreak,
)

/** Material roles mapped to the design's text styles (names in comments). */
internal val GotestMaterialTypography =
    Typography(
        // Display/lg, Display/md, Display/sm
        displayLarge = bbhBogle(size = 64, lineHeight = 64),
        displayMedium = bbhBogle(size = 48, lineHeight = 48),
        displaySmall = bbhBogle(size = 40, lineHeight = 40),
        // heading/xl, heading/lg, heading/md
        headlineLarge = heading(size = 32, lineHeight = 32),
        headlineMedium = heading(size = 24, lineHeight = 24),
        headlineSmall = heading(size = 20, lineHeight = 20),
        // Body/bold/lg, heading/sm, Body/bold/md
        titleLarge = inter(FontWeight.Medium, size = 18, lineHeight = 32),
        titleMedium =
            inter(
                FontWeight.SemiBold,
                size = 16,
                lineHeight = 16,
                letterSpacing = TightTracking.value,
                lineBreak = HeadingLineBreak,
            ),
        titleSmall = inter(FontWeight.Medium, size = 16, lineHeight = 24),
        // Body/Regular/md, Body/Regular/sm, Body/Regular/xs
        bodyLarge = inter(FontWeight.Normal, size = 16, lineHeight = 24),
        bodyMedium = inter(FontWeight.Normal, size = 14, lineHeight = 20),
        bodySmall = inter(FontWeight.Normal, size = 12, lineHeight = 20),
        // Body/bold/sm, Body/bold/xs, label-and-caption/Regular/caption
        labelLarge = inter(FontWeight.Medium, size = 14, lineHeight = 20),
        labelMedium = inter(FontWeight.Medium, size = 12, lineHeight = 20),
        labelSmall = inter(FontWeight.Normal, size = 12, lineHeight = 18),
    )

/** Styles the design uses that have no Material role. */
@Immutable
data class GotestTypography(
    /** BBH Bogle 32/34, the Upgrade headline. */
    val displayExtraSmall: TextStyle,
    /** Button label, large and medium buttons. */
    val buttonLarge: TextStyle,
    /** Button label, small buttons. */
    val buttonSmall: TextStyle,
    /** Body/Regular/lg. */
    val bodyExtraLarge: TextStyle,
    /** Medium 12/16, used by compact list metadata. */
    val labelCompact: TextStyle,
)

internal val DefaultGotestTypography =
    GotestTypography(
        displayExtraSmall = bbhBogle(size = 32, lineHeight = 34),
        buttonLarge = inter(FontWeight.Medium, size = 14, lineHeight = 16),
        buttonSmall = inter(FontWeight.Medium, size = 12, lineHeight = 16),
        bodyExtraLarge = inter(FontWeight.Normal, size = 18, lineHeight = 32),
        labelCompact = inter(FontWeight.Medium, size = 12, lineHeight = 16),
    )

internal val LocalGotestTypography = staticCompositionLocalOf { DefaultGotestTypography }
