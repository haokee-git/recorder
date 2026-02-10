package org.haokee.recorder.llm

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * LLM API Service - OpenAI-compatible API interface
 */
interface LLMApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): ChatCompletionResponse

    @Streaming
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletionStream(@Body request: ChatCompletionRequest): ResponseBody
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

// SSE streaming data classes
data class StreamChunk(
    val choices: List<StreamChoice>? = null
)

data class StreamChoice(
    val delta: StreamDelta? = null
)

data class StreamDelta(
    val role: String? = null,
    val content: String? = null
)
