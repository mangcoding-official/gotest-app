package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.gotest.app.BuildConfig
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * Circular profile picture. [model] is anything Coil accepts: a drawable id for the dummy avatars, a picked or
 * captured photo, or the server's photo address.
 */
@Composable
fun GotestAvatar(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = GotestTheme.sizes.avatarMd,
) {
    val shape = GotestTheme.shapes.full
    AsyncImage(
        model = serverImage(model, BuildConfig.API_BASE_URL),
        contentDescription = contentDescription,
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .background(GotestTheme.colors.bgBrandSoft)
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, shape),
        contentScale = ContentScale.Crop,
    )
}

/**
 * The server sends a stored photo as `/storage/avatars/…` (`User::avatar_url`), which the web opens on its own
 * address; the app joins it with the server's address, or the photo never loads (T-026 A2a, staging test).
 */
internal fun serverImage(
    model: Any?,
    baseUrl: String,
): Any? =
    (model as? String)
        ?.takeIf { it.startsWith("/") && !it.startsWith("//") }
        ?.let { baseUrl.trimEnd('/') + it }
        ?: model

@Preview(showBackground = true)
@Composable
private fun GotestAvatarPreview() {
    GotestTheme {
        GotestAvatar(model = null, contentDescription = null)
    }
}
