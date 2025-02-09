package cz.internetradio.app.di

import android.content.Context
import androidx.room.Room
import com.google.android.exoplayer2.ExoPlayer
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.data.dao.RadioDao
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
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RadioDatabase {
        return Room.databaseBuilder(
            context,
            RadioDatabase::class.java,
            "radio_database"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideRadioDao(database: RadioDatabase): RadioDao {
        return database.radioDao()
    }
} 