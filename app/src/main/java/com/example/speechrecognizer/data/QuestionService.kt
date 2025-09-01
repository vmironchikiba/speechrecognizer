package com.example.speechrecognizer.data

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class QuestionService private constructor() {

    private val client = OkHttpClient()
//    private val baseUrl = "http://10.0.2.2:4000" // emulator -> localhost of Mac
private val baseUrl = "http://Mironchik-VRvm.iba:4000" // emulator -> localhost of Mac
//    private val baseUrl = "http://192.168.1.110:4000" // emulator -> localhost of Mac

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
    fun getAnswers(callback: (List<AnswerItem>?) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/answers")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val list = body?.let { parseResults(it) }
                callback(list)
            }
        })
    }

    private fun parseResults(json: String): List<AnswerItem> {
        val arr = org.json.JSONArray(json)
        val result = mutableListOf<AnswerItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(
                AnswerItem(
                    id = obj.getInt("id"),
                    qIndex = obj.getInt("qIndex"),
                    question = obj.getString("question"),
                    answer = obj.getString("answer")
                )
            )
        }
        return result
    }

    fun reset(onResult: (Boolean) -> Unit) {
        val url = "$baseUrl/answers"   // ✅ matches your backend
        val request = Request.Builder()
            .delete()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onResult(false)
            }

            override fun onResponse(call: Call, response: Response) {
                onResult(response.isSuccessful)
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
//    private fun parseResults(json: String): Map<Int, String> {
//        val obj = JSONObject(json)
//        return obj.keys().asSequence().associate { k -> k.toInt() to obj.getString(k) }
//    }
}
data class AnswerItem(
    val id: Int,
    val qIndex: Int,
    val question: String,
    val answer: String
)
