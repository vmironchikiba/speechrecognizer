package com.example.speechrecognizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.speechrecognizer.data.QuestionsRepository

@Composable
fun ResultScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Ваши ответы:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        QuestionsRepository.questions.forEachIndexed { index, question ->
            val answer = AnswersHolder.answers[index] ?: "—"
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Вопрос: $question", style = MaterialTheme.typography.bodyLarge)
                TextField(
                    value = answer,
                    onValueChange = { newText -> AnswersHolder.answers[index] = newText },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            // Here we would "send to server", now just print
            println("FINAL RESULT: ${AnswersHolder.answers}")
        }) {
            Text("✅ Отправить")
        }
    }
}
