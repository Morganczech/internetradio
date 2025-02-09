package cz.internetradio.app.di

import android.content.Context
import androidx.room.Room
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.audio.DefaultAudioSink
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.audio.AudioSpectrumProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioSpectrumProcessor: AudioSpectrumProcessor
    ): ExoPlayer {
        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf<AudioProcessor>(audioSpectrumProcessor))
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context, renderersFactory)
            .build()
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
} 