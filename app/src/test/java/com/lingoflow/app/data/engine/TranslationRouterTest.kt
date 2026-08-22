package com.lingoflow.app.data.engine

import com.lingoflow.app.data.llm.FakeLlmProvider
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.data.translator.FakeTranslator
import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationNotices
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationRouterTest {

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

    private fun createRouter(apiKey: String): TranslationRouter {
        val provider = FakeLlmProvider(
            chatResult = ChatResponse("llm result", finishReason = "stop", usage = null)
        )
        return TranslationRouter(
            mlKitEngine = MlKitTranslationEngine(FakeTranslator()),
            llmEngine = LlmTranslationEngine(settingsWithKey(apiKey)) { provider },
            settingsRepository = settingsWithKey(apiKey)
        )
    }

    private fun request(
        mode: TranslationMode,
        text: String = "Hello"
    ) = TranslationRequest(
        text = text,
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = mode
    )

    @Test
    fun `standard mode always uses ml kit`() = runTest {
        val router = createRouter(apiKey = "sk-test")

        val result = router.translate(request(TranslationMode.STANDARD))

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        // ML Kit fake translator's canned output, not the LLM's "llm result".
        assertEquals("你好", response.translatedText)
    }

    @Test
    fun `long standard mode with api key uses the llm engine`() = runTest {
        val router = createRouter(apiKey = "sk-test")

        val result = router.translate(
            request(TranslationMode.STANDARD, "a".repeat(500))
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("llm result", response.translatedText)
    }

    @Test
    fun `long standard mode without api key falls back to ml kit with a notice`() = runTest {
        val longText = "a".repeat(500)
        val router = createRouter(apiKey = "")
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val result = router.translate(
            request(TranslationMode.STANDARD, longText)
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("[Fake Translation] $longText", response.translatedText)
        assertEquals(
            listOf(TranslationNotices.LLM_KEY_MISSING),
            received
        )
        job.cancel()
    }

    @Test
    fun `long standard mode falls back to ml kit when llm fails`() = runTest {
        val longText = "a".repeat(500)
        val provider = FakeLlmProvider(
            chatError = IllegalStateException("boom")
        )
        val router = TranslationRouter(
            mlKitEngine = MlKitTranslationEngine(FakeTranslator()),
            llmEngine = LlmTranslationEngine(settingsWithKey("sk-test")) { provider },
            settingsRepository = settingsWithKey("sk-test")
        )
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val result = router.translate(
            request(TranslationMode.STANDARD, longText)
        )
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("[Fake Translation] $longText", response.translatedText)
        assertEquals(
            listOf(TranslationNotices.LLM_FAILED),
            received
        )
        job.cancel()
    }

    @Test
    fun `llm mode without api key falls back to ml kit with a notice`() = runTest {
        val router = createRouter(apiKey = "")
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val result = router.translate(request(TranslationMode.NATURAL))
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("你好", response.translatedText)
        assertEquals(
            listOf(TranslationNotices.LLM_KEY_MISSING),
            received
        )
        job.cancel()
    }

    @Test
    fun `llm mode with api key uses the llm engine`() = runTest {
        val router = createRouter(apiKey = "sk-test")

        val result = router.translate(request(TranslationMode.NATURAL))

        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("llm result", response.translatedText)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationRouterStreamTest {

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

    private fun createRouter(apiKey: String, stream: kotlinx.coroutines.flow.Flow<String>) =
        TranslationRouter(
            mlKitEngine = MlKitTranslationEngine(FakeTranslator()),
            llmEngine = LlmTranslationEngine(settingsWithKey(apiKey)) {
                FakeLlmProvider(streamFlow = stream)
            },
            settingsRepository = settingsWithKey(apiKey)
        )

    private fun request() = TranslationRequest(
        text = "Hello",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = TranslationMode.NATURAL
    )

    private fun standardRequest(text: String) = TranslationRequest(
        text = text,
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = TranslationMode.STANDARD
    )

    @Test
    fun `stream with api key emits llm deltas`() = runTest {
        val router = createRouter("sk-test", flowOf("你", "好"))

        val deltas = router.translateStream(request()).toList()

        assertEquals(listOf("你", "好"), deltas)
    }

    @Test
    fun `stream without api key falls back to a single ml kit emission`() = runTest {
        val router = createRouter("", flowOf("should", "not", "emit"))
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val deltas = router.translateStream(request()).toList()
        advanceUntilIdle()

        assertEquals(listOf("你好"), deltas)
        assertEquals(
            listOf(TranslationNotices.LLM_KEY_MISSING),
            received
        )
        job.cancel()
    }

    @Test
    fun `long standard stream with api key emits llm deltas`() = runTest {
        val longText = "a".repeat(TranslationRouter.LONG_TEXT_MIN_LENGTH)
        val router = createRouter("sk-test", flowOf("第一段。", "\n\n", "第二段。"))

        val deltas = router.translateStream(standardRequest(longText)).toList()

        assertEquals(listOf("第一段。", "\n\n", "第二段。"), deltas)
    }

    @Test
    fun `short standard stream stays on ml kit even with api key`() = runTest {
        val router = createRouter("sk-test", flowOf("should", "not", "emit"))
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val deltas = router.translateStream(standardRequest("Hello")).toList()
        advanceUntilIdle()

        // Single on-device emission, and no fallback notice: short STANDARD
        // never intended to use the LLM.
        assertEquals(listOf("你好"), deltas)
        assertTrue(received.isEmpty())
        job.cancel()
    }

    @Test
    fun `long standard stream without api key falls back to ml kit`() = runTest {
        val longText = "a".repeat(TranslationRouter.LONG_TEXT_MIN_LENGTH)
        val router = createRouter("", flowOf("should", "not", "emit"))
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val deltas = router.translateStream(standardRequest(longText)).toList()
        advanceUntilIdle()

        assertEquals(listOf("[Fake Translation] $longText"), deltas)
        assertEquals(
            listOf(TranslationNotices.LLM_KEY_MISSING),
            received
        )
        job.cancel()
    }

    @Test
    fun `long standard stream falls back when llm fails before emitting`() = runTest {
        val longText = "a".repeat(TranslationRouter.LONG_TEXT_MIN_LENGTH)
        val failingStream = kotlinx.coroutines.flow.flow<String> {
            throw IllegalStateException("boom")
        }
        val router = createRouter("sk-test", failingStream)
        val received = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            router.fallbackMessages.collect { received += it }
        }

        val deltas = router.translateStream(standardRequest(longText)).toList()
        advanceUntilIdle()

        assertEquals(listOf("[Fake Translation] $longText"), deltas)
        assertEquals(
            listOf(TranslationNotices.LLM_FAILED),
            received
        )
        job.cancel()
    }
}
