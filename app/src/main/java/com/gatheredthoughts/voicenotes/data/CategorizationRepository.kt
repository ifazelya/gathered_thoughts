package com.gatheredthoughts.voicenotes.data

import com.gatheredthoughts.voicenotes.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CategorizationRepository(
    private val apiKey: String = BuildConfig.OPENAI_API_KEY,
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun categorizeNote(transcript: String): CategorizationResult = withContext(Dispatchers.IO) {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) {
            return@withContext fallbackResult("")
        }

        if (apiKey.isBlank()) {
            return@withContext fallbackResult(trimmed)
        }

        try {
            val result = callOpenAi(trimmed)
            validateResult(result, trimmed)
        } catch (_: Exception) {
            fallbackResult(trimmed)
        }
    }

    private fun callOpenAi(transcript: String): CategorizationResult {
        val prompt = """
            Categorize this voice memo transcript. Return strict JSON only with no markdown:
            {"title": "<short descriptive title>", "category": "<Task|Idea|Journal|Reminder>"}

            Categories:
            - Task: actionable to-do items
            - Idea: creative thoughts, brainstorming
            - Journal: personal reflections, diary entries
            - Reminder: things to remember later (appointments, dates)

            Transcript:
            $transcript
        """.trimIndent()

        val body = JSONObject()
            .put("model", "gpt-4o-mini")
            .put(
                "messages",
                org.json.JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )
            .put("temperature", 0.3)
            .toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("API error: ${response.code}")
            }
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response body")
            parseApiResponse(responseBody)
        }
    }

    private fun parseApiResponse(responseBody: String): CategorizationResult {
        val root = JSONObject(responseBody)
        val content = root
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(content)
        return CategorizationResult(
            title = json.getString("title").trim(),
            category = json.getString("category").trim()
        )
    }

    private fun validateResult(result: CategorizationResult, transcript: String): CategorizationResult {
        val title = result.title.ifBlank { fallbackTitle(transcript) }
        val category = if (NoteCategories.isValid(result.category)) {
            result.category
        } else {
            NoteCategories.JOURNAL
        }
        return CategorizationResult(title = title, category = category)
    }

    private fun fallbackResult(transcript: String): CategorizationResult {
        return CategorizationResult(
            title = fallbackTitle(transcript),
            category = NoteCategories.JOURNAL
        )
    }

    private fun fallbackTitle(transcript: String): String {
        val words = transcript.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "Untitled Note"
            words.size <= 5 -> words.joinToString(" ")
            else -> words.take(5).joinToString(" ") + "…"
        }
    }

    companion object {
        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
