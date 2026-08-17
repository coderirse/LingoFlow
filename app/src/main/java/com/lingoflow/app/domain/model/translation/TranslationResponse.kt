package com.lingoflow.app.domain.model.translation

import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.model.learning.ContextExplanation

/** Result of a translation, shaped by the requested TranslationMode. */
sealed class TranslationResponse {

    /** Plain translated text, optionally with the detected source language. */
    data class Standard(
        val translatedText: String,
        val detectedLanguage: Language? = null
    ) : TranslationResponse()

    /** Translation enriched with dictionary entries and an in-context explanation. */
    data class Learning(
        val translatedText: String,
        val dictionaryEntries: List<DictionaryEntry>,
        val contextExplanation: ContextExplanation?
    ) : TranslationResponse()
}
