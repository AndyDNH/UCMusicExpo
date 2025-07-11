package com.example.ucmusic.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acrcloud.rec.*
import com.example.ucmusic.database.AppDatabase
import com.example.ucmusic.model.SongHistoryEntity
import com.example.ucmusic.model.SongInfo
import com.example.ucmusic.service.iTunesService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject


sealed class RecognitionEvent {
    data class SongRecognized(val songInfo: SongInfo) : RecognitionEvent()
    object SongNotFound : RecognitionEvent()
    object NonEcuadorianSong : RecognitionEvent() // New event for non-Ecuadorian songs
    data class Error(val message: String) : RecognitionEvent()
    object Processing : RecognitionEvent()
}

class RecognizerViewModel(application: Application) :
    AndroidViewModel(application), IACRCloudListener {

    // --- State Properties for UI ---
    var lastRecognizedSong by mutableStateOf<SongInfo?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var volume by mutableStateOf(0.0)
        private set

    var resultText by mutableStateOf("Idle")
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // --- Event Flow for UI Communication ---
    private val _recognitionEvents = MutableSharedFlow<RecognitionEvent>()
    val recognitionEvents = _recognitionEvents.asSharedFlow()

    // --- Services and Clients ---
    private val itunesService = iTunesService()
    private var mediaPlayer: MediaPlayer? = null
    private val songDao = AppDatabase.getDatabase(application).songDao()
    private val client: ACRCloudClient

    // --- Database-driven History ---
    val songHistory: StateFlow<List<SongHistoryEntity>> = songDao.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ecuatorianArtists = listOf(
        "Carmencita Lara", "Julio Jaramillo", "Carlota Jaramillo", "Toty Rodríguez", "Olga Gutiérrez",
        "Petita Palma", "Carlos Rubira Infante", "Dúo Benítez Valencia", "Seg Condori", "Don Medardo y sus Players",
        "Hermanos Miño Naranjo", "Las Alondras del Guayas", "Paulina Tamayo", "Hilda Murillo", "Margarita Laso",
        "Maria Tejada", "Juan Fernando Velasco", "Mirella Cesa", "Daniel Betancourth", "Fausto Miño",
        "Paola Navarrete", "Gabriela Villalba", "Pamela Cortés", "Ren Kai", "Verde 70", "Tranzas", "Lolabúm",
        "La Máquina Camaleón", "Sal y Mileto", "Da Pawn", "Mamá soy demente", "Guardarraya", "Krucs en Karnak",
        "AU‑D", "Guanaco MC", "Mala Fama", "EVHA", "Gerardo", "Danilo Parra", "Swing Original Monks",
        "Nicola Cruz", "Quixosis", "Mateo Kingman", "Humazapas", "Fabrikante", "Sharon la Hechicera",
        "Delfín Quishpe", "Nelly Janeth", "Tamya Morán", "Mariela Condo", "Ricardo Pita", "Sudakaya", "Aladino",
        "Papa Chango", "Los Corrientes", "Mikrofon", "Marqués", "Daniel Beta", "Alex Ponce", "Los Chaucha Kings",
        "Playeros Kichwas", "Gustavo Velasquez", "Angel Velasquez", "Widinson", "Pepe Jaramillo",
        "Hermanas Mendoza Suasti", "Hermanas Sangurima", "Maxima Mejia", "Hermanos Nuñez", "Los Brillantes",
        "Duo Ecuador", "Jesus Vasquez", "Tito del Salto", "Kike Vega", "Irma Arauz", "Liliam Suarez",
        "Eduardo Brito", "Nicolas Fiallos", "Carlos Grijalva", "Alexandra Cabanilla", "Gerardo Moran",
        "La Toquilla", "Daniel Paez", "Fresia Saavedra", "Mélida Maria Jaramillo", "Consuelo Vargas",
        "Daniel Realpe", "Misquilla", "Segundo Rosero", "Chugo Tobar", "Roberto Calero", "Jenny Rosero",
        "Cecilio Alva", "Juanita Burbano", "Elias Vera", "Maximo Escaleras", "Fanny Jauch", "Jorge Luis del Hierro",
        "Edgar Palacios", "Martha Velasco", "Franklin Urrutia", "Los Hermanos Aymara", "Luis Aymara",
        "Polo Aymara", "Chinito del Ande", "Cecilia Canta", "Los Reales", "Rocio Jurado Ecuador",
        "Las Chicas Dulces", "Rocola Bacalao", "Johann Vera", "Tres Dedos", "Munn", "Latorre", "Chloé Silva",
        "Norka", "Chris Naranjo", "Luigi Muletto", "George Levi", "Daniela Albán", "Jayac", "Jaime Enrique Aymara",
        "Jinsop", "Normita Navarro", "Mary Murillo", "Las Tres Marías", "Sendero", "Normita Arcos",
        "Olimpo Cárdenas", "José Ignacio Canelos", "Francisco Paredes Herrera", "Carlos Brito Benavides",
        "Margarita Lugue", "María de los Ángeles", "Chumichasqui", "Los Zhunaulas", "Azucena Aymara",
        "Hipatia Balseca", "Máximo Escaleras", "Las Kautivas", "Bella Ilusión", "Diosas", "Dulce Desafío",
        "Encanto Latino", "Rumba Caliente", "Grupo Canela", "Jazmín la Tumbadora", "Sanyi", "Manolo",
        "Ricardo Suntaxi", "Aladino", "Jazmín Balseca", "Rosita Flores", "Cecy Narváez", "Bayron Caicedo",
        "Franklin Band", "Freddy Alexander", "Miriancita", "Lolita Echeverria", "Carmencita Lara", "Julio Jaramillo", "Lolita Echeverria",

        // Nuevos artistas urbanos / trap / reguetón
        "Renn OG",    // fenómeno urbano con millones de streams :contentReference[oaicite:1]{index=1}
        "Jombriel",  // reguetón y dancehall viral :contentReference[oaicite:2]{index=2}
        "Alex Solis (Auro)", // urbano-pop :contentReference[oaicite:3]{index=3}
        "Jorsh JMP", "Seich", "Maykel", "Peter V", "Diego Villacís",

        // Pop / urbano-pop / Andipop
        "Álex Ponce",
        "Mirella Cesa", // madre del Andipop :contentReference[oaicite:5]{index=5}
        "Ceci Juno",
        "Lolabúm", "La Máquina Camaleón", "Chloé Silva", "Letelefono", // Rolling Stone picks :contentReference[oaicite:7]{index=7}
        "Alkaloides",
        "Tonicamo",
        "Deslogin",
        "Entrañas", "Humazapas", "Nicola Cruz", "Mateo Kingman", "Sudakaya",
        "KAIFO",
        "Humazapas",
        "Minipony",
        "Gonzalo Ávila", "LaTorre", "Luz Pinos", "Camila Pérez", "Fiebre"
    )

    init {
        val config = ACRCloudConfig().apply {
            context = application
            host = "identify-us-west-2.acrcloud.com"
            accessKey = "9127a01c3bdcd932636d1fc60c135a0a" // Recuerda manejar esto de forma segura
            accessSecret = "GJQACgAt24BD15OtPrjVCelokg6RP4FBoxjWmJDg" // Recuerda manejar esto de forma segura
            recorderConfig.apply {
                rate = 8000
                channels = 1
                isVolumeCallback = true
            }
            acrcloudListener = this@RecognizerViewModel
        }
        client = ACRCloudClient().apply {
            initWithConfig(config)
        }
    }

    fun start() {
        if (client.startRecognize()) {
            isProcessing = true
            resultText = "Reconociendo…"
            lastRecognizedSong = null
            viewModelScope.launch {
                _recognitionEvents.emit(RecognitionEvent.Processing)
            }
        } else {
            resultText = "Error al iniciar el reconocimiento. Revisa permisos o configuración."
            isProcessing = false
            viewModelScope.launch {
                _recognitionEvents.emit(RecognitionEvent.Error(resultText))
            }
        }
    }

    fun cancel() {
        if (isProcessing) {
            client.cancel()
            isProcessing = false
            volume = 0.0
            resultText = "Cancelado"
        }
    }

    override fun onVolumeChanged(curVolume: Double) {
        volume = curVolume
    }

    override fun onResult(results: ACRCloudResult?) {
        isProcessing = false
        viewModelScope.launch {
            results?.result?.let { resultJson ->
                try {
                    val json = JSONObject(resultJson)
                    if (json.getJSONObject("status").getInt("code") == 0) {
                        val songInfoFromACR = parseSongInfo(json)
                        if (songInfoFromACR != null) {
                            fetchiTunesData(songInfoFromACR)
                        } else {
                            resultText = "Error al procesar la información de la canción."
                            _recognitionEvents.emit(RecognitionEvent.Error(resultText))
                        }
                    } else {
                        resultText = "Canción no encontrada."
                        _recognitionEvents.emit(RecognitionEvent.SongNotFound)
                    }
                } catch (e: JSONException) {
                    Log.e("RecognizerViewModel", "Error de formato JSON en la respuesta", e)
                    resultText = "Error de formato JSON: ${e.message}"
                    _recognitionEvents.emit(RecognitionEvent.Error(resultText))
                }
            } ?: run {
                resultText = "No se detectó resultado."
                _recognitionEvents.emit(RecognitionEvent.SongNotFound)
            }
        }
    }

    private fun fetchiTunesData(songInfo: SongInfo) {
        itunesService.searchTrack(
            songInfo.title,
            songInfo.artist,
            object : iTunesService.SearchCallback {
                override fun onSuccess(previewUrl: String?, artworkUrl: String?) {
                    val fullSongInfo = songInfo.copy(
                        artworkUrl = artworkUrl,
                        previewUrl = previewUrl
                    )
                    handleRecognitionSuccess(fullSongInfo)
                }

                override fun onFailure(error: String) {
                    Log.w("RecognizerViewModel", "Búsqueda en iTunes falló: $error. Usando datos de ACRCloud.")
                    // Si iTunes falla, usamos la info que ya teníamos
                    handleRecognitionSuccess(songInfo)
                }
            })
    }

    private fun handleRecognitionSuccess(songInfo: SongInfo) {
        val isEcuadorian = ecuatorianArtists.any { knownArtist ->
            songInfo.artist.contains(knownArtist, ignoreCase = true)
        }

        if (isEcuadorian) {
            lastRecognizedSong = songInfo
            resultText = "${songInfo.title} – ${songInfo.artist}"

            // ADD THIS LOG LINE
            Log.d("RecognizerApp", "Song recognized: Title: ${songInfo.title}, Artist: ${songInfo.artist}")

            viewModelScope.launch {
                songDao.insertSong(songInfo.toEntity())
                _recognitionEvents.emit(RecognitionEvent.SongRecognized(songInfo))
            }
        } else {
            // ADD THIS LOG LINE FOR NON-ECUADORIAN SONGS
            Log.d("RecognizerApp", "Non-Ecuadorian song recognized: Title: ${songInfo.title}, Artist: ${songInfo.artist}")
            viewModelScope.launch {
                _recognitionEvents.emit(RecognitionEvent.NonEcuadorianSong)
            }
            resultText = "La canción no es ecuatoriana."
        }
    }

    fun playPreview(previewUrl: String?) {
        stopPreview()
        if (previewUrl.isNullOrEmpty()) {
            Log.e("RecognizerViewModel", "URL de previsualización es nula o vacía.")
            _isPlaying.value = false
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(previewUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("RecognizerViewModel", "MediaPlayer error: what=$what, extra=$extra")
                    stopPreview()
                    true
                }
                setOnCompletionListener {
                    stopPreview()
                }
            }
        } catch (e: Exception) {
            Log.e("RecognizerViewModel", "Error al configurar MediaPlayer", e)
            stopPreview()
        }
    }

    fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun parseSongInfo(json: JSONObject): SongInfo? {
        return try {
            val music = json.getJSONObject("metadata").getJSONArray("music").getJSONObject(0)
            val title = music.getString("title")
            val artist = music.getJSONArray("artists").getJSONObject(0).getString("name")
            val album = music.optJSONObject("album")?.optString("name")
            val year = music.optString("release_date", null)?.take(4)
            val genre = music.optJSONArray("genres")?.optJSONObject(0)?.optString("name")
            SongInfo(title, artist, album, year, genre)
        } catch (e: Exception) {
            Log.e("RecognizerViewModel", "Error al parsear SongInfo", e)
            null
        }
    }


    override fun onCleared() {
        super.onCleared()
        client.release()
        stopPreview()
    }
}


fun SongInfo.toEntity(): SongHistoryEntity {
    return SongHistoryEntity(
        title = this.title,
        artist = this.artist,
        album = this.album,
        year = this.year,
        genre = this.genre,
        artworkUrl = this.artworkUrl,
        previewUrl = this.previewUrl
    )
}