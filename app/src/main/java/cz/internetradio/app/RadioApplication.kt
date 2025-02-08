package cz.internetradio.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.Logger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RadioApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .logger(object : Logger {
                override var level: Int = Log.DEBUG
                
                override fun log(tag: String, priority: Int, message: String?, throwable: Throwable?) {
                    Log.d("CoilImage", "$tag: $message", throwable)
                }
            })
            .build()
    }
} 