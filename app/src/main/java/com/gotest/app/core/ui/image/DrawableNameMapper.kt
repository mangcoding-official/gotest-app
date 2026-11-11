package com.gotest.app.core.ui.image

import android.annotation.SuppressLint
import android.content.Context
import coil3.map.Mapper
import coil3.request.Options

/**
 * Lets dummy data reference bundled images as `android.resource://<pkg>/drawable/<name>` (see
 * `drawableUri`). Coil only resolves resource ids, so this turns the name into its id first.
 * Real API data will be http(s) URLs and pass through untouched.
 */
class DrawableNameMapper(
    private val context: Context,
) : Mapper<String, Int> {
    @SuppressLint("DiscouragedApi") // Names come from dummy data only; ids cannot be serialized there.
    override fun map(
        data: String,
        options: Options,
    ): Int? {
        val prefix = "android.resource://${context.packageName}/drawable/"
        if (!data.startsWith(prefix)) return null
        val id = context.resources.getIdentifier(data.removePrefix(prefix), "drawable", context.packageName)
        return id.takeIf { it != 0 }
    }
}
