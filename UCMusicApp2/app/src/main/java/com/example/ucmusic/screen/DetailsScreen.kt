package com.example.ucmusic.screen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ucmusic.R
import com.example.ucmusic.viewmodel.RecognizerViewModel
import kotlin.math.sqrt

@Composable
fun DetailsScreen(
    vm: RecognizerViewModel,
    navController: NavHostController
) {
    val songInfo = vm.lastRecognizedSong ?: run {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }

    var showArtworkView by remember { mutableStateOf(false) }
    val isPlayingPreview by vm.isPlaying.collectAsState()
    val context = LocalContext.current

    // Sensor setup para detectar agitar el móvil
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val SHAKE_THRESHOLD_GRAVITY = 2.7f
    val SHAKE_SLOP_TIME_MS = 500L
    val SHAKE_COUNT_RESET_TIME_MS = 3000L

    var lastShakeTime by remember { mutableStateOf(0L) }
    var shakeCount by remember { mutableStateOf(0) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            var mAccel: Float = 0.00f
            var mAccelCurrent: Float = SensorManager.GRAVITY_EARTH
            var mAccelLast: Float = SensorManager.GRAVITY_EARTH

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    mAccelLast = mAccelCurrent
                    mAccelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val delta = mAccelCurrent - mAccelLast
                    mAccel = mAccel * 0.9f + delta

                    if (mAccel > SHAKE_THRESHOLD_GRAVITY) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                            if (currentTime - lastShakeTime > SHAKE_COUNT_RESET_TIME_MS) {
                                shakeCount = 0
                            }
                            shakeCount++
                            lastShakeTime = currentTime

                            if (shakeCount >= 2) {
                                showArtworkView = !showArtworkView
                                shakeCount = 0
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(sensorManager, accelerometer) {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            vm.stopPreview()
        }
    }

    val isEcuadorian = true

    val backgroundColors = if (isEcuadorian)
        listOf(Color(0xFFFCD116), Color(0xFF003893))
    else
        listOf(Color(0xFF1E1E2E), Color(0xFF121212))

    val textColor = if (isEcuadorian) Color(0xFF003893) else Color.White
    val logo = if (isEcuadorian) R.drawable.logoec1 else R.drawable.logos2

    val animationDuration = 800
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(animationDuration))
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(animationDuration))

    val fraseEcuatoriana = "“La música es el alma de un pueblo que no olvida sus raíces.”"

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = backgroundColors,
                    startY = 0f,
                    endY = 1000f
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (showArtworkView) "Música 100% Ecuatoriana" else "Detalles de la canción",
            fontSize = 30.sp,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(4f, 4f),
                    blurRadius = 10f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha)
        )

        Spacer(modifier = Modifier.size(16.dp))

        if (showArtworkView && songInfo.artworkUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(songInfo.artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Carátula del Álbum",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(5.dp, if (isEcuadorian) Color(0xFF003893) else Color.Gray, RoundedCornerShape(30.dp))
                    .shadow(12.dp, RoundedCornerShape(30.dp))
                    .scale(scale)
                    .alpha(alpha)
            )

            songInfo.previewUrl?.let { previewUrl ->
                Button(
                    onClick = {
                        if (isPlayingPreview) vm.stopPreview() else vm.playPreview(previewUrl)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEcuadorian) Color(0xFF003893) else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingPreview) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingPreview) "Detener" else "Reproducir",
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPlayingPreview) "Detener Muestra" else "Reproducir Muestra",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.size(18.dp))

            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .background(
                        color = if (isEcuadorian) Color(0xAAFFFFFF) else Color(0xAA000000),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = fraseEcuatoriana,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontStyle = FontStyle.Italic)
                )
            }
        } else {
            Image(
                painter = painterResource(id = logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(5.dp, if (isEcuadorian) Color(0xFF003893) else Color.Gray, RoundedCornerShape(30.dp))
                    .shadow(12.dp, RoundedCornerShape(30.dp))
                    .scale(scale)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.size(18.dp))

            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .background(
                        color = if (isEcuadorian) Color(0xAAFFFFFF) else Color(0xAA000000),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Título: ${songInfo.title}", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Artista: ${songInfo.artist}", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Álbum: ${songInfo.album ?: "Desconocido"}", color = textColor, fontSize = 18.sp)
                Text("Año: ${songInfo.year ?: "Desconocido"}", color = textColor, fontSize = 18.sp)
                Text("Género: ${songInfo.genre ?: "Desconocido"}", color = textColor, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                vm.stopPreview()
                navController.navigateUp()
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Volver", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
