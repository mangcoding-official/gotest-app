package com.gotest.app.core.designsystem.component.richtext

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The WebView behind [RichText]: it loads `assets/richtext/index.html` through [WebViewAssetLoader] (so
 * the page, KaTeX and the app font share one https origin and the page's CSP can allow only its own
 * scripts), then draws each piece of content with `GotestRich.render`.
 *
 * It takes no touches: a tap on an option's text still selects the option, and a drag over a question
 * still scrolls the screen. Image taps are forwarded from Compose through [tapAt].
 */
@SuppressLint("ViewConstructor")
internal class RichWebView(
    context: Context,
    private val onHeight: (Int) -> Unit,
    private val onImage: (String) -> Unit,
) : WebView(context) {
    private var pageReady = false
    private var pendingScript: String? = null
    private var shownScript: String? = null

    init {
        configure()
        val loader =
            WebViewAssetLoader
                .Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(context))
                .build()
        webViewClient = RichWebViewClient(loader) { onPageReady() }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(this, BRIDGE, setOf(ORIGIN)) { _, message, _, _, _ ->
                message.data?.let(::handleMessage)
            }
        }
        loadUrl(PAGE_URL)
    }

    // The page only runs its own bundled scripts (KaTeX and renderer.js); content scripts are blocked by
    // the server's sanitizer and again by the page's Content-Security-Policy.
    @SuppressLint("SetJavaScriptEnabled")
    private fun configure() {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
        settings.textZoom = TEXT_ZOOM_PERCENT
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
    }

    /** Draws [raw] with the given theme [style] (CSS custom properties); repeated calls are free. */
    fun show(
        raw: String,
        isHtml: Boolean,
        baseUrl: String,
        style: Map<String, String>,
    ) {
        val styleJson = JsonObject(style.mapValues { JsonPrimitive(it.value) })
        val script =
            "GotestRich.render(${JsonPrimitive(raw)}, $isHtml, ${JsonPrimitive(baseUrl)}, $styleJson)"
        if (script == shownScript) return
        shownScript = script
        if (pageReady) run(script) else pendingScript = script
    }

    /** A tap at ([x], [y]) in CSS px (dp): an image under it is reported through [onImage]. */
    fun tapAt(
        x: Float,
        y: Float,
    ) {
        if (pageReady) evaluateJavascript("GotestRich.tapAt($x, $y)", null)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false

    private fun onPageReady() {
        pageReady = true
        pendingScript?.let(::run)
        pendingScript = null
    }

    private fun run(script: String) {
        evaluateJavascript(script, null)
        // The first height also arrives without the message listener (old WebView versions).
        evaluateJavascript(HEIGHT_SCRIPT) { value -> value?.toFloatOrNull()?.let { onHeight(it.toInt()) } }
    }

    private fun handleMessage(data: String) {
        val message = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return
        when (message["type"]?.jsonPrimitive?.contentOrNull) {
            "height" -> message["value"]?.jsonPrimitive?.intOrNull?.let(onHeight)
            "image" -> message["src"]?.jsonPrimitive?.contentOrNull?.let(onImage)
        }
    }

    private class RichWebViewClient(
        private val loader: WebViewAssetLoader,
        private val onReady: () -> Unit,
    ) : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

        /** The content is read, not browsed: no link or redirect ever navigates away. */
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean = true

        override fun onPageFinished(
            view: WebView,
            url: String,
        ) {
            if (url == PAGE_URL) onReady()
        }
    }

    private companion object {
        const val ORIGIN = "https://appassets.androidplatform.net"
        const val PAGE_URL = "$ORIGIN/assets/richtext/index.html"
        const val BRIDGE = "GotestBridge"
        const val HEIGHT_SCRIPT = "Math.ceil(document.getElementById('content').getBoundingClientRect().height)"

        /** Text size comes from the app theme (already scaled by the user's font size) through CSS. */
        const val TEXT_ZOOM_PERCENT = 100
    }
}
