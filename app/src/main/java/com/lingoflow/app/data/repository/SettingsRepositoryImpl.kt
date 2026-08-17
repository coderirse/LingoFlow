package com.lingoflow.app.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Split-storage [SettingsRepository]: non-sensitive fields (active provider,
 * default mode, per-provider baseUrl/model/temperature) live in DataStore
 * Preferences, while all API keys live in EncryptedSharedPreferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: SharedPreferences
) : SettingsRepository {

    override suspend fun getSettings(): AppSettings {
        val prefs = dataStore.data.first()

        val activeProviderId = prefs[KEY_ACTIVE_PROVIDER]
            ?.let { runCatching { LlmProviderId.valueOf(it) }.getOrNull() }
            ?: LlmProviderId.DEEPSEEK

        val defaultMode = prefs[KEY_DEFAULT_MODE]
            ?.let { runCatching { TranslationMode.valueOf(it) }.getOrNull() }
            ?: TranslationMode.STANDARD

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
            defaultTranslationMode = defaultMode
        )
    }

    override suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER] = settings.activeLlmProviderId.name
            prefs[KEY_DEFAULT_MODE] = settings.defaultTranslationMode.name
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
        const val KEY_DICTIONARY_API_KEY = "dictionary_api_key"

        fun baseUrlKey(id: LlmProviderId) = "base_url_${id.name.lowercase()}"
        fun modelKey(id: LlmProviderId) = "model_${id.name.lowercase()}"
        fun temperatureKey(id: LlmProviderId) = "temperature_${id.name.lowercase()}"
        fun apiKeyKey(id: LlmProviderId) = "api_key_${id.name.lowercase()}"
    }
}
