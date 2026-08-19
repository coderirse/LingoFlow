package com.lingoflow.app.domain.usecase

import com.lingoflow.app.data.llm.FakeLlmProvider
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

    // ------------------------------------------------------------------
    // lookupStream
    // ------------------------------------------------------------------

    @Test
    fun `lookupStream emits progressively more complete info`() = runTest {
        val provider = FakeLlmProvider(
            streamFlow = flowOf(
                "WORD: consider\nPOS: vt. | 考虑；认为；细想\n",
                "POS: vi. | 考虑；认为\n",
                "EX: I will consider your proposal.\nEX_CN: 我会考虑你的提议。\n"
            )
        )
        val useCase = useCaseWith(provider)

        val emissions = useCase.lookupStream("consider").toList()

        assertTrue(emissions.size >= 2)
        assertEquals(1, emissions.first().entries.size)
        val final = emissions.last()
        assertEquals("consider", final.word)
        assertEquals(2, final.entries.size)
        assertEquals("vt.", final.entries[0].partOfSpeech)
        assertEquals(listOf("考虑", "认为", "细想"), final.entries[0].meanings)
        assertEquals("I will consider your proposal.", final.example)
        assertEquals("我会考虑你的提议。", final.exampleTranslation)
    }

    @Test
    fun `lookupStream final result is cached and repeat lookups skip the provider`() = runTest {
        var streamCollections = 0
        val provider = FakeLlmProvider(
            streamFlow = flow {
                streamCollections++
                emit("POS: n. | 叉子\n")
            }
        )
        val useCase = useCaseWith(provider)

        useCase.lookupStream("fork").toList()
        val second = useCase.lookupStream("Fork").toList()

        assertEquals(1, streamCollections)
        assertEquals(1, second.size)
        assertEquals(listOf("叉子"), second[0].entries[0].meanings)
    }

    @Test
    fun `lookupStream tolerates malformed lines`() = runTest {
        val provider = FakeLlmProvider(
            streamFlow = flowOf(
                "garbage line\nPOS: adj. | 高兴的\nPOS: missing-pipe\nEX_CN: 没有例句\n"
            )
        )
        val useCase = useCaseWith(provider)

        val result = useCase.lookupStream("happy").toList().last()

        assertEquals(1, result.entries.size)
        assertEquals("adj.", result.entries[0].partOfSpeech)
        assertEquals(listOf("高兴的"), result.entries[0].meanings)
        assertNull(result.example)
        assertEquals("没有例句", result.exampleTranslation)
    }

    @Test
    fun `lookupStream with no usable lines fails`() = runTest {
        val provider = FakeLlmProvider(streamFlow = flowOf("complete nonsense\n"))
        val useCase = useCaseWith(provider)

        assertTrue(runCatching { useCase.lookupStream("x").toList() }.isFailure)
    }

    @Test
    fun `lookupStream without api key fails before calling the provider`() = runTest {
        val provider = FakeLlmProvider()
        val useCase = useCaseWith(provider, apiKey = "")

        assertTrue(runCatching { useCase.lookupStream("x").toList() }.isFailure)
        assertNull(provider.lastRequest)
    }

    @Test
    fun `lookupStream tolerates full-width colons and markdown noise`() = runTest {
        val provider = FakeLlmProvider(
            streamFlow = flowOf(
                "WORD：engage\n",
                "- **POS:** vt. | 参与；从事；吸引\n",
                "POS：vi. | 参与；订婚\n",
                "EX：They engaged in discussion.\n",
                "EX_CN：他们参与了讨论。\n"
            )
        )
        val useCase = useCaseWith(provider)

        val result = useCase.lookupStream("engage").toList().last()

        assertEquals("engage", result.word)
        assertEquals(2, result.entries.size)
        assertEquals("vt.", result.entries[0].partOfSpeech)
        assertEquals(listOf("参与", "从事", "吸引"), result.entries[0].meanings)
        assertEquals("They engaged in discussion.", result.example)
        assertEquals("他们参与了讨论。", result.exampleTranslation)
    }
}
