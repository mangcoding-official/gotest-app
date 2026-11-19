package com.gotest.app.core.designsystem.richtext

import com.gotest.app.core.designsystem.component.richtext.RichContent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Samples are real question content from the local bank soal (questions 1421, 1386, stimulus 544). */
class RichContentTest {
    @Test
    fun plainOptionTextStaysComposeText() {
        assertEquals(
            RichContent.Plain("Pernyataan (1) SAJA cukup."),
            RichContent.of("Pernyataan (1) SAJA cukup.", false),
        )
    }

    @Test
    fun simpleOptionHtmlStaysComposeText() {
        assertEquals(RichContent.SimpleHtml("2<br>"), RichContent.of("2<br>", false))
        assertEquals(
            RichContent.SimpleHtml("<p>Jadi, <strong>(1) tidak cukup</strong>.</p>"),
            RichContent.of("<p>Jadi, <strong>(1) tidak cukup</strong>.</p>", true),
        )
    }

    @Test
    fun katexNodesGoToTheWebView() {
        val prompt = """<p>nilai <span data-type="inline-math" data-latex="b"></span> adalah ....</p>"""
        assertEquals(RichContent.Web(prompt, true), RichContent.of(prompt, true))
        val block = """<div data-type="block-math" data-latex="x^2"></div>"""
        assertEquals(RichContent.Web(block, true), RichContent.of(block, true))
    }

    @Test
    fun dollarMathInPlainTextGoesToTheWebView() {
        assertEquals(
            RichContent.Web("Jawaban \$\\sqrt{2}\$ saja", false),
            RichContent.of("Jawaban \$\\sqrt{2}\$ saja", false),
        )
    }

    @Test
    fun singleDollarSignIsNotMath() {
        assertEquals(RichContent.Plain("Harga \$5"), RichContent.of("Harga \$5", false))
    }

    @Test
    fun imagesListsAlignmentAndHighlightsGoToTheWebView() {
        listOf(
            """<p><img src="data:image/png;base64,iVBORw0KGgo="></p>""",
            """<ul><li>satu</li></ul>""",
            """<p data-align="center">tengah</p>""",
            """<p>kata <mark>penting</mark></p>""",
            """<p><a href="https://gotest.id">tautan</a></p>""",
        ).forEach { html -> assertEquals(html, RichContent.Web(html, true), RichContent.of(html, true)) }
    }

    @Test
    fun plainTextMarkedAsHtmlIsSimpleHtml() {
        // The web renders `html` content as HTML even without tags, so entities still decode.
        assertEquals(RichContent.SimpleHtml("1 &lt; 2"), RichContent.of("1 &lt; 2", true))
    }
}
