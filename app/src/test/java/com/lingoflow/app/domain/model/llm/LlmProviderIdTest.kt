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
        assertEquals(6, LlmProviderId.entries.size)
        assertTrue(LlmProviderId.entries.contains(LlmProviderId.DEEPSEEK))
    }

    @Test
    fun `CUSTOM starts blank`() {
        assertEquals("", LlmProviderId.CUSTOM.defaultBaseUrl)
        assertEquals("", LlmProviderId.CUSTOM.defaultModel)
    }
}
