package com.example.ucmusic.model

import java.io.Serializable

data class SongInfo(
    val title: String,
    val artist: String,
    val album: String?,
    val year: String?,
    val genre: String?,
    val artworkUrl: String? = null,      // ANTERIOR: spotifyImageUrl
    val previewUrl: String? = null,    // ANTERIOR: spotifyPreviewUrl

) : Serializable {

}