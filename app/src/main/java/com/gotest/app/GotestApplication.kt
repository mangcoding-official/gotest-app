package com.gotest.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.gotest.app.core.ui.image.DrawableNameMapper
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class GotestApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(DrawableNameMapper(context))
                // Images from the server: a plain client, so the sign-in token never goes with an image.
                add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
            }.build()
}
