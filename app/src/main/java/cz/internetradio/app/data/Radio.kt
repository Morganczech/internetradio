package cz.internetradio.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "radios")
data class Radio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val description: String?
) 