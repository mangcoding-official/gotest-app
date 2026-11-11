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
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * Circular profile picture. [model] is anything Coil accepts: a drawable id for the
 * dummy avatars today, a URL once the API exists.
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
        model = model,
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

@Preview(showBackground = true)
@Composable
private fun GotestAvatarPreview() {
    GotestTheme {
        GotestAvatar(model = null, contentDescription = null)
    }
}
