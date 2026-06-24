package com.gallery.cleaner

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.gallery.cleaner.util.ActivityProvider
import com.gallery.cleaner.util.log.AppLogger
import com.gallery.cleaner.util.log.LogLevel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GalleryApp : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "GalleryApp"
    }

    override fun onCreate() {
        super.onCreate()

        ActivityProvider.init(this)

        AppLogger.init(
            context = this,
            level = LogLevel.DEBUG
        )

        AppLogger.i(TAG, "🚀 应用程序启动")
        AppLogger.d(TAG, "SDK版本: ${android.os.Build.VERSION.SDK_INT}")
        AppLogger.d(TAG, "设备型号: ${android.os.Build.MODEL}")
        AppLogger.d(TAG, "系统版本: ${android.os.Build.VERSION.RELEASE}")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .allowHardware(true)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
}
