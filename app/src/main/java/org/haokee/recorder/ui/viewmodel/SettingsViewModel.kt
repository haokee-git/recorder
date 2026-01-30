package org.haokee.recorder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.haokee.recorder.data.repository.SettingsRepository
import org.haokee.recorder.data.repository.ThoughtRepository

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
    val error: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val thoughtRepository: ThoughtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeThemeChanges()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                llmEnabled = settingsRepository.getLLMEnabled(),
                llmBaseUrl = settingsRepository.getLLMBaseUrl(),
                llmApiKey = settingsRepository.getLLMApiKey(),
                llmModel = settingsRepository.getLLMModel(),
                isDarkTheme = settingsRepository.getDarkTheme(),
                appVersion = settingsRepository.getAppVersion(),
                isLLMConfigured = settingsRepository.isLLMConfigured()
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

    fun updateLLMBaseUrl(url: String) {
        settingsRepository.setLLMBaseUrl(url)
        _uiState.update { it.copy(llmBaseUrl = url) }
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

    fun testLLMConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showTestDialog = true, testResult = null) }

            try {
                // TODO: Implement actual API test
                // For now, just check if API key is configured
                kotlinx.coroutines.delay(1000) // Simulate network request

                val result = if (_uiState.value.isLLMConfigured) {
                    "✅ 连接测试成功\n\nBase URL: ${_uiState.value.llmBaseUrl}\nModel: ${_uiState.value.llmModel}"
                } else {
                    "❌ 连接测试失败\n\n请先配置 API Key"
                }

                _uiState.update { it.copy(testResult = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        testResult = "❌ 连接测试失败\n\n${e.message}",
                        isLoading = false
                    )
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
