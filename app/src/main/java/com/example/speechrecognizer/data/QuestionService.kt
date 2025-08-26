package com.example.speechrecognizer.data

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class QuestionService private constructor() {

    private val client = OkHttpClient()
//    private val baseUrl = "http://10.0.2.2:4000" // emulator -> localhost of Mac
    private val baseUrl = "http://Mironchik-VRvm.iba:4000" // emulator -> localhost of Mac

    companion object {
        val instance: QuestionService by lazy { QuestionService() }
    }

    fun getNextQuestion(callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/next-question")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    val question = JSONObject(body).optString("question", "")
                    callback(question)
                }
            }
        })
    }

    fun getAnswers(callback: (Map<Int, String>?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/answers")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val map = body?.let { parseResults(it) }
                callback(map)
            }
        })
    }

    fun sendAnswer(answer: String, callback: (String?) -> Unit) {
        val json = JSONObject().put("answer", answer)
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$baseUrl/answer")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string()
                    val question = JSONObject(body).optString("question", "")
                    callback(question)
                }
            }
        })
    }
    private fun parseResults(json: String): Map<Int, String> {
        val obj = JSONObject(json)
        return obj.keys().asSequence().associate { k -> k.toInt() to obj.getString(k) }
    }
}
