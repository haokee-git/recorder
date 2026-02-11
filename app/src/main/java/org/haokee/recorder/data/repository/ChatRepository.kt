package org.haokee.recorder.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class PersistedMessage(
    val id: String,
    val role: String,   // "user", "assistant", "system"
    val content: String,
    val timestamp: Long
)

/**
 * Persists chat history to a JSON file in the app's private files directory.
 * Only stores role/content/id/timestamp — thought context is intentionally excluded
 * (it is injected as system prompt at send time, not persisted here).
 */
class ChatRepository(private val context: Context) {

    private val gson = Gson()
    private val historyFile: File get() = File(context.filesDir, "chat_history.json")

    fun save(messages: List<PersistedMessage>) {
        try {
            historyFile.writeText(gson.toJson(messages))
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to save chat history", e)
        }
    }

    fun load(): List<PersistedMessage> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<PersistedMessage>>() {}.type
            gson.fromJson<List<PersistedMessage>>(historyFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to load chat history", e)
            emptyList()
        }
    }

    fun clear() {
        try {
            if (historyFile.exists()) historyFile.delete()
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Failed to clear chat history", e)
        }
    }
}
