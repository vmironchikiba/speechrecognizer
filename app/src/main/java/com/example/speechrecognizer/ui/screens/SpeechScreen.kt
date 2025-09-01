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
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    var currentQuestion by remember { mutableStateOf("Загрузка...") }
    var partialBuffer by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var offlineTtsAvailable by remember { mutableStateOf(true) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val service = remember { QuestionService.instance }
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    // Silence timer
    val silenceHandler = remember { Handler(Looper.getMainLooper()) }
    val silenceTimeout = 5000L // 5 seconds

    fun resetSilenceTimer(onTimeout: () -> Unit) {
        silenceHandler.removeCallbacksAndMessages(null)
        silenceHandler.postDelayed({
            Log.d("MVR", "Silence timeout reached")
            onTimeout()
        }, silenceTimeout)
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
                val locale = Locale("ru", "RU")
                val result = tts?.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    offlineTtsAvailable = false
                    Log.e("TTS", "Русский язык не поддерживается или отсутствуют данные")
                } else {
                    val offlineVoice = tts?.voices?.find { voice ->
                        voice.locale.language == "ru" &&
                                voice.locale.country == "RU" &&
                                !voice.isNetworkConnectionRequired
                    }

                    if (offlineVoice != null) {
                        tts?.voice = offlineVoice
                        Log.d("TTS", "Используется оффлайн голос: ${offlineVoice.name}")
                        ttsReady = true
                        offlineTtsAvailable = true
                    } else {
                        offlineTtsAvailable = false
                        Log.e("TTS", "Нет оффлайн-голоса для ru-RU")
                    }
                }
            } else {
                offlineTtsAvailable = false
                Log.e("TTS", "Инициализация TTS не удалась")
            }
        }
    }


    LaunchedEffect(currentQuestion, ttsReady) {
        if (ttsReady && currentQuestion.isNotEmpty() && currentQuestion != "Загрузка...") {
            tts?.speak(currentQuestion, TextToSpeech.QUEUE_FLUSH, null, "questionId")
        }
    }

    val scope = rememberCoroutineScope()

    fun goToNextQuestion(answer: String) {
        silenceHandler.removeCallbacksAndMessages(null) // stop silence detection
        service.sendAnswer(answer) { nextQ ->
            scope.launch(Dispatchers.Main) {
                if (nextQ.isNullOrEmpty()) {
                    navController.navigate(Screen.Result.route)
                } else {
                    currentQuestion = nextQ
                    answerText = ""
                    partialBuffer = ""
                    Log.d("MVR", "goToNextQuestion: $answerText")
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            }
        }
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            answerText = "Говорите..."
            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) {
                    goToNextQuestion(cleaned)
                } else {
                    speechRecognizer.cancel()
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            }
        }
        override fun onBeginningOfSpeech() {
            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) {
                    goToNextQuestion(cleaned)
                }
            }
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            // Don’t immediately finish; let silence timer handle it
            Log.d("MVR", "onEndOfSpeech called")
        }
        override fun onError(error: Int) {
            silenceHandler.removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).post {
                speechRecognizer.startListening(intent)
                isListening = true
            }
        }
        override fun onResults(results: Bundle?) {
            silenceHandler.removeCallbacksAndMessages(null)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalText = matches?.firstOrNull()?.trim() ?: ""
            goToNextQuestion(finalText)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim() ?: ""
            if (partial.isNotEmpty()) {
                partialBuffer = partial
                answerText = partial
            }
            Log.d("MVR", "onPartialResults: $answerText")

            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) {
                    goToNextQuestion(cleaned)
                } else {
                    speechRecognizer.cancel()
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)

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

        onDispose {
            silenceHandler.removeCallbacksAndMessages(null)
            speechRecognizer.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!offlineTtsAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ Оффлайн голос для русского не найден",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        context.startActivity(installIntent)
                    }) {
                        Text("📥 Установить оффлайн-голос")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = "Вопрос: $currentQuestion", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Ваш ответ: $answerText", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isListening) {
                    service.reset { success ->
                        if (success) {
                            Handler(Looper.getMainLooper()).post {
                                silenceHandler.removeCallbacksAndMessages(null)
                                speechRecognizer.cancel()
                                currentQuestion = "Загрузка..."
                                partialBuffer = ""
                                answerText = ""
                                speechRecognizer.startListening(intent)
                                isListening = true
                                service.getNextQuestion { q ->
                                    currentQuestion = q ?: "Ошибка загрузки вопроса"
                                }
                            }
                        }
                    }

                } else {
                    isListening = true
                    speechRecognizer.startListening(intent)
                }
            }
        ) {
            Text(if (isListening) "⏹ Сбросить" else "🎤 Слушать")
        }
    }
}
