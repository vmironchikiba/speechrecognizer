package com.example.speechrecognizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.speechrecognizer.navigation.Screen

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate(Screen.Voice.route) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("🎤 Voice")
        }

        Button(
            onClick = { navController.navigate(Screen.Speech.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⏱ Timer")
        }
    }
}
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    // Use a fake navController for preview purposes
    val navController = rememberNavController()
    HomeScreen(navController)
}