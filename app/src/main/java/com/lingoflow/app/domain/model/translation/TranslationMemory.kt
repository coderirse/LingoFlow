package com.lingoflow.app.domain.model.translation

import com.lingoflow.app.domain.model.Language

/**
 * The last translation setup the user actually used (language pair + mode),
 * persisted so the next launch resumes where they left off instead of
 * resetting to AUTO→中文 / STANDARD.
 */
data class TranslationMemory(
    val source: Language,
    val target: Language,
    val mode: TranslationMode
)
