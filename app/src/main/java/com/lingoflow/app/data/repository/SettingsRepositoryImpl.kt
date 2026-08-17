package com.lingoflow.app.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Split-storage [SettingsRepository]: non-sensitive fields (active provider,
 * default mode, per-provider baseUrl/model/temperature, theme, language)
 * live in DataStore Preferences, while all API keys live in
 * EncryptedSharedPreferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: SharedPreferences
) : SettingsRepository {

    override suspend fun getSettings(): AppSettings =
        buildSettings(dataStore.data.first())

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map(::buildSettings)

    private fun buildSettings(prefs: Preferences): AppSettings {
        val activeProviderId = prefs[KEY_ACTIVE_PROVIDER]
            ?.let { runCatching { LlmProviderId.valueOf(it) }.getOrNull() }
            ?: LlmProviderId.DEEPSEEK

        val defaultMode = prefs[KEY_DEFAULT_MODE]
            ?.let { runCatching { TranslationMode.valueOf(it) }.getOrNull() }
            ?: TranslationMode.STANDARD

        val themeMode = prefs[KEY_THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

        val appLanguage = prefs[KEY_APP_LANGUAGE]
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.ENGLISH

        val providers = LlmProviderId.entries.associateWith { id ->
            ProviderConfig(
                providerId = id,
                apiKey = encryptedPrefs.getString(apiKeyKey(id), "") ?: "",
                baseUrl = prefs[stringPreferencesKey(baseUrlKey(id))]?.ifBlank { null },
                model = prefs[stringPreferencesKey(modelKey(id))]?.ifBlank { null }
                    ?: id.defaultModel,
                temperature = prefs[floatPreferencesKey(temperatureKey(id))] ?: 0.7f
            )
        }

        return AppSettings(
            activeLlmProviderId = activeProviderId,
            llmProviders = providers,
            dictionaryApiKey = encryptedPrefs.getString(KEY_DICTIONARY_API_KEY, "") ?: "",
            defaultTranslationMode = defaultMode,
            themeMode = themeMode,
            appLanguage = appLanguage
        )
    }

    override suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER] = settings.activeLlmProviderId.name
            prefs[KEY_DEFAULT_MODE] = settings.defaultTranslationMode.name
            prefs[KEY_THEME_MODE] = settings.themeMode.name
            prefs[KEY_APP_LANGUAGE] = settings.appLanguage.name
            settings.llmProviders.forEach { (id, config) ->
                config.baseUrl?.let { prefs[stringPreferencesKey(baseUrlKey(id))] = it }
                prefs[stringPreferencesKey(modelKey(id))] = config.model
                prefs[floatPreferencesKey(temperatureKey(id))] = config.temperature
            }
        }
        encryptedPrefs.edit().apply {
            settings.llmProviders.forEach { (id, config) ->
                putString(apiKeyKey(id), config.apiKey)
            }
            putString(KEY_DICTIONARY_API_KEY, settings.dictionaryApiKey)
            apply()
        }
    }

    private companion object {
        val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_llm_provider")
        val KEY_DEFAULT_MODE = stringPreferencesKey("default_translation_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        const val KEY_DICTIONARY_API_KEY = "dictionary_api_key"

        fun baseUrlKey(id: LlmProviderId) = "base_url_${id.name.lowercase()}"
        fun modelKey(id: LlmProviderId) = "model_${id.name.lowercase()}"
        fun temperatureKey(id: LlmProviderId) = "temperature_${id.name.lowercase()}"
        fun apiKeyKey(id: LlmProviderId) = "api_key_${id.name.lowercase()}"
    }
}
