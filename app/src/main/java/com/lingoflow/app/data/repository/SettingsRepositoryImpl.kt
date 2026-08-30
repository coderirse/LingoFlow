package com.lingoflow.app.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.InterfaceStyle
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.domain.model.translation.TranslationMemory
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Split-storage [SettingsRepository]: non-sensitive fields (active provider,
 * default mode, per-provider baseUrl/model/temperature, theme, language)
 * live in DataStore Preferences, while all API keys live in
 * EncryptedSharedPreferences.
 *
 * ESP decryption is expensive (~ms per key) and [EncryptedSharedPreferences]
 * is not safe to touch from the main thread, so key reads are cached in
 * memory and every settings read is confined to [Dispatchers.IO]
 * ([observeSettings] via [flowOn], [getSettings] via [withContext]).
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: SharedPreferences
) : SettingsRepository {

    /** Decrypted API keys; populated on first IO read, refreshed on save. */
    @Volatile
    private var keysCache: Map<String, String>? = null

    override suspend fun getSettings(): AppSettings = withContext(Dispatchers.IO) {
        buildSettings(dataStore.data.first())
    }

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map(::buildSettings).flowOn(Dispatchers.IO)

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

        val interfaceStyle = prefs[KEY_INTERFACE_STYLE]
            ?.let { runCatching { InterfaceStyle.valueOf(it) }.getOrNull() }
            ?: InterfaceStyle.MODERN

        val keys = readApiKeys()
        val providers = LlmProviderId.entries.associateWith { id ->
            ProviderConfig(
                providerId = id,
                apiKey = keys[apiKeyKey(id)].orEmpty(),
                baseUrl = prefs[stringPreferencesKey(baseUrlKey(id))]?.ifBlank { null },
                model = prefs[stringPreferencesKey(modelKey(id))]?.ifBlank { null }
                    ?: id.defaultModel,
                temperature = prefs[floatPreferencesKey(temperatureKey(id))] ?: 0.7f
            )
        }

        return AppSettings(
            activeLlmProviderId = activeProviderId,
            llmProviders = providers,
            dictionaryApiKey = keys[KEY_DICTIONARY_API_KEY].orEmpty(),
            defaultTranslationMode = defaultMode,
            themeMode = themeMode,
            appLanguage = appLanguage,
            interfaceStyle = interfaceStyle
        )
    }

    /**
     * Reads every API key once per cache generation. Individual keys that
     * fail to decrypt (corrupted entry, Keystore hiccup) come back empty —
     * the user re-enters them; the app must never crash over a secret.
     */
    private fun readApiKeys(): Map<String, String> {
        keysCache?.let { return it }
        val keys = LlmProviderId.entries.associate { id ->
            apiKeyKey(id) to runCatching {
                encryptedPrefs.getString(apiKeyKey(id), "").orEmpty()
            }.getOrDefault("")
        } + (KEY_DICTIONARY_API_KEY to runCatching {
            encryptedPrefs.getString(KEY_DICTIONARY_API_KEY, "").orEmpty()
        }.getOrDefault(""))
        keysCache = keys
        return keys
    }

    override suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER] = settings.activeLlmProviderId.name
            prefs[KEY_DEFAULT_MODE] = settings.defaultTranslationMode.name
            prefs[KEY_THEME_MODE] = settings.themeMode.name
            prefs[KEY_APP_LANGUAGE] = settings.appLanguage.name
            prefs[KEY_INTERFACE_STYLE] = settings.interfaceStyle.name
            settings.llmProviders.forEach { (id, config) ->
                config.baseUrl?.let { prefs[stringPreferencesKey(baseUrlKey(id))] = it }
                prefs[stringPreferencesKey(modelKey(id))] = config.model
                prefs[floatPreferencesKey(temperatureKey(id))] = config.temperature
            }
        }
        withContext(Dispatchers.IO) {
            val writeOk = runCatching {
                encryptedPrefs.edit().apply {
                    settings.llmProviders.forEach { (id, config) ->
                        putString(apiKeyKey(id), config.apiKey)
                    }
                    putString(KEY_DICTIONARY_API_KEY, settings.dictionaryApiKey)
                    apply()
                }
            }.isSuccess
            // Refresh the cache from what was written; drop it entirely on
            // failure so the next read reloads from disk.
            keysCache = if (writeOk) {
                LlmProviderId.entries.associate { id ->
                    apiKeyKey(id) to configApiKey(settings, id)
                } + (KEY_DICTIONARY_API_KEY to settings.dictionaryApiKey)
            } else {
                null
            }
        }
    }

    private fun configApiKey(settings: AppSettings, id: LlmProviderId): String =
        settings.llmProviders[id]?.apiKey.orEmpty()

    override suspend fun translationMemory(): TranslationMemory? {
        val prefs = dataStore.data.first()
        val source = prefs[KEY_MEM_SOURCE]?.let { enumOrNull<Language>(it) }
        val target = prefs[KEY_MEM_TARGET]?.let { enumOrNull<Language>(it) }
        val mode = prefs[KEY_MEM_MODE]?.let { enumOrNull<TranslationMode>(it) }
        // A partial record (e.g. from a future schema change) is no memory.
        if (source == null || target == null || mode == null) return null
        // AUTO is only ever valid as a source; never restore it as target.
        if (target == Language.AUTO) return null
        return TranslationMemory(source, target, mode)
    }

    override suspend fun saveTranslationMemory(memory: TranslationMemory) {
        dataStore.edit { prefs ->
            prefs[KEY_MEM_SOURCE] = memory.source.name
            prefs[KEY_MEM_TARGET] = memory.target.name
            prefs[KEY_MEM_MODE] = memory.mode.name
        }
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        runCatching { enumValueOf<T>(name) }.getOrNull()

    private companion object {
        val KEY_MEM_SOURCE = stringPreferencesKey("mem_source_lang")
        val KEY_MEM_TARGET = stringPreferencesKey("mem_target_lang")
        val KEY_MEM_MODE = stringPreferencesKey("mem_mode")

        val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_llm_provider")
        val KEY_DEFAULT_MODE = stringPreferencesKey("default_translation_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_INTERFACE_STYLE = stringPreferencesKey("interface_style")
        const val KEY_DICTIONARY_API_KEY = "dictionary_api_key"

        fun baseUrlKey(id: LlmProviderId) = "base_url_${id.name.lowercase()}"
        fun modelKey(id: LlmProviderId) = "model_${id.name.lowercase()}"
        fun temperatureKey(id: LlmProviderId) = "temperature_${id.name.lowercase()}"
        fun apiKeyKey(id: LlmProviderId) = "api_key_${id.name.lowercase()}"
    }
}
