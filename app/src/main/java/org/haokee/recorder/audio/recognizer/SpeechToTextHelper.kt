package org.haokee.recorder.audio.recognizer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.haokee.recorder.audio.whisper.WhisperHelper
import org.haokee.recorder.data.model.Thought
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.llm.LLMClient
import org.haokee.recorder.util.ChineseConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Speech-to-text helper using Whisper for offline transcription
 *
 * This helper manages the lifecycle of WhisperHelper and provides
 * a simple API for converting audio thoughts to text.
 *
 * Phase 4 implementation: Integrated with LLM for title generation
 */
class SpeechToTextHelper(
    private val context: Context,
    private val settingsRepository: SettingsRepository? = null
) {

    companion object {
        private const val TAG = "SpeechToTextHelper"

        @Volatile
        private var instance: SpeechToTextHelper? = null

        fun getInstance(context: Context, settingsRepository: SettingsRepository? = null): SpeechToTextHelper {
            return instance ?: synchronized(this) {
                instance ?: SpeechToTextHelper(context.applicationContext, settingsRepository).also { instance = it }
            }
        }
    }

    private val whisperHelper = WhisperHelper.getInstance(context)
    private var isWhisperInitialized = false
    private val recordingsDir = context.filesDir.resolve("recordings")

    /**
     * Initialize Whisper model
     * Should be called once during app startup
     */
    suspend fun initialize(): Result<Unit> {
        if (isWhisperInitialized) {
            return Result.success(Unit)
        }

        return whisperHelper.initialize().also { result ->
            if (result.isSuccess) {
                isWhisperInitialized = true
                Log.d(TAG, "Whisper initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize Whisper", result.exceptionOrNull())
            }
        }
    }

    /**
     * Convert a thought to transcribed state using Whisper
     *
     * @param thought Original thought with audio file
     * @return Transcribed thought with title and content, or thought with error message if transcription fails
     */
    suspend fun convertThought(thought: Thought): Thought = withContext(Dispatchers.IO) {
        try {
            // Ensure Whisper is initialized
            if (!isWhisperInitialized) {
                val initResult = initialize()
                if (initResult.isFailure) {
                    Log.w(TAG, "Whisper not available, using fallback")
                    return@withContext createFallbackThought(thought, "Whisper 模型未初始化")
                }
            }

            // Get full audio file path
            val audioFile = recordingsDir.resolve(thought.audioPath)
            val audioPath = audioFile.absolutePath

            Log.d(TAG, "Converting thought with audio file: $audioPath")

            // Transcribe audio file
            val transcribeResult = whisperHelper.transcribe(audioPath)

            if (transcribeResult.isSuccess) {
                val rawText = transcribeResult.getOrNull() ?: ""

                if (rawText.isBlank()) {
                    Log.w(TAG, "Transcription returned empty text")
                    return@withContext createFallbackThought(thought, "未识别到语音内容")
                }

                // Convert Traditional Chinese to Simplified Chinese
                val transcribedText = ChineseConverter.toSimplified(rawText)
                Log.d(TAG, "Transcription successful (after conversion): $transcribedText")

                // Generate title using LLM if available, otherwise use simple extraction
                val title = generateTitle(transcribedText)
                val content = transcribedText

                thought.copy(
                    title = title,
                    content = content,
                    transcribedAt = LocalDateTime.now(),
                    isTranscribed = true
                )
            } else {
                Log.e(TAG, "Transcription failed", transcribeResult.exceptionOrNull())
                createFallbackThought(thought, "转换失败：${transcribeResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting thought", e)
            createFallbackThought(thought, "转换出错：${e.message}")
        }
    }

    /**
     * Generate a title using LLM if available, otherwise use simple extraction
     */
    private suspend fun generateTitle(text: String): String {
        // Check if LLM is enabled, configured, and auto-generate title is on
        if (settingsRepository != null &&
            settingsRepository.getLLMEnabled() &&
            settingsRepository.isLLMConfigured() &&
            settingsRepository.getAutoGenerateTitle()
        ) {
            try {
                Log.d(TAG, "Generating title using LLM...")
                val llmClient = LLMClient.create(
                    baseUrl = settingsRepository.getLLMBaseUrl(),
                    apiKey = settingsRepository.getLLMApiKey(),
                    model = settingsRepository.getLLMModel()
                )

                val result = llmClient.generateTitle(text)

                if (result.isSuccess) {
                    val generatedTitle = result.getOrNull() ?: ""
                    if (generatedTitle.isNotBlank()) {
                        Log.d(TAG, "LLM generated title: $generatedTitle")
                        return generatedTitle
                    }
                }

                Log.w(TAG, "LLM title generation failed, using fallback")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating title with LLM", e)
            }
        }

        // Fallback to simple extraction
        return generateTitleFromText(text)
    }

    /**
     * Generate a title from transcribed text using simple extraction
     * Takes first 30 characters or first sentence
     */
    private fun generateTitleFromText(text: String): String {
        val maxLength = 30
        val firstSentence = text.split(".", "。", "!", "！", "?", "？").firstOrNull() ?: text

        return if (firstSentence.length > maxLength) {
            firstSentence.substring(0, maxLength) + "..."
        } else {
            firstSentence
        }
    }

    /**
     * Create a fallback thought when transcription fails
     */
    private fun createFallbackThought(thought: Thought, errorMessage: String): Thought {
        val defaultTitle = generateDefaultTitle(thought.createdAt)
        val defaultContent = "$errorMessage\n\n请点击编辑按钮输入内容"

        return thought.copy(
            title = defaultTitle,
            content = defaultContent,
            transcribedAt = LocalDateTime.now(),
            isTranscribed = true
        )
    }

    /**
     * Generate a default title based on creation time
     */
    private fun generateDefaultTitle(createdAt: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return "感言 ${createdAt.format(formatter)}"
    }

    /**
     * Release Whisper resources
     */
    fun release() {
        whisperHelper.release()
        isWhisperInitialized = false
    }
}
