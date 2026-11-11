package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Keeps a short label on one line, as it is on the 440dp design frame: when the line is a
 * little too long for a narrower phone, the text shrinks in small steps down to
 * [MIN_SCALE] of its design size. When even that does not fit, the text keeps its design size
 * and wraps instead of becoming illegibly small or being cut off. Give the text `maxLines`
 * of 2 or more so the fallback can wrap.
 */
internal data class SingleLineTextAutoSize(
    val fontSize: TextUnit,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        val min = fontSize.value * MIN_SCALE
        var size = fontSize.value
        while (size >= min) {
            val layout = performLayout(constraints, text, size.sp)
            if (layout.lineCount == 1 && !layout.hasVisualOverflow) return size.sp
            size -= STEP
        }
        return fontSize
    }

    private companion object {
        const val MIN_SCALE = 0.85f
        const val STEP = 0.5f
    }
}

/** [SingleLineTextAutoSize] for text drawn in [style]. */
internal fun singleLineAutoSize(style: TextStyle): TextAutoSize = SingleLineTextAutoSize(style.fontSize)
