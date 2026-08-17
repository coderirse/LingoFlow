package com.lingoflow.app.domain.usecase

import com.lingoflow.app.data.translator.FakeTranslator
import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateTextUseCaseTest {

    private val useCase = TranslateTextUseCase(
        MlKitTranslationEngine(FakeTranslator())
    )

    private fun request(
        text: String,
        source: Language,
        target: Language
    ) = TranslationRequest(text = text, sourceLanguage = source, targetLanguage = target)

    @Test
    fun `blank text fails without touching the engine`() = runTest {
        val result = useCase(request("   ", Language.AUTO, Language.CHINESE))
        assertTrue(result.isFailure)
    }

    @Test
    fun `english to chinese translates`() = runTest {
        val result = useCase(request("Hello", Language.ENGLISH, Language.CHINESE))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response is TranslationResponse.Standard)
        assertEquals("你好", (response as TranslationResponse.Standard).translatedText)
    }

    @Test
    fun `chinese source is accepted`() = runTest {
        val result = useCase(request("你好", Language.CHINESE, Language.ENGLISH))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals(Language.CHINESE, response.detectedLanguage)
    }

    @Test
    fun `japanese source is accepted`() = runTest {
        val result = useCase(request("こんにちは", Language.JAPANESE, Language.CHINESE))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals(Language.JAPANESE, response.detectedLanguage)
    }

    @Test
    fun `korean source is accepted`() = runTest {
        val result = useCase(request("안녕하세요", Language.KOREAN, Language.CHINESE))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals(Language.KOREAN, response.detectedLanguage)
    }

    @Test
    fun `auto detects english input`() = runTest {
        val result = useCase(request("Hello world", Language.AUTO, Language.CHINESE))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals(Language.ENGLISH, response.detectedLanguage)
    }

    @Test
    fun `same source and target returns original text`() = runTest {
        val result = useCase(request("Hello", Language.ENGLISH, Language.ENGLISH))
        assertTrue(result.isSuccess)
        val response = result.getOrThrow() as TranslationResponse.Standard
        assertEquals("Hello", response.translatedText)
    }
}
