package org.haokee.recorder.util

import android.util.Log
import com.github.houbb.opencc4j.util.ZhConverterUtil

/**
 * Chinese Character Converter
 *
 * Converts Traditional Chinese to Simplified Chinese using OpenCC4J library.
 * This is useful for post-processing Whisper recognition results that may
 * contain Traditional Chinese characters.
 */
object ChineseConverter {

    private const val TAG = "ChineseConverter"

    /**
     * Convert Traditional Chinese to Simplified Chinese
     *
     * @param text Input text (may contain Traditional Chinese)
     * @return Simplified Chinese text
     */
    fun toSimplified(text: String): String {
        if (text.isBlank()) {
            return text
        }

        return try {
            // Use OpenCC4J to convert Traditional to Simplified Chinese
            val simplified = ZhConverterUtil.toSimple(text)
            Log.d(TAG, "Converted: '$text' -> '$simplified'")
            simplified
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to simplified Chinese", e)
            // Return original text if conversion fails
            text
        }
    }

    /**
     * Check if text contains Traditional Chinese characters
     *
     * @param text Input text
     * @return true if contains Traditional Chinese
     */
    fun containsTraditional(text: String): Boolean {
        if (text.isBlank()) {
            return false
        }

        // Simple check: compare original with simplified version
        val simplified = toSimplified(text)
        return text != simplified
    }

    /**
     * Convert text to Simplified Chinese if it contains Traditional Chinese
     *
     * @param text Input text
     * @return Simplified Chinese text (only converts if needed)
     */
    fun ensureSimplified(text: String): String {
        return if (containsTraditional(text)) {
            toSimplified(text)
        } else {
            text
        }
    }
}
