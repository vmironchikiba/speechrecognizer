package com.example.speechrecognizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.speechrecognizer.data.QuestionService

@Composable
fun ResultScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    // Holds answers from server
    var answers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
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

            answers.forEach { (index, answer) ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text("Вопрос #${index + 1}", style = MaterialTheme.typography.bodyLarge)
                    TextField(
                        value = answer,
                        onValueChange = { newText ->
                            // Update local state
                            answers = answers.toMutableMap().apply {
                                this[index] = newText
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
//                service.sendFinalResults(answers) { success ->
//                    if (success) {
//                        println("✅ Результаты успешно отправлены: $answers")
//                        // TODO: maybe navigate back to start screen
//                    } else {
//                        println("❌ Ошибка отправки")
//                    }
//                }
            }) {
                Text("✅ Отправить")
            }
        }
    }
}
