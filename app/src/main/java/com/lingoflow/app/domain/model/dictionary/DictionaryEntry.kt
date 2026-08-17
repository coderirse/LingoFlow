package com.lingoflow.app.domain.model.dictionary

/** Full dictionary lookup result for one headword. */
data class DictionaryEntry(
    val word: String,
    val phonetics: List<Phonetic>,
    val entries: List<PartOfSpeechEntry>,
    val phrases: List<PhrasalVerb>,
    val etymology: String?
)
