package com.example.ucmusic.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_history")
data class SongHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String?,
    val year: String?,
    val genre: String?,
    val artworkUrl: String?,
    val previewUrl: String?,
) {

}