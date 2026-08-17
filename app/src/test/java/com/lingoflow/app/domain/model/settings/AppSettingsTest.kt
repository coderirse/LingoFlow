package com.lingoflow.app.domain.model.settings

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.translation.TranslationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaultTranslationMode defaults to STANDARD`() {
        val settings = AppSettings(
            activeLlmProviderId = LlmProviderId.DEEPSEEK,
            llmProviders = emptyMap(),
            dictionaryApiKey = ""
        )
        assertEquals(TranslationMode.STANDARD, settings.defaultTranslationMode)
    }

    @Test
    fun `ProviderConfig temperature defaults to 0_7`() {
        val config = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = LlmProviderId.DEEPSEEK.defaultModel
        )
        assertEquals(0.7f, config.temperature)
    }

    @Test
    fun `settings keep provider map and active provider`() {
        val config = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = "deepseek-chat"
        )
        val settings = AppSettings(
            activeLlmProviderId = LlmProviderId.DEEPSEEK,
            llmProviders = mapOf(LlmProviderId.DEEPSEEK to config),
            dictionaryApiKey = "mw-key",
            defaultTranslationMode = TranslationMode.LEARNING
        )
        assertEquals(LlmProviderId.DEEPSEEK, settings.activeLlmProviderId)
        assertEquals(config, settings.llmProviders[LlmProviderId.DEEPSEEK])
        assertEquals("mw-key", settings.dictionaryApiKey)
        assertEquals(TranslationMode.LEARNING, settings.defaultTranslationMode)
    }
}
