package com.lingoflow.app.domain.model.translation

import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationResponseTest {

    @Test
    fun `Standard is a TranslationResponse`() {
        val standard = TranslationResponse.Standard(
            translatedText = "你好",
            detectedLanguage = Language.ENGLISH
        )
        val response: TranslationResponse = standard
        assertTrue(response is TranslationResponse.Standard)
        assertEquals("你好", standard.translatedText)
        assertEquals(Language.ENGLISH, standard.detectedLanguage)
    }

    @Test
    fun `Standard detectedLanguage defaults to null`() {
        val response = TranslationResponse.Standard(translatedText = "你好")
        assertNull(response.detectedLanguage)
    }

    @Test
    fun `Learning is a TranslationResponse and carries dictionary data`() {
        val entry = DictionaryEntry(
            word = "abide",
            phonetics = emptyList(),
            entries = emptyList(),
            phrases = emptyList(),
            etymology = null
        )
        val learning = TranslationResponse.Learning(
            translatedText = "遵守",
            dictionaryEntries = listOf(entry),
            contextExplanation = null
        )
        val response: TranslationResponse = learning
        assertTrue(response is TranslationResponse.Learning)
        assertEquals(listOf(entry), learning.dictionaryEntries)
        assertNull(learning.contextExplanation)
    }
}
