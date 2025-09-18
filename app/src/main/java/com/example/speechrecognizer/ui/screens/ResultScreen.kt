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
import com.example.speechrecognizer.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController



@Composable
fun ResultScreen(navController: NavController) {
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

    ResultScreenContent(
        answers = answers,
        isLoading = isLoading,
        onAnswerChange = { id, newText ->
            answers = answers.toMutableList().apply {
                val idx = indexOfFirst { it.id == id }
                if (idx != -1) {
                    this[idx] = this[idx].copy(answer = newText)
                }
            }
        },
        onResetClick = {
            service.reset { success ->
                if (success) {
                    Handler(Looper.getMainLooper()).post {
                        navController.popBackStack()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                    println("❌ Ошибка при очистке БД")
                }
            }
        }
    )
}

@Composable
fun ResultScreenContent(
    answers: List<AnswerItem>,
    isLoading: Boolean,
    onAnswerChange: (Int, String) -> Unit,
    onResetClick: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                        onValueChange = { newText -> onAnswerChange(item.id, newText) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onResetClick) {
                Text("🔄 Reset")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenPreview() {
    val previewAnswers = listOf(
        AnswerItem(id = 1, qIndex = 0, question = "What is your name?", answer = "Alice"),
        AnswerItem(id = 2, qIndex = 1, question = "What is your favorite color?", answer = "Blue"),
        AnswerItem(id = 3, qIndex = 2, question = "What is your hobby?", answer = "Cycling")
    )

    MaterialTheme {
        ResultScreenContent(
            answers = previewAnswers,
            isLoading = false,
            onAnswerChange = { _, _ -> },
            onResetClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenLoadingPreview() {
    MaterialTheme {
        ResultScreenContent(
            answers = emptyList(),
            isLoading = true,
            onAnswerChange = { _, _ -> },
            onResetClick = {}
        )
    }
}