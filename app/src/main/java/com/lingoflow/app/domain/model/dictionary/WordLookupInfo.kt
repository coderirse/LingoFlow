package com.lingoflow.app.domain.model.dictionary

/** One part of speech with its Chinese glosses, e.g. vt. 考虑；认为. */
data class PosMeaning(
    val partOfSpeech: String,
    val meanings: List<String>
)

/**
 * Concise Chinese dictionary info for a single English word, produced by the
 * LLM (Merriam-Webster supplies phonetics and the full English entries).
 */
data class WordLookupInfo(
    val word: String,
    val entries: List<PosMeaning>,
    val example: String?,
    val exampleTranslation: String?
)
