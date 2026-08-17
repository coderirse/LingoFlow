package com.lingoflow.app.domain.usecase

import com.lingoflow.app.data.llm.FakeLlmProvider
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupWordUseCaseTest {

    private val goodJson = """
        {
          "word": "consider",
          "entries": [
            {"partOfSpeech": "vt.", "meanings": ["考虑", "认为", "细想"]},
            {"partOfSpeech": "vi.", "meanings": ["考虑", "认为"]}
          ],
          "example": "I will consider your proposal.",
          "exampleTranslation": "我会考虑你的提议。"
        }
    """.trimIndent()

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

    private fun useCaseWith(
        provider: FakeLlmProvider,
        apiKey: String = "sk-test"
    ) = LookupWordUseCase(settingsWithKey(apiKey)) { provider }

    @Test
    fun `valid json parses into word lookup info`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse(goodJson, finishReason = "stop", usage = null)
        )
        val useCase = useCaseWith(provider)

        val result = useCase("consider")

        assertTrue(result.isSuccess)
        val info = result.getOrThrow()
        assertEquals("consider", info.word)
        assertEquals(2, info.entries.size)
        assertEquals("vt.", info.entries[0].partOfSpeech)
        assertEquals(listOf("考虑", "认为", "细想"), info.entries[0].meanings)
        assertEquals("I will consider your proposal.", info.example)
        assertEquals("我会考虑你的提议。", info.exampleTranslation)
    }

    @Test
    fun `json wrapped in markdown fences still parses`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse("```json\n$goodJson\n```", "stop", null)
        )
        val useCase = useCaseWith(provider)

        val result = useCase("consider")

        assertTrue(result.isSuccess)
        assertEquals("consider", result.getOrThrow().word)
    }

    @Test
    fun `repeat lookup is served from cache without a second call`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse(goodJson, finishReason = "stop", usage = null)
        )
        val useCase = useCaseWith(provider)

        useCase("consider")
        val second = useCase("Consider")

        assertTrue(second.isSuccess)
        // Provider only saw one request (cache normalized the case).
        assertEquals("consider", provider.lastRequest?.messages?.last()?.content)
    }

    @Test
    fun `invalid json yields failure`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse("not json at all", "stop", null)
        )
        val useCase = useCaseWith(provider)

        assertTrue(useCase("consider").isFailure)
    }

    @Test
    fun `missing api key yields failure without calling the provider`() = runTest {
        val provider = FakeLlmProvider()
        val useCase = useCaseWith(provider, apiKey = "")

        assertTrue(useCase("consider").isFailure)
        assertNull(provider.lastRequest)
    }

    @Test
    fun `entries without meanings are dropped and empty entries fail`() = runTest {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse(
                """{"word": "x", "entries": [{"partOfSpeech": "n.", "meanings": []}]}""",
                "stop",
                null
            )
        )
        val useCase = useCaseWith(provider)

        assertTrue(useCase("x").isFailure)
    }
}
