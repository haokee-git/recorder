package org.haokee.recorder.llm

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * LLM API Service - OpenAI-compatible API interface
 */
interface LLMApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false
)

data class Message(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String?
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
