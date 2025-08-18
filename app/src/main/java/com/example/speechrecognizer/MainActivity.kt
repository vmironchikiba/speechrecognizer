package com.example.speechrecognizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.speechrecognizer.navigation.AppNavGraph
import com.example.speechrecognizer.ui.theme.SpeechRecognizerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechRecognizerTheme {
                AppNavGraph()
            }
        }
    }
}
