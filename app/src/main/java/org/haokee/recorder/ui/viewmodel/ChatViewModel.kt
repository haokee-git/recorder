package org.haokee.recorder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.llm.LLMClient
import org.haokee.recorder.llm.Message

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLLMEnabled: Boolean = false,
    val isLLMConfigured: Boolean = false
)

class ChatViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.isLLMEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(
                        isLLMEnabled = enabled,
                        isLLMConfigured = settingsRepository.isLLMConfigured()
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty()) return

        if (!_uiState.value.isLLMEnabled || !_uiState.value.isLLMConfigured) {
            _uiState.update {
                it.copy(error = "请先在设置中启用并配置大模型 API")
            }
            return
        }

        // Add user message to chat
        val userMessage = ChatMessage(
            role = "user",
            content = inputText
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        // Call LLM API
        viewModelScope.launch {
            try {
                val llmClient = createLLMClient()

                val result = llmClient.chat(
                    userMessage = inputText,
                    systemPrompt = "你是一个友好、专业的AI助手，帮助用户整理和思考他们的感言。"
                )

                result.onSuccess { response ->
                    val assistantMessage = ChatMessage(
                        role = "assistant",
                        content = response
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                }.onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "发送失败：${exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "发送失败：${e.message}"
                    )
                }
            }
        }
    }

    fun clearContext() {
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    role = "system",
                    content = "--- 上下文已清除 ---"
                )
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun createLLMClient(): LLMClient {
        return LLMClient.create(
            baseUrl = settingsRepository.getLLMBaseUrl(),
            apiKey = settingsRepository.getLLMApiKey(),
            model = settingsRepository.getLLMModel()
        )
    }
}
