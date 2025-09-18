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
import android.media.AudioManager
import android.media.ToneGenerator


@Composable
fun SpeechVoiceScreen(navController: NavController) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    var currentQuestion by remember { mutableStateOf("Загрузка...") }
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

    val scope = rememberCoroutineScope()

    val stopKeyword = "стоп"

    /** === Flow control === */
    fun startListeningWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            speechRecognizer.cancel()
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
        Log.d("MVR", "goToNextQuestion")
        val cleanedAnswer = answer.trim()

        // 🔒 Skip if answer is empty (user only said "стоп")
        if (cleanedAnswer.isEmpty()) {
            Log.d("MVR", "Empty answer ignored, waiting for valid input")
            startListeningWithDelay() // keep listening for next try
            return
        }

        service.sendAnswer(cleanedAnswer) { nextQ ->
            scope.launch(Dispatchers.Main) {
                Log.d("MVR", "sendAnswer")
                if (nextQ.isNullOrEmpty()) {
                    navController.navigate(Screen.Result.route)
                } else {
                    currentQuestion = nextQ
                    answerText = ""
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
                    Log.e("MVR", "Русский язык не поддерживается или отсутствуют данные")
                } else {
                    val offlineVoice = tts?.voices?.find { v ->
                        v.locale.language == "ru" && v.locale.country == "RU" && !v.isNetworkConnectionRequired
                    }
                    if (offlineVoice != null) {
                        tts?.voice = offlineVoice
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                Log.d("MVR", "Speaking started → stop STT")
                                Handler(Looper.getMainLooper()).post {
                                    if (isListening) {
                                        speechRecognizer.cancel()
                                        isListening = false
                                    }
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                Log.d("MVR", "Speaking finished → STT in 1 sec")
                                startListeningWithDelay()
                            }

                            override fun onError(utteranceId: String?) {
                                Log.e("MVR", "Error in TTS")
                                startListeningWithDelay()
                            }
                        })
                        Log.d("MVR", "Используется оффлайн голос: ${offlineVoice.name}")
                        ttsReady = true
                        offlineTtsAvailable = true
                    } else {
                        offlineTtsAvailable = false
                        Log.e("MVR", "Нет оффлайн-голоса для ru-RU")
                    }
                }
            } else {
                offlineTtsAvailable = false
                Log.e("MVR", "Инициализация TTS не удалась")
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
            Log.d("MVR", "onReadyForSpeech")
            answerText = "Говорите..."
            // 🔊 Play short tone
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_SUP_PIP, 1500) // 150 ms beep

        }
        override fun onBeginningOfSpeech() {
            Log.d("MVR", "onBeginningOfSpeech")
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {Log.d("MVR", "onBufferReceived")}
        override fun onEndOfSpeech() { Log.d("MVR", "onEndOfSpeech") }
        override fun onError(error: Int) {
            Log.e("MVR", "onError ->> $error")
            startListeningWithDelay()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            Log.d("MVR", "onPartialResults")
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: ""
            if (partial.isNotEmpty()) {
                answerText = partial
            }

            // ✅ If keyword detected → finalize answer immediately
            if (partial.contains(stopKeyword)) {
                val cleaned = answerText.replace(stopKeyword, "", ignoreCase = true).trim()
                goToNextQuestion(cleaned)
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d("MVR", "onResults")
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalText = matches?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) ?: ""
            if (finalText.contains(stopKeyword)) {
                val cleaned = finalText.replace(stopKeyword, "", ignoreCase = true).trim()
                goToNextQuestion(cleaned)
            } else {
                startListeningWithDelay()
  //              goToNextQuestion(finalText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {Log.d("MVR", "onEvent")}
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
                            speechRecognizer.cancel()
                            currentQuestion = "Загрузка..."
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
