package cz.internetradio.app.di

import android.content.Context
import androidx.room.Room
import cz.internetradio.app.data.RadioDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.dao.FavoriteSongDao
import cz.internetradio.app.repository.RadioRepository
import cz.internetradio.app.api.RadioBrowserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RadioDatabase {
        return Room.databaseBuilder(
            context,
            RadioDatabase::class.java,
            "radio_database"
        )
        .fallbackToDestructiveMigration()
        .addMigrations(
            RadioDatabase.MIGRATION_1_2,
            RadioDatabase.MIGRATION_2_3
        )
        .build()
    }

    @Provides
    fun provideRadioDao(database: RadioDatabase): RadioDao {
        return database.radioDao()
    }

    @Provides
    fun provideFavoriteSongDao(database: RadioDatabase): FavoriteSongDao {
        return database.favoriteSongDao()
    }

    @Provides
    @Singleton
    fun provideRadioRepository(
        radioDao: RadioDao,
        radioBrowserApi: RadioBrowserApi
    ): RadioRepository {
        return RadioRepository(radioDao, radioBrowserApi)
    }
} 