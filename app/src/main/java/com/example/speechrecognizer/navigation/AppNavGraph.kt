package com.example.speechrecognizer.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.speechrecognizer.ui.screens.SpeechScreen
import com.example.speechrecognizer.ui.screens.ResultScreen

sealed class Screen(val route: String) {
    object Speech : Screen("speech")
    object Result : Screen("result")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Speech.route) {
        composable(Screen.Speech.route) {
            SpeechScreen(navController = navController)
        }
        composable(Screen.Result.route) {
            ResultScreen(navController = navController)
        }
    }
}
