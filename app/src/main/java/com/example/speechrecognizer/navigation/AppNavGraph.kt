package com.example.speechrecognizer.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.speechrecognizer.ui.screens.HomeScreen
import com.example.speechrecognizer.ui.screens.SpeechScreen
import com.example.speechrecognizer.ui.screens.SpeechVoiceScreen
import com.example.speechrecognizer.ui.screens.ResultScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Speech : Screen("speech")
    object Voice : Screen("voice")
    object Result : Screen("result")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Speech.route) {
            SpeechScreen(navController = navController)
        }
        composable(Screen.Voice.route) {
            SpeechVoiceScreen(navController = navController)
        }
        composable(Screen.Result.route) {
            ResultScreen(navController = navController)
        }
    }
}
