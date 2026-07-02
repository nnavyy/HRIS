package com.ptniger.hris.utils

import com.ptniger.hris.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Groq AI client menggunakan Llama model gratis.
 * API key disimpan di local.properties (tidak di-hardcode).
 * Rate limit free tier: 30 req/menit, 14.400 req/hari.
 */
object GroqAiClient {

    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "llama-3.3-70b-versatile"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Generate AI text dari Groq API.
     * @param prompt Prompt yang dikirim ke AI
     * @return Result<String> berisi response text atau error
     */
    suspend fun generateReview(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Try local BuildConfig first, then fallback to Firestore app_configs
            var apiKey = BuildConfig.GROQ_API_KEY
            if (apiKey.isBlank()) {
                apiKey = try {
                    val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection(Constants.Collections.APP_CONFIGS)
                        .document("config_groq_api")
                        .get()
                        .await()
                    (doc.getString("value") ?: "").also {
                        if (it.isBlank() || it == "YOUR_API_KEY_HERE")
                            return@withContext Result.failure(Exception("Groq API key belum dikonfigurasi. Masukkan di Firestore > app_configs > config_groq_api > value."))
                    }
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Gagal membaca API key dari Firestore: ${e.message}"))
                }
            }

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 800)
                put("temperature", 0.7)
            }.toString()

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Groq API error ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
