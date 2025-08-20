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
import com.example.speechrecognizer.data.QuestionsRepository
import com.example.speechrecognizer.navigation.Screen

@Composable
fun SpeechScreen(navController: NavController) {
    val context = LocalContext.current

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var currentQuestion by remember { mutableStateOf(QuestionsRepository.questions.first()) }
    var answerText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    fun goToNextQuestionOrFinish() {
        if (currentQuestionIndex < QuestionsRepository.questions.lastIndex) {
            currentQuestionIndex++
            currentQuestion = QuestionsRepository.questions[currentQuestionIndex]
            answerText = ""
            // restart listening
            speechRecognizer.startListening(intent)
            isListening = true
        } else {
            navController.navigate(Screen.Result.route)
        }
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            answerText = "Говорите..."
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { isListening = false }
        override fun onError(error: Int) {
            answerText = "Ошибка распознавания: $error"
            isListening = false
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognized = matches?.getOrNull(0) ?: "Ничего не распознано"

            // Check if "готово" was said
            val cleaned = recognized.replace("готово", "", ignoreCase = true).trim()

            if (cleaned.isNotEmpty()) {
                // Save only the actual answer
                AnswersHolder.answers[currentQuestionIndex] = cleaned
                answerText = cleaned
            }

            if (recognized.contains("готово", ignoreCase = true)) {
                // Move on if keyword spoken
                goToNextQuestionOrFinish()
            } else {
                // Otherwise continue listening for the same question
                speechRecognizer.startListening(intent)
                isListening = true
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)

        // 🔹 Start listening immediately
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            speechRecognizer.startListening(intent)
            isListening = true
        } else {
            ActivityCompat.requestPermissions(
                context as ComponentActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        onDispose { speechRecognizer.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Вопрос: $currentQuestion", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Ваш ответ: $answerText", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isListening) {
                    isListening = false
                    speechRecognizer.stopListening()
                } else {
                    isListening = true
                    speechRecognizer.startListening(intent)
                }
            }
        ) {
            Text(if (isListening) "⏹ Остановить" else "🎤 Слушать")
        }
    }
}

// Simple holder for answers (later can be Room DB)
object AnswersHolder {
    val answers = mutableMapOf<Int, String>()
}
