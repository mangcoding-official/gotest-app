package com.gotest.app.core.designsystem.component.richtext

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml

/**
 * Question content (prompt, passage, option, explanation) drawn like the web's `MathContent`: cleaned
 * HTML, KaTeX math, `$...$` math in plain text, and images. Plain and simple content is Compose text;
 * the rest goes to a WebView served from app assets (see [RichContent]).
 *
 * @param isHtml true for prompts, passages and explanations (the web's `html` prop); false for option
 * text, whose HTML is detected the same way as on the web.
 * @param zoomImages lets a tap on an image open it full screen. Pass false when the content sits inside
 * something tappable (an answer option), so the tap goes to that instead.
 */
@Composable
fun RichText(
    content: String,
    modifier: Modifier = Modifier,
    isHtml: Boolean = false,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    zoomImages: Boolean = true,
) {
    val kind = remember(content, isHtml) { RichContent.of(content, isHtml) }
    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    when (kind) {
        is RichContent.Plain -> {
            Text(text = kind.text, modifier = modifier, style = style, color = textColor)
        }

        is RichContent.SimpleHtml -> {
            val text = remember(kind.html) { AnnotatedString.fromHtml(kind.html).trimmed() }
            Text(text = text, modifier = modifier, style = style, color = textColor)
        }

        is RichContent.Web -> {
            RichWebContent(
                content = kind,
                style = style,
                color = textColor,
                zoomImages = zoomImages,
                modifier = modifier,
            )
        }
    }
}

/** `<p>` and a trailing `<br>` leave line breaks at the ends; the web's layout does not show them. */
private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }
    if (start < 0) return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return subSequence(start, end)
}
