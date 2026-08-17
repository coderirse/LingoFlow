package com.lingoflow.app.domain.model.settings

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.translation.TranslationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppSettingsSerializationTest {

    @Test
    fun `ProviderConfig equality and hashCode follow data class semantics`() {
        val a = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = "deepseek-chat"
        )
        val b = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = "deepseek-chat"
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `ProviderConfig copy changes only the requested field`() {
        val config = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = "deepseek-chat"
        )
        val copied = config.copy(apiKey = "other")
        assertEquals("other", copied.apiKey)
        assertEquals(config.model, copied.model)
        assertNotEquals(config, copied)
    }

    @Test
    fun `AppSettings copy preserves providers map`() {
        val config = ProviderConfig(
            providerId = LlmProviderId.DEEPSEEK,
            apiKey = "key",
            baseUrl = null,
            model = "deepseek-chat"
        )
        val settings = AppSettings(
            activeLlmProviderId = LlmProviderId.DEEPSEEK,
            llmProviders = mapOf(LlmProviderId.DEEPSEEK to config),
            dictionaryApiKey = "mw",
            defaultTranslationMode = TranslationMode.NATURAL
        )
        val copied = settings.copy(dictionaryApiKey = "mw2")
        assertEquals("mw2", copied.dictionaryApiKey)
        assertEquals(settings.llmProviders, copied.llmProviders)
        assertEquals(TranslationMode.NATURAL, copied.defaultTranslationMode)
    }

    @Test
    fun `provider defaults stay stable`() {
        assertEquals("https://api.deepseek.com/v1", LlmProviderId.DEEPSEEK.defaultBaseUrl)
        assertEquals("deepseek-chat", LlmProviderId.DEEPSEEK.defaultModel)
        assertEquals("https://api.openai.com/v1", LlmProviderId.OPENAI.defaultBaseUrl)
        assertEquals("gpt-4o-mini", LlmProviderId.OPENAI.defaultModel)
    }
}
