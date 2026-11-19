package com.gotest.app.core.designsystem.component.richtext

/**
 * How one piece of question content (prompt, passage, option, explanation) is drawn.
 *
 * The server sends cleaned HTML (RichTextSanitizer) with KaTeX math nodes, and option text may still be
 * plain text with `$...$` math, exactly as the web's `MathContent` receives it. Most options are short
 * plain text, so only content that needs a browser (math, images, lists, alignment, highlights, links)
 * goes to the WebView; the rest is drawn as Compose text, which is faster and keeps screens light.
 */
sealed interface RichContent {
    /** Text without markup, shown as is. */
    data class Plain(
        val text: String,
    ) : RichContent

    /** Inline HTML that Compose can draw itself (paragraphs, breaks, bold, italic, sub, sup). */
    data class SimpleHtml(
        val html: String,
    ) : RichContent

    /** Content drawn by the KaTeX page in a WebView, the same way the web draws it. */
    data class Web(
        val raw: String,
        val isHtml: Boolean,
    ) : RichContent

    companion object {
        /** The tags the web treats as HTML (`containsHtml` in MathContent.tsx). */
        private val HTML_TAG =
            Regex(
                """</?(p|br|strong|b|em|i|u|s|sub|sup|ul|ol|li|h2|h3|blockquote|a|mark|hr|span|div|img)\b""",
                RegexOption.IGNORE_CASE,
            )

        /** Markup that Compose text cannot draw like the web does. */
        private val NEEDS_BROWSER =
            Regex(
                """data-type\s*=\s*["'](inline|block)-math|data-align|""" +
                    """</?(img|ul|ol|li|blockquote|h2|h3|hr|mark|a|table)\b""",
                RegexOption.IGNORE_CASE,
            )

        /** `$...$` or `$$...$$`, which the web renders as math in text (`renderDollarMath`). */
        private val DOLLAR_MATH = Regex("""\$[^$]+\$""")

        /**
         * [isHtml] mirrors the web's `html` prop: true for prompts, passages and explanations; false lets
         * option text be detected, as the web does with its `text` prop.
         */
        fun of(
            raw: String,
            isHtml: Boolean,
        ): RichContent {
            val hasHtml = isHtml || HTML_TAG.containsMatchIn(raw)
            return when {
                NEEDS_BROWSER.containsMatchIn(raw) || DOLLAR_MATH.containsMatchIn(raw) -> Web(raw, isHtml)
                hasHtml -> SimpleHtml(raw)
                else -> Plain(raw)
            }
        }
    }
}
