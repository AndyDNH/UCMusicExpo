package com.example.ucmusic.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ucmusic.R
import com.example.ucmusic.viewmodel.RecognitionEvent
import com.example.ucmusic.viewmodel.RecognizerViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, vm: RecognizerViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val songHistory by vm.songHistory.collectAsState()

    LaunchedEffect(Unit) {
        vm.recognitionEvents.collectLatest { event ->
            when (event) {
                is RecognitionEvent.SongRecognized -> {
                    navController.navigate("details") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is RecognitionEvent.SongNotFound -> {
                    snackbarHostState.showSnackbar(
                        message = "Canción no encontrada. Intenta de nuevo.",
                        withDismissAction = true
                    )
                }
                is RecognitionEvent.NonEcuadorianSong -> {
                    snackbarHostState.showSnackbar(
                        message = "La canción no es ecuatoriana. Intenta de nuevo.",
                        withDismissAction = true
                    )
                }
                is RecognitionEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${event.message}. Por favor, intenta de nuevo.",
                        withDismissAction = true
                    )
                }
                is RecognitionEvent.Processing -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("v 1.0", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historial",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFFBBDEFB))
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.logos2),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(250.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Pulsa el botón para reconocer una canción",
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            if (vm.isProcessing) {
                ListeningAnimation()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { vm.start() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !vm.isProcessing
            ) {
                Text(
                    text = if (vm.isProcessing) "Reconociendo..." else "🎵 Reconocer Canción",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ListeningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(150.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp * scale)
                .background(color = Color.White.copy(alpha = alpha), shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color = Color.White, shape = CircleShape)
        )
    }
}
