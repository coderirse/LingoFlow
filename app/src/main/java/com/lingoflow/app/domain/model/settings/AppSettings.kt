package com.lingoflow.app.domain.model.settings

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.translation.TranslationMode

/** Persisted application settings. */
data class AppSettings(
    val activeLlmProviderId: LlmProviderId,
    val llmProviders: Map<LlmProviderId, ProviderConfig>,
    val dictionaryApiKey: String,
    val defaultTranslationMode: TranslationMode = TranslationMode.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val interfaceStyle: InterfaceStyle = InterfaceStyle.MODERN
)
