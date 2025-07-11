package com.example.ucmusic.service

// En un nuevo archivo llamado "iTunesService.kt"

import android.net.Uri
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.text.replace

class iTunesService {

    private val client = OkHttpClient()

    interface SearchCallback {
        fun onSuccess(previewUrl: String?, artworkUrl: String?)
        fun onFailure(error: String)
    }

    fun searchTrack(term: String, artist: String, callback: SearchCallback) {
        // Construimos un término de búsqueda más específico
        val searchTerm = "$term $artist"
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("itunes.apple.com")
            .addPathSegment("search")
            .addQueryParameter("term", searchTerm)
            .addQueryParameter("media", "music")
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "1") // Solo nos interesa el resultado más relevante
            .build()

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("iTunesService", "API call failed", e)
                callback.onFailure(e.message ?: "Error de red")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback.onFailure("Respuesta no exitosa: ${response.code}")
                    return
                }

                try {
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData)
                    val results = json.getJSONArray("results")

                    if (results.length() > 0) {
                        val track = results.getJSONObject(0)
                        val previewUrl = track.optString("previewUrl", null)
                        // Obtenemos una imagen de mayor calidad reemplazando '100x100' por '600x600'
                        val artworkUrl100 = track.optString("artworkUrl100", null)
                        val artworkUrl600 = artworkUrl100?.replace("100x100", "600x600")

                        callback.onSuccess(previewUrl, artworkUrl600)
                    } else {
                        callback.onFailure("No se encontraron resultados en iTunes.")
                    }
                } catch (e: Exception) {
                    Log.e("iTunesService", "Error parsing JSON", e)
                    callback.onFailure("Error al procesar la respuesta de iTunes.")
                }
            }
        })
    }
}