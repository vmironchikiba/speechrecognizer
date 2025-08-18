package com.example.speechrecognizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.speechrecognizer.navigation.Screen

@Composable
fun ResultScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val recognizedText = RecognizedTextHolder.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = recognizedText.ifBlank { "Нет распознанного текста" },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { navController.navigate(Screen.Speech.route) }) {
            Text("⬅ Назад")
        }
    }
}
