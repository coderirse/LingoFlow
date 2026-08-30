package com.lingoflow.app.domain.model.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmProviderIdTest {

    @Test
    fun `DeepSeek is the default provider with correct endpoint`() {
        assertEquals("https://api.deepseek.com/v1", LlmProviderId.DEEPSEEK.defaultBaseUrl)
        assertEquals("deepseek-chat", LlmProviderId.DEEPSEEK.defaultModel)
    }

    @Test
    fun `all providers are registered`() {
        // ANTHROPIC was removed: its native API (/v1/messages) does not speak
        // the OpenAI-compatible protocol this client uses; gateway users go
        // through CUSTOM.
        assertEquals(5, LlmProviderId.entries.size)
        assertTrue(LlmProviderId.entries.contains(LlmProviderId.DEEPSEEK))
    }

    @Test
    fun `GEMINI default points at the OpenAI-compatible endpoint`() {
        assertTrue(LlmProviderId.GEMINI.defaultBaseUrl.endsWith("/openai"))
    }

    @Test
    fun `CUSTOM starts blank`() {
        assertEquals("", LlmProviderId.CUSTOM.defaultBaseUrl)
        assertEquals("", LlmProviderId.CUSTOM.defaultModel)
    }
}
