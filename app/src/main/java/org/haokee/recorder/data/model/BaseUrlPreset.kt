package org.haokee.recorder.data.model

import java.util.UUID

data class BaseUrlPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val isBuiltIn: Boolean = false
) {
    companion object {
        val DEFAULTS = listOf(
            BaseUrlPreset(
                id = "preset_openai",
                name = "OpenAI",
                url = "https://api.openai.com/v1/",
                isBuiltIn = true
            ),
            BaseUrlPreset(
                id = "preset_anthropic",
                name = "Anthropic",
                url = "https://api.anthropic.com/v1/",
                isBuiltIn = true
            ),
            BaseUrlPreset(
                id = "preset_gemini",
                name = "Google Gemini",
                url = "https://generativelanguage.googleapis.com/v1beta/openai/",
                isBuiltIn = true
            ),
            BaseUrlPreset(
                id = "preset_deepseek",
                name = "DeepSeek",
                url = "https://api.deepseek.com/v1/",
                isBuiltIn = true
            )
        )

        const val DEFAULT_SELECTED_ID = "preset_openai"
    }
}
