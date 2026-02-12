package org.haokee.recorder.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.haokee.recorder.data.model.BaseUrlPreset

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
        private const val KEY_AUTO_GENERATE_TITLE = "auto_generate_title"
        private const val KEY_AUTO_START = "auto_start"

        // Onboarding
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        // Alarm Settings
        private const val KEY_ALARM_SOUND = "alarm_sound"
        private const val KEY_ALARM_VIBRATION = "alarm_vibration"

        // Base URL Presets
        private const val KEY_BASE_URL_PRESETS = "base_url_presets"
        private const val KEY_SELECTED_PRESET_ID = "selected_preset_id"

        // Default values
        private const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }

    private val gson = Gson()

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

    fun getLLMBaseUrl(): String {
        val presets = getBaseUrlPresets()
        val selectedId = getSelectedPresetId()
        return presets.find { it.id == selectedId }?.url ?: DEFAULT_BASE_URL
    }

    fun setLLMBaseUrl(url: String) {
        prefs.edit().putString(KEY_LLM_BASE_URL, url).apply()
    }

    // Base URL Presets
    fun getBaseUrlPresets(): List<BaseUrlPreset> {
        val json = prefs.getString(KEY_BASE_URL_PRESETS, null)
        if (json == null) {
            // First load: migrate from old llm_base_url if exists
            return migrateAndInitPresets()
        }
        val type = object : TypeToken<List<BaseUrlPreset>>() {}.type
        val stored: List<BaseUrlPreset> = try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            BaseUrlPreset.DEFAULTS
        }
        // Merge: ensure all built-in presets exist
        val storedIds = stored.map { it.id }.toSet()
        val missing = BaseUrlPreset.DEFAULTS.filter { it.id !in storedIds }
        return if (missing.isEmpty()) stored else (stored + missing).also { saveBaseUrlPresets(it) }
    }

    fun saveBaseUrlPresets(presets: List<BaseUrlPreset>) {
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_BASE_URL_PRESETS, json).apply()
    }

    fun getSelectedPresetId(): String {
        return prefs.getString(KEY_SELECTED_PRESET_ID, null)
            ?: BaseUrlPreset.DEFAULT_SELECTED_ID
    }

    fun setSelectedPresetId(id: String) {
        prefs.edit().putString(KEY_SELECTED_PRESET_ID, id).apply()
    }

    private fun migrateAndInitPresets(): List<BaseUrlPreset> {
        val oldUrl = prefs.getString(KEY_LLM_BASE_URL, null)
        val presets = BaseUrlPreset.DEFAULTS.toMutableList()
        var selectedId = BaseUrlPreset.DEFAULT_SELECTED_ID

        if (oldUrl != null && oldUrl != DEFAULT_BASE_URL) {
            // Check if old URL matches any built-in preset
            val normalizedOld = oldUrl.trimEnd('/')
            val match = presets.find { it.url.trimEnd('/') == normalizedOld }
            if (match != null) {
                selectedId = match.id
            } else {
                // Create custom preset for the old URL
                val custom = BaseUrlPreset(name = "Custom", url = oldUrl)
                presets.add(custom)
                selectedId = custom.id
            }
        }

        saveBaseUrlPresets(presets)
        setSelectedPresetId(selectedId)
        return presets
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

    fun getAutoGenerateTitle(): Boolean = prefs.getBoolean(KEY_AUTO_GENERATE_TITLE, true)

    fun setAutoGenerateTitle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_GENERATE_TITLE, enabled).apply()
    }

    fun getAutoStart(): Boolean = prefs.getBoolean(KEY_AUTO_START, false)

    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    // Alarm Settings
    fun getAlarmSound(): Boolean = prefs.getBoolean(KEY_ALARM_SOUND, true)

    fun setAlarmSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALARM_SOUND, enabled).apply()
    }

    fun getAlarmVibration(): Boolean = prefs.getBoolean(KEY_ALARM_VIBRATION, true)

    fun setAlarmVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALARM_VIBRATION, enabled).apply()
    }

    // Onboarding
    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
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
