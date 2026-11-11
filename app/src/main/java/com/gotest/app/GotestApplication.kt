package com.gotest.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.gotest.app.core.ui.image.DrawableNameMapper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GotestApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(DrawableNameMapper(context)) }
            .build()
}
