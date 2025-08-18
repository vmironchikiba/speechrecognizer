package com.example.speechrecognizer.ui.screens


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.speechrecognizer.navigation.Screen

@Composable
fun SpeechScreen(navController: NavController) {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("Нажмите кнопку и начните говорить") }
    var fullText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            resultText = "Говорите..."
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            resultText = "Ошибка распознавания: $error"
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognized = matches?.getOrNull(0) ?: "Ничего не распознано"
            fullText += " $recognized"
            resultText = recognized

            // restart recognition automatically
            if (isListening) {
                speechRecognizer.startListening(intent)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = resultText, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isListening) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            context as ComponentActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            1
                        )
                    } else {
                        isListening = true
                        speechRecognizer.startListening(intent)
                    }
                } else {
                    isListening = false
                    speechRecognizer.stopListening()
                    // Navigate to results screen with collected text
                    navController.navigate(Screen.Result.route) {
                        launchSingleTop = true
                    }
                }
            }
        ) {
            Text(if (isListening) "⏹ Остановить" else "🎤 Начать")
        }
    }

    // Save recognized text in a shared state holder
    LaunchedEffect(fullText) {
        RecognizedTextHolder.text = fullText.trim()
    }
}

// Singleton state holder (simple for now)
object RecognizedTextHolder {
    var text: String by mutableStateOf("")
}
