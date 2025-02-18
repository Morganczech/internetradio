package cz.internetradio.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.model.RadioCategory
import javax.inject.Singleton
import cz.internetradio.app.data.entity.FavoriteSong
import cz.internetradio.app.data.dao.FavoriteSongDao

@Database(
    entities = [RadioEntity::class, FavoriteSong::class], 
    version = 4,
    exportSchema = false
)
@Singleton
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao
    abstract fun favoriteSongDao(): FavoriteSongDao

    companion object {
        const val DATABASE_NAME = "radio_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Přidání sloupce category s výchozí hodnotou MISTNI
                database.execSQL(
                    "ALTER TABLE radio_stations ADD COLUMN category TEXT NOT NULL DEFAULT '${RadioCategory.MISTNI.name}'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Vytvoření tabulky pro oblíbené skladby
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_songs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT,
                        radio_name TEXT NOT NULL,
                        radio_id TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        category TEXT
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Vytvoření nové tabulky bez sloupce gradientId
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS radios_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        originalCategory TEXT,
                        startColor INTEGER NOT NULL,
                        endColor INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        bitrate INTEGER
                    )
                """)

                // Kopírování dat ze staré tabulky do nové
                database.execSQL("""
                    INSERT INTO radios_new (
                        id, name, streamUrl, imageUrl, description, category, originalCategory,
                        startColor, endColor, isFavorite, bitrate
                    )
                    SELECT id, name, streamUrl, imageUrl, description, category, originalCategory,
                           startColor, endColor, isFavorite, bitrate
                    FROM radios
                """)

                // Smazání staré tabulky
                database.execSQL("DROP TABLE radios")

                // Přejmenování nové tabulky
                database.execSQL("ALTER TABLE radios_new RENAME TO radios")
            }
        }
    }
} 