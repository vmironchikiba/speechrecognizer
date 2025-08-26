package com.example.speechrecognizer.helpers

import android.speech.SpeechRecognizer

fun speechError(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO ->  "ERROR AUDIO"
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT ->  "ERROR CANNOT CHECK_SUPPORT"
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS ->  "ERROR CANNOT LISTEN TO DOWNLOAD EVENTS"
        SpeechRecognizer.ERROR_CLIENT ->  "ERROR CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->  "ERROR INSUFFICIENT PERMISSIONS"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->  "ERROR LANGUAGE NOT SUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->  "ERROR LANGUAGE UNAVAILABLE"
        SpeechRecognizer.ERROR_NETWORK ->  "ERROR NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->  "ERROR NETWORK TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH ->  "ERROR NO MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->  "ERROR RECOGNIZER BUSY"
        SpeechRecognizer.ERROR_SERVER ->  "ERROR SERVER"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->  "ERROR SERVER DISCONNECTED"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->  "ERROR SPEECH TIMEOUT"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->  "ERROR TOO MANY REQUESTS"
        else -> "ERROR UNKNOWN"
    }

}

