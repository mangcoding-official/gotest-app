package com.gotest.app.core.designsystem.component.richtext

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import com.gotest.app.BuildConfig
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme

/** [RichContent.Web] in a [RichWebView] that grows to the height of its content. */
@Composable
internal fun RichWebContent(
    content: RichContent.Web,
    style: TextStyle,
    color: Color,
    zoomImages: Boolean,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        // Previews cannot run a WebView; show the source so the layout is still visible.
        Text(text = content.raw, modifier = modifier, style = style, color = color)
        return
    }
    val density = LocalDensity.current
    val colors = GotestTheme.colors
    val cssStyle = remember(style, color, colors, density) { cssStyle(style, color, colors, density) }
    var heightDp by remember { mutableIntStateOf(0) }
    var zoomedImage by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<RichWebView?>(null) }
    val currentZoom by rememberUpdatedState(zoomImages)

    AndroidView(
        factory = { context ->
            RichWebView(
                context = context,
                onHeight = { heightDp = it },
                onImage = { src -> if (currentZoom) zoomedImage = src },
            ).also { webView = it }
        },
        update = { it.show(content.raw, content.isHtml, BuildConfig.API_BASE_URL, cssStyle) },
        onRelease = { it.destroy() },
        modifier =
            modifier
                .fillMaxWidth()
                .height(maxOf(heightDp, 1).dp)
                .then(
                    if (zoomImages) {
                        Modifier.pointerInput(density) {
                            detectTapGestures { offset ->
                                webView?.tapAt(offset.x / density.density, offset.y / density.density)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
    )

    zoomedImage?.let { src -> ImageZoomDialog(source = src, onDismiss = { zoomedImage = null }) }
}

/** Theme values as the CSS custom properties read by `rich.css`. CSS px in the WebView equal dp. */
private fun cssStyle(
    style: TextStyle,
    color: Color,
    colors: GotestColors,
    density: Density,
): Map<String, String> =
    buildMap {
        put("text", color.css())
        put("muted", colors.textSecondary.css())
        put("link", colors.textBrandDark.css())
        put("border", colors.borderStrong.css())
        style.fontSize.cssPx(density)?.let { put("font-size", it) }
        style.lineHeight.cssPx(density)?.let { put("line-height", it) }
    }

private fun TextUnit.cssPx(density: Density): String? =
    if (isSpecified && type == androidx.compose.ui.unit.TextUnitType.Sp) "${value * density.fontScale}px" else null

private fun Color.css(): String {
    val argb = toArgb()
    val alpha = (argb ushr ALPHA_SHIFT) / MAX_CHANNEL.toFloat()
    return "rgba(${argb shr RED_SHIFT and MAX_CHANNEL}, ${argb shr GREEN_SHIFT and MAX_CHANNEL}, " +
        "${argb and MAX_CHANNEL}, $alpha)"
}

private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val MAX_CHANNEL = 0xFF
