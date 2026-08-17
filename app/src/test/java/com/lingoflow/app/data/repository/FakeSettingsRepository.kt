package com.lingoflow.app.data.repository

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository

/** In-memory [SettingsRepository] for ViewModel tests. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(
        activeLlmProviderId = LlmProviderId.DEEPSEEK,
        llmProviders = mapOf(
            LlmProviderId.DEEPSEEK to ProviderConfig(
                providerId = LlmProviderId.DEEPSEEK,
                apiKey = "",
                baseUrl = null,
                model = LlmProviderId.DEEPSEEK.defaultModel
            )
        ),
        dictionaryApiKey = "",
        defaultTranslationMode = TranslationMode.STANDARD
    )
) : SettingsRepository {

    var savedSettings: AppSettings? = null
        private set

    private var current: AppSettings = initial

    override suspend fun getSettings(): AppSettings = current

    override suspend fun saveSettings(settings: AppSettings) {
        current = settings
        savedSettings = settings
    }
}
