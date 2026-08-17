package com.lingoflow.app.domain.model.translation

import com.lingoflow.app.domain.model.Language

/** Input for a translation executed by a TranslationEngine. */
data class TranslationRequest(
    val text: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val mode: TranslationMode = TranslationMode.STANDARD
)
