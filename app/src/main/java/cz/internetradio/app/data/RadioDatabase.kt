package cz.internetradio.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity
import cz.internetradio.app.model.RadioCategory
import javax.inject.Singleton

@Database(
    entities = [RadioEntity::class], 
    version = 2,
    exportSchema = false
)
@Singleton
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao

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
    }
} 