package org.haokee.recorder.audio.recognizer

import org.haokee.recorder.data.model.Thought
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Speech-to-text helper
 *
 * Note: Android's native SpeechRecognizer can only recognize real-time microphone input,
 * not audio files. For true audio file transcription, we'll integrate a cloud API
 * (such as OpenAI Whisper) in Phase 3.
 *
 * Phase 2 implementation: Generate default title and allow manual editing
 */
object SpeechToTextHelper {

    /**
     * Convert a thought to transcribed state with default title
     * In Phase 3, this will be replaced with actual speech-to-text API call
     */
    fun convertThought(thought: Thought): Thought {
        val defaultTitle = generateDefaultTitle(thought.createdAt)
        val defaultContent = "请点击编辑按钮输入内容"

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
}
