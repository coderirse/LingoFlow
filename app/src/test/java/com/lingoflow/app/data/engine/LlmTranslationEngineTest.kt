package com.lingoflow.app.data.engine

import com.lingoflow.app.data.llm.FakeLlmProvider
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmTranslationEngineTest {

    private fun settingsWithKey(apiKey: String) = FakeSettingsRepository(
        AppSettings(
            activeLlmProviderId = LlmProviderId.DEEPSEEK,
            llmProviders = mapOf(
                LlmProviderId.DEEPSEEK to ProviderConfig(
                    providerId = LlmProviderId.DEEPSEEK,
                    apiKey = apiKey,
                    baseUrl = null,
                    model = "deepseek-chat"
                )
            ),
            dictionaryApiKey = ""
        )
    )

    private fun engineWith(
        provider: FakeLlmProvider,
        apiKey: String = "sk-test"
    ): LlmTranslationEngine = LlmTranslationEngine(
        settingsRepository = settingsWithKey(apiKey),
        providerFactory = { provider }
    )

    private fun request(mode: TranslationMode) = TranslationRequest(
        text = "Hello",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = mode
    )

    @Test
    fun `natural mode returns Standard response`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse("你好呀", finishReason = "stop", usage = null)
        )
        val engine = engineWith(provider)

        val result = engine.translate(request(TranslationMode.NATURAL))

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response is TranslationResponse.Standard)
        assertEquals("你好呀", (response as TranslationResponse.Standard).translatedText)

        // The system prompt must describe a natural-register translation.
        val systemPrompt = provider.lastRequest!!.messages.first().content
        assertTrue(systemPrompt.contains("naturally"))
    }

    @Test
    fun `learning mode parses json into Learning response`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse(
                content = """
                {
                  "translation": "你好",
                  "meaning_in_context": "A common greeting",
                  "grammar_note": null,
                  "usage_note": "Used informally",
                  "synonyms": ["hi", "hey"]
                }
                """.trimIndent(),
                finishReason = "stop",
                usage = null
            )
        )
        val engine = engineWith(provider)

        val result = engine.translate(request(TranslationMode.LEARNING))

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response is TranslationResponse.Learning)
        val learning = response as TranslationResponse.Learning
        assertEquals("你好", learning.translatedText)
        assertEquals("A common greeting", learning.contextExplanation?.meaningInContext)
        assertEquals("Used informally", learning.contextExplanation?.usageNote)
        assertEquals(
            listOf("hi", "hey"),
            learning.contextExplanation?.synonymsInContext?.map { it.word }
        )
    }

    @Test
    fun `learning mode degrades to Standard on non-json output`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse("plain text", finishReason = "stop", usage = null)
        )
        val engine = engineWith(provider)

        val result = engine.translate(request(TranslationMode.LEARNING))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow() is TranslationResponse.Standard)
    }
}
