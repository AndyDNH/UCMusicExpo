package com.example.ucmusic.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ucmusic.dao.SongDao
import com.example.ucmusic.model.SongHistoryEntity

@Database(entities = [SongHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        // Volatile asegura que la instancia sea siempre la más actualizada.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si la instancia no es nula, la retornamos.
            // Si es nula, creamos la base de datos en un bloque synchronized.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "song_recognizer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}