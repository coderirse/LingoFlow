package com.lingoflow.app.data.repository

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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

    private val current = MutableStateFlow(initial)

    override suspend fun getSettings(): AppSettings = current.value

    override suspend fun saveSettings(settings: AppSettings) {
        current.value = settings
        savedSettings = settings
    }

    override fun observeSettings(): Flow<AppSettings> = current
}
