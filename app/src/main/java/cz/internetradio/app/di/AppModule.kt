package cz.internetradio.app.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioSink
import cz.internetradio.app.audio.AudioSpectrumProcessor
import cz.internetradio.app.audio.EqualizerManager
import cz.internetradio.app.api.RadioBrowserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.media3.common.C
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideEqualizerManager(@ApplicationContext context: Context): EqualizerManager {
        return EqualizerManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioSpectrumProcessor(): AudioSpectrumProcessor {
        return AudioSpectrumProcessor()
    }

    @Provides
    @Singleton
    fun provideDefaultAudioSink(
        @ApplicationContext context: Context,
        audioSpectrumProcessor: AudioSpectrumProcessor
    ): DefaultAudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(audioSpectrumProcessor))
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(false)
            .build()
    }

    @UnstableApi
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        defaultAudioSink: DefaultAudioSink
    ): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setEnableDecoderFallback(true)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000, // minimální buffer 1s
                3000, // maximální buffer 3s
                500,  // buffer pro začátek přehrávání 500ms
                500   // buffer pro pokračování po rebufferingu 500ms
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setDeviceVolumeControlEnabled(true)
            .build().apply {
                setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    false
                )
                addAnalyticsListener(EventLogger("RadioPlayer"))
            }
    }

    @Provides
    @Singleton
    fun provideRadioBrowserApi(): RadioBrowserApi {
        return RadioBrowserApi()
    }
} 