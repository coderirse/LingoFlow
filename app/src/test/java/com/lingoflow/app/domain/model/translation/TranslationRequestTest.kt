package com.lingoflow.app.domain.model.translation

import com.lingoflow.app.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationRequestTest {

    @Test
    fun `request keeps given fields`() {
        val request = TranslationRequest(
            text = "Hello",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.CHINESE,
            mode = TranslationMode.LEARNING
        )
        assertEquals("Hello", request.text)
        assertEquals(Language.ENGLISH, request.sourceLanguage)
        assertEquals(Language.CHINESE, request.targetLanguage)
        assertEquals(TranslationMode.LEARNING, request.mode)
    }

    @Test
    fun `mode defaults to STANDARD`() {
        val request = TranslationRequest(
            text = "Hello",
            sourceLanguage = Language.AUTO,
            targetLanguage = Language.CHINESE
        )
        assertEquals(TranslationMode.STANDARD, request.mode)
    }
}
