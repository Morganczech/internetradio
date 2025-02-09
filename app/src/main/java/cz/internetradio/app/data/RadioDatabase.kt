package cz.internetradio.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import cz.internetradio.app.data.dao.RadioDao
import cz.internetradio.app.data.entity.RadioEntity

@Database(entities = [RadioEntity::class], version = 1)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao
} 