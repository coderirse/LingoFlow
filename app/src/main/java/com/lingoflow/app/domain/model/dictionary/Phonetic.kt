package com.lingoflow.app.domain.model.dictionary

/** Pronunciation of a word. */
data class Phonetic(
    /** Phonetic transcription, e.g. "/əˈkɒmpənɪment/". */
    val text: String,
    val audioUrl: String?
)
