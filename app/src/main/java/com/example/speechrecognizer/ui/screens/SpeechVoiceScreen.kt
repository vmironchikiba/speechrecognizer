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
import android.speech.tts.UtteranceProgressListener
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
fun SpeechVoiceScreen(navController: NavController) {
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

    val silenceHandler = remember { Handler(Looper.getMainLooper()) }
    val silenceTimeout = 5000L // 5 sec
    val scope = rememberCoroutineScope()

    fun resetSilenceTimer(onTimeout: () -> Unit) {
        silenceHandler.removeCallbacksAndMessages(null)
        silenceHandler.postDelayed({
            Log.d("MVR", "Silence timeout reached")
            onTimeout()
        }, silenceTimeout)
    }

    /** === Flow control === */
    fun startListeningWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            speechRecognizer.startListening(intent)
            isListening = true
        }, 1000) // 1 sec gap
    }

    fun speakQuestion(text: String) {
        if (ttsReady && text.isNotEmpty() && text != "Загрузка...") {
            val utteranceId = "q_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun goToNextQuestion(answer: String) {
        silenceHandler.removeCallbacksAndMessages(null)
        service.sendAnswer(answer) { nextQ ->
            scope.launch(Dispatchers.Main) {
                if (nextQ.isNullOrEmpty()) {
                    navController.navigate(Screen.Result.route)
                } else {
                    currentQuestion = nextQ
                    answerText = ""
                    partialBuffer = ""
                    // ⚠️ Don’t start STT here — TTS will do it
                }
            }
        }
    }

    /** === Init: load first question === */
    LaunchedEffect(Unit) {
        service.getNextQuestion { q ->
            currentQuestion = q ?: "Ошибка загрузки вопроса"
        }
    }

    /** === Init: setup TTS === */
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("ru", "RU")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    offlineTtsAvailable = false
                    Log.e("TTS", "Русский язык не поддерживается или отсутствуют данные")
                } else {
                    val offlineVoice = tts?.voices?.find { v ->
                        v.locale.language == "ru" && v.locale.country == "RU" && !v.isNetworkConnectionRequired
                    }
                    if (offlineVoice != null) {
                        tts?.voice = offlineVoice
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                Log.d("TTS", "Speaking started → stop STT")
                                Handler(Looper.getMainLooper()).post {
                                    if (isListening) {
                                        speechRecognizer.cancel()
                                        isListening = false
                                    }
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                Log.d("TTS", "Speaking finished → STT in 1 sec")
                                startListeningWithDelay()
                            }

                            override fun onError(utteranceId: String?) {
                                Log.e("TTS", "Error in TTS")
                                startListeningWithDelay()
                            }
                        })
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

    /** === Speak whenever question changes === */
    LaunchedEffect(currentQuestion, ttsReady) {
        speakQuestion(currentQuestion)
    }

    /** === STT listener === */
    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            answerText = "Говорите..."
            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) goToNextQuestion(cleaned)
                else startListeningWithDelay()
            }
        }
        override fun onBeginningOfSpeech() {
            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) goToNextQuestion(cleaned)
            }
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { Log.d("MVR", "onEndOfSpeech") }
        override fun onError(error: Int) {
            silenceHandler.removeCallbacksAndMessages(null)
            startListeningWithDelay()
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
            resetSilenceTimer {
                val cleaned = answerText.trim()
                if (cleaned.isNotEmpty()) goToNextQuestion(cleaned)
                else startListeningWithDelay()
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /** === Lifecycle === */
    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
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

    /** === UI === */
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
                service.reset { success ->
                    if (success) {
                        Handler(Looper.getMainLooper()).post {
                            silenceHandler.removeCallbacksAndMessages(null)
                            speechRecognizer.cancel()
                            currentQuestion = "Загрузка..."
                            partialBuffer = ""
                            answerText = ""
                            service.getNextQuestion { q ->
                                currentQuestion = q ?: "Ошибка загрузки вопроса"
                            }
                        }
                    }
                }
            }
        ) {
            Text("⏹ Сбросить")
        }
    }
}
