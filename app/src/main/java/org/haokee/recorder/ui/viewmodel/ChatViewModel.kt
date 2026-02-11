package org.haokee.recorder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.haokee.recorder.data.repository.ChatRepository
import org.haokee.recorder.data.repository.PersistedMessage
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository
import org.haokee.recorder.llm.LLMClient
import org.haokee.recorder.llm.Message
import java.time.format.DateTimeFormatter

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", or "system"
    val content: String,
    val isStreaming: Boolean = false,
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
    private val settingsRepository: SettingsRepository,
    private val thoughtRepository: ThoughtRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        loadSettings()
        loadHistory()
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

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val persisted = chatRepository.load()
            val messages = persisted.map { p ->
                ChatMessage(id = p.id, role = p.role, content = p.content, timestamp = p.timestamp)
            }
            _uiState.update { it.copy(messages = messages) }
        }
    }

    private fun saveHistory() {
        val messages = _uiState.value.messages
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.save(messages.map { m ->
                PersistedMessage(id = m.id, role = m.role, content = m.content, timestamp = m.timestamp)
            })
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty()) return
        if (!_uiState.value.isLLMEnabled || !_uiState.value.isLLMConfigured) return

        // Capture history BEFORE adding the new messages (only after last context clear)
        val history = buildConversationHistory()

        val userMessage = ChatMessage(role = "user", content = inputText)
        val assistantMessageId = java.util.UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMessageId,
            role = "assistant",
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantPlaceholder,
                inputText = "",
                isLoading = true,
                error = null
            )
        }
        // Save after user message is appended
        saveHistory()

        streamingJob = viewModelScope.launch {
            try {
                val systemPrompt = buildSystemPrompt()
                val llmClient = createLLMClient()
                var accumulatedContent = ""

                llmClient.chatStream(
                    userMessage = inputText,
                    systemPrompt = systemPrompt,
                    history = history
                ).collect { chunk ->
                    accumulatedContent += chunk
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(content = accumulatedContent)
                                else msg
                            }
                        )
                    }
                }

                // Streaming complete — finalize and save
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) msg.copy(isStreaming = false)
                            else msg
                        },
                        isLoading = false
                    )
                }
                saveHistory()
            } catch (e: CancellationException) {
                // User tapped stop — stopStreaming() already cleaned up state and will save
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Streaming failed", e)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMessageId)
                                msg.copy(content = "⚠️ 发送失败：${e.message}", isStreaming = false)
                            else msg
                        },
                        isLoading = false
                    )
                }
                saveHistory()
            }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val allThoughts = thoughtRepository.getAllThoughts().first()
        val transcribed = allThoughts.filter { it.isTranscribed }
        val original = allThoughts.filter { !it.isTranscribed }

        val sb = StringBuilder()
        sb.append("你是一个友好、专业的AI助手，帮助用户整理和思考他们的感言。\n\n")

        if (allThoughts.isEmpty()) {
            sb.append("用户目前还没有任何感言记录。\n")
            return sb.toString()
        }

        if (transcribed.isNotEmpty()) {
            sb.append("以下是用户已转换的感言（共 ${transcribed.size} 条）：\n")
            transcribed.forEachIndexed { index, thought ->
                sb.append("\n${index + 1}. 【${thought.title ?: "无标题"}】\n")
                val content = thought.content
                if (!content.isNullOrBlank() && content != thought.title) {
                    sb.append("   内容：$content\n")
                }
                sb.append("   记录时间：${thought.createdAt.format(timeFormatter)}\n")
            }
        }

        if (original.isNotEmpty()) {
            sb.append("\n用户还有 ${original.size} 条未转换的录音感言（仅有音频，暂无文字内容）。\n")
        }

        sb.append("\n请基于以上感言内容，帮助用户回答问题或整理思路。")
        return sb.toString()
    }

    fun regenerate(assistantMessageId: String) {
        if (_uiState.value.isLoading) return
        val messages = _uiState.value.messages
        val assistantIndex = messages.indexOfFirst { it.id == assistantMessageId }
        if (assistantIndex < 0) return

        // Find the user message right before this assistant response
        val userMsg = messages.subList(0, assistantIndex).lastOrNull { it.role == "user" } ?: return
        val userIndex = messages.indexOf(userMsg)

        // Build history: everything before the user message (after last context clear)
        val before = messages.subList(0, userIndex)
        val lastClearIndex = before.indexOfLast { it.role == "system" }
        val relevant = if (lastClearIndex >= 0) before.drop(lastClearIndex + 1) else before
        val history = relevant
            .filter { it.role == "user" || it.role == "assistant" }
            .filter { it.content.isNotEmpty() }
            .map { Message(role = it.role, content = it.content) }

        // Replace old assistant message with a fresh streaming placeholder
        val newAssistantId = java.util.UUID.randomUUID().toString()
        val placeholder = ChatMessage(id = newAssistantId, role = "assistant", content = "", isStreaming = true)
        val newMessages = messages.toMutableList().also { it[assistantIndex] = placeholder }

        _uiState.update { it.copy(messages = newMessages, isLoading = true) }
        saveHistory()

        streamingJob = viewModelScope.launch {
            try {
                val systemPrompt = buildSystemPrompt()
                val llmClient = createLLMClient()
                var accumulatedContent = ""

                llmClient.chatStream(
                    userMessage = userMsg.content,
                    systemPrompt = systemPrompt,
                    history = history
                ).collect { chunk ->
                    accumulatedContent += chunk
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == newAssistantId) msg.copy(content = accumulatedContent) else msg
                        })
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == newAssistantId) msg.copy(isStreaming = false) else msg
                        },
                        isLoading = false
                    )
                }
                saveHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Regenerate failed", e)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.id == newAssistantId)
                                msg.copy(content = "⚠️ 重新生成失败：${e.message}", isStreaming = false)
                            else msg
                        },
                        isLoading = false
                    )
                }
                saveHistory()
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.isStreaming) msg.copy(isStreaming = false) else msg
                },
                isLoading = false
            )
        }
        saveHistory()
    }

    /**
     * Build conversation history for multi-turn context.
     * Only includes messages after the last "context cleared" divider.
     * Excludes empty streaming placeholders.
     */
    private fun buildConversationHistory(): List<Message> {
        val messages = _uiState.value.messages
        val lastClearIndex = messages.indexOfLast { it.role == "system" }
        val relevant = if (lastClearIndex >= 0) messages.drop(lastClearIndex + 1) else messages
        return relevant
            .filter { it.role == "user" || it.role == "assistant" }
            .filter { it.content.isNotEmpty() }
            .map { Message(role = it.role, content = it.content) }
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
        saveHistory()
    }

    fun clearMessages() {
        _uiState.update { it.copy(messages = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) { chatRepository.clear() }
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
