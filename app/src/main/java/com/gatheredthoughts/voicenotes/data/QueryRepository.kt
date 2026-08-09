package com.gatheredthoughts.voicenotes.data

import com.gatheredthoughts.voicenotes.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class QueryRepository(
    private val apiKey: String = BuildConfig.OPENAI_API_KEY,
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun queryNotes(
        question: String,
        notes: List<NoteEntity>
    ): QueryResult = withContext(Dispatchers.IO) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isEmpty()) {
            return@withContext QueryResult(
                answer = "Enter a question to search your memos.",
                noteIds = emptyList()
            )
        }

        if (notes.isEmpty()) {
            return@withContext QueryResult(
                answer = "You don't have any memos yet. Record one first!",
                noteIds = emptyList()
            )
        }

        if (apiKey.isBlank()) {
            return@withContext localFallback(trimmedQuestion, notes)
        }

        try {
            val result = callOpenAi(trimmedQuestion, notes)
            validateResult(result, notes)
        } catch (_: Exception) {
            localFallback(trimmedQuestion, notes)
        }
    }

    private fun callOpenAi(question: String, notes: List<NoteEntity>): QueryResult {
        val notesJson = JSONArray()
        notes.forEach { note ->
            notesJson.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("category", note.category)
                    .put("transcript", note.transcript)
                    .put("createdAt", note.createdAt)
            )
        }

        val prompt = """
            You help the user query their voice memo database ("Gathered Thoughts").
            Given the notes and the user's question, return strict JSON only with no markdown:
            {"answer": "<concise natural language answer>", "noteIds": [<relevant note ids>]}

            Rules:
            - Only include note IDs that are truly relevant to the question.
            - If nothing matches, return an empty noteIds array and explain in answer.
            - Answer in plain, helpful language (1-3 sentences).

            Notes:
            $notesJson

            Question: $question
        """.trimIndent()

        val body = JSONObject()
            .put("model", "gpt-4o-mini")
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )
            .put("temperature", 0.2)
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

    private fun parseApiResponse(responseBody: String): QueryResult {
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
        val noteIds = json.getJSONArray("noteIds").let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getInt(i))
                }
            }
        }
        return QueryResult(
            answer = json.getString("answer").trim(),
            noteIds = noteIds
        )
    }

    private fun validateResult(result: QueryResult, notes: List<NoteEntity>): QueryResult {
        val validIds = notes.map { it.id }.toSet()
        val filteredIds = result.noteIds.filter { it in validIds }
        val answer = result.answer.ifBlank {
            if (filteredIds.isEmpty()) {
                "No matching memos found."
            } else {
                "Found ${filteredIds.size} relevant memo(s)."
            }
        }
        return QueryResult(answer = answer, noteIds = filteredIds)
    }

    private fun localFallback(question: String, notes: List<NoteEntity>): QueryResult {
        val terms = question.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        val matching = notes.filter { note ->
            val haystack = "${note.title} ${note.transcript} ${note.category}".lowercase()
            terms.all { term -> haystack.contains(term) }
        }

        return if (matching.isEmpty()) {
            QueryResult(
                answer = "No memos matched your question. Try different keywords.",
                noteIds = emptyList()
            )
        } else {
            QueryResult(
                answer = "Found ${matching.size} memo(s) matching your search locally.",
                noteIds = matching.map { it.id }
            )
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
