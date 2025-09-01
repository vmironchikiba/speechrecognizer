package com.example.speechrecognizer.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.speechrecognizer.data.AnswerItem
import com.example.speechrecognizer.data.QuestionService

@Composable
fun ResultScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    // Holds answers from server
//    var answers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var answers by remember { mutableStateOf<List<AnswerItem>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    val service = remember { QuestionService.instance }

    // Load results from server
    LaunchedEffect(Unit) {
        service.getAnswers { result ->
            if (result != null) {
                answers = result
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Ваши ответы:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            answers.forEach { item ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(item.question, style = MaterialTheme.typography.bodyLarge)
                    TextField(
                        value = item.answer,
                        onValueChange = { newText ->
                            answers = answers.toMutableList().apply {
                                val idx = indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    this[idx] = this[idx].copy(answer = newText)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                service.reset { success ->
                    if (success) {
                        println("✅ База данных очищена")
                        Handler(Looper.getMainLooper()).post {
                            navController.popBackStack() // now runs on UI thread
                        } // go back to SpeechScreen
                    } else {
                        println("❌ Ошибка при очистке БД")
                    }
                }
            }) {
                Text("🔄 Reset")
            }
        }
    }
}
