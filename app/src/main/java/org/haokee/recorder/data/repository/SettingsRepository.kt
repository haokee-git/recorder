package org.haokee.recorder.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings Repository - Manages app settings and API configuration
 *
 * Uses EncryptedSharedPreferences to securely store sensitive data like API keys
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // Settings keys
        private const val PREFS_NAME = "recorder_settings"
        private const val ENCRYPTED_PREFS_NAME = "recorder_encrypted_settings"

        // API Settings
        private const val KEY_LLM_ENABLED = "llm_enabled"
        private const val KEY_LLM_BASE_URL = "llm_base_url"
        private const val KEY_LLM_API_KEY = "llm_api_key"
        private const val KEY_LLM_MODEL = "llm_model"

        // UI Settings
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_APP_VERSION = "app_version"

        // Default values
        private const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }

    // Regular SharedPreferences for non-sensitive data
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Encrypted SharedPreferences for sensitive data (API keys)
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // StateFlows for reactive settings
    private val _isLLMEnabled = MutableStateFlow(getLLMEnabled())
    val isLLMEnabled: StateFlow<Boolean> = _isLLMEnabled.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(getDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // LLM API Settings
    fun getLLMEnabled(): Boolean = prefs.getBoolean(KEY_LLM_ENABLED, false)

    fun setLLMEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LLM_ENABLED, enabled).apply()
        _isLLMEnabled.value = enabled
    }

    fun getLLMBaseUrl(): String = prefs.getString(KEY_LLM_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun setLLMBaseUrl(url: String) {
        prefs.edit().putString(KEY_LLM_BASE_URL, url).apply()
    }

    fun getLLMApiKey(): String = encryptedPrefs.getString(KEY_LLM_API_KEY, "") ?: ""

    fun setLLMApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_LLM_API_KEY, apiKey).apply()
    }

    fun getLLMModel(): String = prefs.getString(KEY_LLM_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setLLMModel(model: String) {
        prefs.edit().putString(KEY_LLM_MODEL, model).apply()
    }

    // Check if LLM is configured (has API key)
    fun isLLMConfigured(): Boolean {
        return getLLMApiKey().isNotBlank()
    }

    // UI Settings
    fun getDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, false)

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _isDarkTheme.value = enabled
    }

    // App Info
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    // Data Management
    fun clearAllSettings() {
        prefs.edit().clear().apply()
        encryptedPrefs.edit().clear().apply()
        _isLLMEnabled.value = false
        _isDarkTheme.value = false
    }
}
