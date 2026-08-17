package com.lingoflow.app.domain.model.dictionary

/** All senses of a word under one part of speech. */
data class PartOfSpeechEntry(
    /** e.g. "noun", "verb", "adjective". */
    val partOfSpeech: String,
    val definitions: List<Definition>
)
