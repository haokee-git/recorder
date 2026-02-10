package org.haokee.recorder.llm

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * LLM Client - Wrapper for LLM API calls
 */
class LLMClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {
    private val apiService: LLMApiService
    private val gson = Gson()
    private val okHttpClient: OkHttpClient

    // Separate client for streaming — NO body-level logging interceptor.
    // HttpLoggingInterceptor at Level.BODY buffers the entire response body before
    // passing it on, which completely breaks SSE streaming.
    private val streamingHttpClient: OkHttpClient

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithAuth = originalRequest.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(requestWithAuth)
        }

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        streamingHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(LLMApiService::class.java)
    }

    /**
     * Send a single message and get response (single-turn conversation)
     */
    suspend fun chat(userMessage: String, systemPrompt: String? = null): Result<String> {
        return try {
            val messages = mutableListOf<Message>()

            if (systemPrompt != null) {
                messages.add(Message(role = "system", content = systemPrompt))
            }

            messages.add(Message(role = "user", content = userMessage))

            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = 0.7
            )

            val response = apiService.chatCompletion(request)

            if (response.choices.isNotEmpty()) {
                val content = response.choices[0].message.content
                Result.success(content)
            } else {
                Result.failure(Exception("No response from LLM"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message and receive response as a streaming Flow of text chunks (SSE).
     * Uses OkHttp directly to avoid Retrofit buffering the response.
     */
    fun chatStream(
        userMessage: String,
        systemPrompt: String? = null,
        history: List<Message> = emptyList()
    ): Flow<String> = flow {
        val messages = mutableListOf<Message>()
        if (systemPrompt != null) {
            messages.add(Message(role = "system", content = systemPrompt))
        }
        messages.addAll(history)
        messages.add(Message(role = "user", content = userMessage))

        val requestJson = gson.toJson(
            ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = 0.7,
                stream = true
            )
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toRequestBody(mediaType)

        val httpRequest = Request.Builder()
            .url("${baseUrl}chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = streamingHttpClient.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            response.close()
            throw Exception("HTTP ${response.code}: $errorBody")
        }

        response.body?.use { body ->
            body.charStream().buffered().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        if (data.isNotBlank()) {
                            try {
                                val chunk = gson.fromJson(data, StreamChunk::class.java)
                                val content = chunk.choices?.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    emit(content)
                                }
                            } catch (_: Exception) {
                                // Skip malformed JSON lines
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generate title from content
     */
    suspend fun generateTitle(content: String): Result<String> {
        val systemPrompt = """
            你是一个专业的标题生成助手。
            根据用户提供的内容，生成一个简洁、准确的标题。
            标题要求：
            1. 不超过 20 个字
            2. 概括内容的核心思想
            3. 使用简洁的语言
            4. 不需要添加引号或其他修饰符号
            5. 直接输出标题即可，不要有其他解释
        """.trimIndent()

        val userMessage = "请为以下内容生成标题：\n\n$content"

        return chat(userMessage, systemPrompt).mapCatching { title ->
            // Remove quotes if present
            title.trim().removeSurrounding("\"").removeSurrounding("'")
        }
    }

    /**
     * Multi-turn chat with conversation history
     */
    suspend fun chatWithHistory(
        messages: List<Message>,
        systemPrompt: String? = null
    ): Result<String> {
        return try {
            val fullMessages = mutableListOf<Message>()

            if (systemPrompt != null) {
                fullMessages.add(Message(role = "system", content = systemPrompt))
            }

            fullMessages.addAll(messages)

            val request = ChatCompletionRequest(
                model = model,
                messages = fullMessages,
                temperature = 0.7
            )

            val response = apiService.chatCompletion(request)

            if (response.choices.isNotEmpty()) {
                val content = response.choices[0].message.content
                Result.success(content)
            } else {
                Result.failure(Exception("No response from LLM"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Create LLMClient instance from settings
         */
        fun create(baseUrl: String, apiKey: String, model: String): LLMClient {
            // Ensure base URL ends with /
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            return LLMClient(normalizedBaseUrl, apiKey, model)
        }
    }
}
