package com.example.ucmusic

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ucmusic.screen.DetailsScreen
import com.example.ucmusic.screen.HistoryScreen
import com.example.ucmusic.screen.MainScreen
import com.example.ucmusic.screen.SplashScreen
import com.example.ucmusic.viewmodel.RecognizerViewModel

class MainActivity : ComponentActivity() {

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permiso de micrófono necesario", Toast.LENGTH_LONG).show()
        }
    }

    private val recognizerViewModel: RecognizerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMicPermissionIfNeeded()
        setContent {
            MyApp(recognizerViewModel)
        }
    }

    private fun requestMicPermissionIfNeeded() {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

}



@Composable
fun MyApp(recognizerViewModel: RecognizerViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(navController = navController, vm = recognizerViewModel)
        }
        composable("details") {
            DetailsScreen(navController = navController, vm = recognizerViewModel)
        }

        composable("history") {
            HistoryScreen(vm = recognizerViewModel)
        }
    }

}