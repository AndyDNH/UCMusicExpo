package com.example.ucmusic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ucmusic.model.SongHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    // Inserta una canción. Si ya existe, la reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongHistoryEntity)

    // Obtiene todas las canciones ordenadas por ID descendente (la más nueva primero).
    // Usar Flow asegura que la UI se actualice automáticamente cuando cambien los datos.
    @Query("SELECT * FROM song_history ORDER BY id DESC")
    fun getAllSongs(): Flow<List<SongHistoryEntity>>

    // (Opcional) Un método para borrar el historial si lo necesitas en el futuro.
    @Query("DELETE FROM song_history")
    suspend fun clearHistory()
}