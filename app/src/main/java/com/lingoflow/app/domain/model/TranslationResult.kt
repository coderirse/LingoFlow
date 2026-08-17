package com.lingoflow.app.domain.model

/** Result of a single translation request. */
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: Language,
    val targetLanguage: Language
)
