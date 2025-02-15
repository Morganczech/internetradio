package cz.internetradio.app.di

import android.content.Context
import androidx.room.Room
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.AudioSink
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.audio.AudioSpectrumProcessor
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
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 2,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS * 2,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS * 2
            )
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
    fun provideDatabase(@ApplicationContext context: Context): RadioDatabase {
        return Room.databaseBuilder(
            context,
            RadioDatabase::class.java,
            "radio_database"
        )
        .addMigrations(RadioDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideRadioDao(database: RadioDatabase): RadioDao {
        return database.radioDao()
    }

    @Provides
    @Singleton
    fun provideRadioBrowserApi(): RadioBrowserApi {
        return RadioBrowserApi()
    }
} 