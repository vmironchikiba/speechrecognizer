package com.example.speechrecognizer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
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
import com.example.speechrecognizer.data.QuestionService
import com.example.speechrecognizer.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SpeechScreen(navController: NavController) {
    val stopWord = "готово"
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    var currentQuestion by remember { mutableStateOf("Загрузка...") }
    var answerText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

//    val tts = remember {
//        TextToSpeech(context) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                val result = it.setLanguage(
//                    Locale.Builder().setLanguage("ru").setRegion("RU").build()
//                )
//                if (result == TextToSpeech.LANG_MISSING_DATA ||
//                    result == TextToSpeech.LANG_NOT_SUPPORTED
//                ) {
//                    Log.e("TTS", "Язык не поддерживается")
//                }
//            }
//        }
//    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val service = remember { QuestionService.instance }
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    // Load first question
    LaunchedEffect(Unit) {
        service.getNextQuestion { q ->
            currentQuestion = q ?: "Ошибка загрузки вопроса"
        }
    }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.Builder().setLanguage("ru").setRegion("RU").build()
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("TTS", "Язык не поддерживается")
                }
            }
        }
    }
    LaunchedEffect(currentQuestion) {
        if (currentQuestion.isNotEmpty() && currentQuestion != "Загрузка...") {
            tts?.speak(currentQuestion, TextToSpeech.QUEUE_FLUSH, null, "questionId")
        }
    }

    val scope = rememberCoroutineScope()

    fun goToNextQuestion(answer: String) {
        service.sendAnswer(answer) { nextQ ->
            scope.launch(Dispatchers.Main) {
                if (nextQ.isNullOrEmpty()) {
                    navController.navigate(Screen.Result.route)
                } else {
                    currentQuestion = nextQ
                    answerText = ""
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            }
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
//            answerText = speechError(error)
//            isListening = false
            Handler(Looper.getMainLooper()).post {
                speechRecognizer.startListening(intent)
                isListening = true
            }

        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            onAnyResults(matches)
            Log.d("MVR", "onResults: ${matches?.joinToString(", ") ?: "Nothing"}")
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//            onAnyResults(matches)
            Log.d("MVR", "onPartialResults: ${matches?.joinToString(", ") ?: "Nothing"}")
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        fun onAnyResults(matches: ArrayList<String>?) {
            // 🔹 Find first variant that contains "готово"
            val withDone = matches?.firstOrNull { it.contains(stopWord, ignoreCase = true) }

            // 🔹 If found, clean it, otherwise fallback to first variant
            val recognized = withDone ?: (matches?.get(0) ?: "")
            val hasDoneWord = recognized.contains(stopWord, ignoreCase = true)
            val cleaned = recognized.replace(stopWord, "", ignoreCase = true).trim()


            if (cleaned.isNotEmpty()) {
                answerText = cleaned
            }

            if (hasDoneWord) {
                goToNextQuestion(cleaned)
            } else {
                // keep listening for the same question
                Handler(Looper.getMainLooper()).post {
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            }
        }
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
