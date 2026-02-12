package org.haokee.recorder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.haokee.recorder.data.model.BaseUrlPreset
import org.haokee.recorder.data.repository.ChatRepository
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository
import org.haokee.recorder.llm.LLMClient
import retrofit2.HttpException

data class SettingsUiState(
    val llmEnabled: Boolean = false,
    val llmBaseUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "",
    val isDarkTheme: Boolean = false,
    val appVersion: String = "1.0",
    val isLLMConfigured: Boolean = false,
    val showTestDialog: Boolean = false,
    val testResult: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val baseUrlPresets: List<BaseUrlPreset> = emptyList(),
    val selectedPresetId: String = "",
    val isBaseUrlExpanded: Boolean = false,
    val autoGenerateTitle: Boolean = true,
    val autoStart: Boolean = false,
    val alarmSound: Boolean = true,
    val alarmVibration: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val thoughtRepository: ThoughtRepository,
    private val chatRepository: ChatRepository,
    private val onChatHistoryCleared: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeThemeChanges()
    }

    private fun loadSettings() {
        val presets = settingsRepository.getBaseUrlPresets()
        val selectedId = settingsRepository.getSelectedPresetId()
        val selectedUrl = presets.find { it.id == selectedId }?.url ?: ""

        _uiState.update {
            it.copy(
                llmEnabled = settingsRepository.getLLMEnabled(),
                llmBaseUrl = selectedUrl,
                llmApiKey = settingsRepository.getLLMApiKey(),
                llmModel = settingsRepository.getLLMModel(),
                isDarkTheme = settingsRepository.getDarkTheme(),
                appVersion = settingsRepository.getAppVersion(),
                isLLMConfigured = settingsRepository.isLLMConfigured(),
                baseUrlPresets = presets,
                selectedPresetId = selectedId,
                autoGenerateTitle = settingsRepository.getAutoGenerateTitle(),
                autoStart = settingsRepository.getAutoStart(),
                alarmSound = settingsRepository.getAlarmSound(),
                alarmVibration = settingsRepository.getAlarmVibration()
            )
        }
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            settingsRepository.isDarkTheme.collect { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
    }

    fun updateLLMEnabled(enabled: Boolean) {
        settingsRepository.setLLMEnabled(enabled)
        _uiState.update { it.copy(llmEnabled = enabled) }
    }

    // Base URL Preset management
    fun toggleBaseUrlExpanded() {
        _uiState.update { it.copy(isBaseUrlExpanded = !it.isBaseUrlExpanded) }
    }

    fun selectPreset(presetId: String) {
        settingsRepository.setSelectedPresetId(presetId)
        val url = _uiState.value.baseUrlPresets.find { it.id == presetId }?.url ?: ""
        _uiState.update {
            it.copy(
                selectedPresetId = presetId,
                llmBaseUrl = url,
                isBaseUrlExpanded = false
            )
        }
    }

    fun addPreset(name: String, url: String) {
        val newPreset = BaseUrlPreset(name = name, url = url)
        val updated = _uiState.value.baseUrlPresets + newPreset
        settingsRepository.saveBaseUrlPresets(updated)
        settingsRepository.setSelectedPresetId(newPreset.id)
        _uiState.update {
            it.copy(
                baseUrlPresets = updated,
                selectedPresetId = newPreset.id,
                llmBaseUrl = url,
                isBaseUrlExpanded = false
            )
        }
    }

    fun updatePreset(presetId: String, name: String, url: String) {
        val updated = _uiState.value.baseUrlPresets.map {
            if (it.id == presetId) it.copy(name = if (it.isBuiltIn) it.name else name, url = url) else it
        }
        settingsRepository.saveBaseUrlPresets(updated)
        _uiState.update { state ->
            state.copy(
                baseUrlPresets = updated,
                llmBaseUrl = if (state.selectedPresetId == presetId) url else state.llmBaseUrl
            )
        }
    }

    fun deletePreset(presetId: String) {
        val preset = _uiState.value.baseUrlPresets.find { it.id == presetId } ?: return
        if (preset.isBuiltIn) return
        val updated = _uiState.value.baseUrlPresets.filter { it.id != presetId }
        settingsRepository.saveBaseUrlPresets(updated)

        // If deleted the selected one, fall back to first preset
        val newSelectedId = if (_uiState.value.selectedPresetId == presetId) {
            val fallbackId = updated.firstOrNull()?.id ?: BaseUrlPreset.DEFAULT_SELECTED_ID
            settingsRepository.setSelectedPresetId(fallbackId)
            fallbackId
        } else {
            _uiState.value.selectedPresetId
        }
        val newUrl = updated.find { it.id == newSelectedId }?.url ?: ""
        _uiState.update {
            it.copy(
                baseUrlPresets = updated,
                selectedPresetId = newSelectedId,
                llmBaseUrl = newUrl
            )
        }
    }

    fun updateLLMApiKey(apiKey: String) {
        settingsRepository.setLLMApiKey(apiKey)
        _uiState.update {
            it.copy(
                llmApiKey = apiKey,
                isLLMConfigured = apiKey.isNotBlank()
            )
        }
    }

    fun updateLLMModel(model: String) {
        settingsRepository.setLLMModel(model)
        _uiState.update { it.copy(llmModel = model) }
    }

    fun toggleDarkTheme() {
        val newValue = !_uiState.value.isDarkTheme
        settingsRepository.setDarkTheme(newValue)
        _uiState.update { it.copy(isDarkTheme = newValue) }
    }

    fun toggleAutoGenerateTitle() {
        val newValue = !_uiState.value.autoGenerateTitle
        settingsRepository.setAutoGenerateTitle(newValue)
        _uiState.update { it.copy(autoGenerateTitle = newValue) }
    }

    fun toggleAutoStart(enabled: Boolean) {
        settingsRepository.setAutoStart(enabled)
        _uiState.update { it.copy(autoStart = enabled) }
    }

    fun toggleAlarmSound(enabled: Boolean) {
        settingsRepository.setAlarmSound(enabled)
        _uiState.update { it.copy(alarmSound = enabled) }
    }

    fun toggleAlarmVibration(enabled: Boolean) {
        settingsRepository.setAlarmVibration(enabled)
        _uiState.update { it.copy(alarmVibration = enabled) }
    }

    fun testLLMConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showTestDialog = true, testResult = null) }

            val state = _uiState.value
            if (!state.isLLMConfigured) {
                _uiState.update {
                    it.copy(testResult = "❌ 未配置 API Key，请先设置", isLoading = false)
                }
                return@launch
            }

            try {
                val client = LLMClient.create(
                    baseUrl = state.llmBaseUrl,
                    apiKey = state.llmApiKey,
                    model = state.llmModel
                )

                // Send a real test message to validate model + key + URL
                val result = client.chat(
                    userMessage = "请回复数字1",
                    systemPrompt = "You are a test assistant. Respond with just the number 1."
                )

                result.onSuccess { response ->
                    val preview = response.take(80).replace("\n", " ")
                    _uiState.update {
                        it.copy(
                            testResult = "✅ 连接成功\n\nBase URL: ${state.llmBaseUrl}\nModel: ${state.llmModel}\n\nAI 回复: $preview",
                            isLoading = false
                        )
                    }
                }.onFailure { exception ->
                    val errorMsg = when (exception) {
                        is HttpException -> when (exception.code()) {
                            401 -> "❌ API Key 无效（401 Unauthorized）\n\n请检查 API Key 是否正确"
                            403 -> "❌ 访问被拒绝（403 Forbidden）\n\n请检查 API Key 权限"
                            404 -> "❌ 接口地址或模型名称错误（404 Not Found）\n\n请检查 Base URL 和 Model ID"
                            400 -> {
                                val body = try { exception.response()?.errorBody()?.string()?.take(120) } catch (_: Exception) { null }
                                "❌ 请求错误（400 Bad Request）\n\n可能是 Model ID 无效\n${body ?: ""}"
                            }
                            429 -> "❌ 请求频率超限（429 Too Many Requests）\n\n请稍后重试"
                            500 -> "❌ 服务器内部错误（500）\n\n请稍后重试或联系服务商"
                            else -> "❌ HTTP 错误 ${exception.code()}\n\n${exception.message()}"
                        }
                        else -> {
                            val msg = exception.message ?: "未知错误"
                            when {
                                msg.contains("timeout", ignoreCase = true) ||
                                        msg.contains("timed out", ignoreCase = true) ->
                                    "❌ 连接超时\n\n请检查 Base URL 是否正确，或网络是否正常"
                                msg.contains("Unable to resolve host", ignoreCase = true) ||
                                        msg.contains("UnknownHostException", ignoreCase = true) ->
                                    "❌ 无法连接到服务器\n\n请检查 Base URL 是否正确"
                                else -> "❌ 连接失败\n\n$msg"
                            }
                        }
                    }
                    _uiState.update { it.copy(testResult = errorMsg, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(testResult = "❌ 连接失败\n\n${e.message}", isLoading = false)
                }
            }
        }
    }

    fun closeTestDialog() {
        _uiState.update { it.copy(showTestDialog = false, testResult = null) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get all thoughts
                val allThoughts = mutableListOf<org.haokee.recorder.data.model.Thought>()
                thoughtRepository.getAllThoughts().first().let { allThoughts.addAll(it) }

                // Delete all thoughts
                thoughtRepository.deleteThoughts(allThoughts)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "清除数据失败：${e.message}"
                    )
                }
            }
        }
    }

    fun clearChatHistory() {
        chatRepository.clear()
        onChatHistoryCleared()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
